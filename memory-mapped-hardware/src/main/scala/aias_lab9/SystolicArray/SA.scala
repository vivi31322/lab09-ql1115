package aias_lab9.SystolicArray

import chisel3._
import chisel3.util._

import scala.io.Source

import aias_lab9.AXILite._

class SA(rows:Int,
         cols:Int,
         addr_width:Int,
         data_width:Int,
         reg_width:Int) extends Module{
    val io = IO(new Bundle{
        // for the connection to mmio
        val mmio = Flipped(new MMIO(reg_width))

        // for access localmem when SA still be a slave
        val raddr = Output(UInt(addr_width.W))
        val rdata = Input(UInt(data_width.W))
        
        val wen   = Output(Bool())
        val waddr = Output(UInt(addr_width.W))
        val wdata = Output(UInt(data_width.W))
        val wstrb = Output(UInt((data_width>>3).W))

        // for making localMem print the value
        val finish = Output(Bool())
    })

    // constant declaration
    val byte = 8
    val mat_a_rows = io.mmio.MATA_SIZE(11, 0) + 1.U
    val mat_a_cols = io.mmio.MATA_SIZE(27,16) + 1.U
    val mat_b_rows = io.mmio.MATB_SIZE(11, 0) + 1.U
    val mat_b_cols = io.mmio.MATB_SIZE(27,16) + 1.U
    val mat_c_rows = io.mmio.MATC_SIZE(11, 0) + 1.U
    val mat_c_cols = io.mmio.MATC_SIZE(27,16) + 1.U        

    // Module Declaration
    val i_buffer = Module(new buffer(rows,byte))
    val o_buffer = Module(new buffer(cols,byte))
    val tile = Module(new tile(rows,cols,byte))

    //counter declaration
    val w_cnt = RegInit(0.U(4.W))  //used for "weight" data access 
    val i_cnt = RegInit(0.U(4.W))  //used for "input" data access 
    val o_cnt = RegInit(0.U(4.W))  //used for "input" data access 

    // Enable Register
    val ENABLE_REG = RegInit(false.B)

    // Read Memory Wiring
    val mat_buf = 0x000000 // 0 because the localMem is still local for SA 
    val a_base_addr = WireDefault(io.mmio.MATA_MEM_ADDR)
    val b_base_addr = WireDefault(io.mmio.MATB_MEM_ADDR)
    val c_base_addr = WireDefault(io.mmio.MATC_MEM_ADDR)

    //state declaration
    val sIdle :: sReady  :: sStall_0 :: sPreload :: sStall_1 ::  sPropagate :: sCheck :: sFinish :: Nil = Enum(8)
    val state = RegInit(sIdle)

    when(state===sReady){
        io.raddr := 0.U
    }
    .elsewhen(state===sStall_0){
        io.raddr := b_base_addr + (w_cnt << 2)
    }
    .elsewhen(state===sPreload){
        io.raddr := b_base_addr + (w_cnt << 2)
    }
    .elsewhen(state===sStall_1){
        io.raddr := a_base_addr + (i_cnt << 2)
    }
    .elsewhen(state===sPropagate){
        io.raddr := a_base_addr + (i_cnt << 2)
    }
    .elsewhen(state===sCheck){
        io.raddr := c_base_addr + (o_cnt << 2)
    }
    .otherwise{
        io.raddr := 0.U
    }

    val rdata_picker = RegNext(io.raddr)
    val rdata = Mux(rdata_picker(2) === 0.U,io.rdata(63,32),io.rdata(31,0))


    io.waddr := c_base_addr + (o_cnt<<2)
    val word_wdata = WireDefault(List.range(0,cols).map{case x => o_buffer.io.output(x).bits <<(byte*(cols-1-x))}.reduce(_+_))
    io.wdata := Mux(o_cnt(0)===0.U,Cat(0.U(32.W),word_wdata),Cat(word_wdata,0.U(32.W)))
    io.wstrb := Mux(o_cnt(0)===0.U,"b00001111".U,"b11110000".U)
    io.wen := o_buffer.io.output(0).valid

    // tile 2 Output Buffer wiring
    List.range(0,cols).map{x=>
        o_buffer.io.input(cols-1-x) <> tile.io.output(x)
    }

    io.finish := io.mmio.STATUS_IN


    //input buffer 2 tile wiring
    tile.io.input <> i_buffer.io.output

    //In our design, the preload of weight doesn't pass through the buffer
    List.range(0,cols).map{x=>

        tile.io.weight(x).bits := Mux(state===sPreload,rdata((cols-x)*byte-1,(cols-x-1)*byte),0.U)

        tile.io.weight(x).valid := state===sPreload
    }

    tile.io.preload := state === sPreload

    //mem to input buffer wiring
    List.range(0,rows).map{x=>
        
        i_buffer.io.input(x).bits := Mux(
            state === sPropagate && i_cnt <= cols.U , 
            rdata(byte*(x+1)-1,byte*x),
            0.U 
        )
        
        i_buffer.io.input(x).valid := Mux(
            state === sPropagate && i_cnt <= cols.U , 
            true.B, 
            false.B
        )
    }


    when(state===sIdle){
        state := sReady
    }
    .elsewhen(state===sReady){
        state := Mux(io.mmio.ENABLE_OUT,sStall_0,sReady)
        ENABLE_REG := io.mmio.ENABLE_OUT
    }
    .elsewhen(state===sStall_0){
        state := sPreload
        w_cnt := w_cnt + 1.U
    }
    .elsewhen(state===sPreload){
        when(io.mmio.ENABLE_OUT){
            state := Mux(w_cnt === rows.U, sStall_1, sPreload)
            w_cnt := Mux(w_cnt === rows.U, 0.U, w_cnt + 1.U)
        }.otherwise{
            state := sReady
            w_cnt := 0.U
        }
    }
    .elsewhen(state===sStall_1){
        state := sPropagate
        i_cnt := i_cnt + 1.U
    }
    .elsewhen(state===sPropagate){
        when(io.mmio.ENABLE_OUT){
            state := Mux(i_cnt===(cols+rows-1).U, sCheck, sPropagate)
            i_cnt := i_cnt + Mux(i_cnt===(cols+rows-1).U, 0.U, 1.U)
        }.otherwise{
            state := sReady
            i_cnt := 0.U
        }
    }
    .elsewhen(state===sCheck){
        state := Mux(o_cnt===(rows-1).U, sFinish, sCheck)
        o_cnt := Mux(o_cnt===(rows-1).U, 0.U, o_cnt + 1.U)
        ENABLE_REG := Mux(o_cnt===(rows-1).U, false.B, ENABLE_REG)
    }
    .elsewhen(state===sFinish){
        state := sIdle
    }

    io.mmio.WEN := state===sFinish
    io.mmio.STATUS_IN := state===sFinish
    io.mmio.ENABLE_IN := ENABLE_REG
    // state := Mux(!io.mmio.ENABLE,sReady,sPreload)
    
    
    
//     //--------------------------------------------------------------------
//     io.c_rdata := c_rdata

}

object SA extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(
        new SA(4,4,32,64,32),
        Array("-td","./generated/SA")
    )
}
