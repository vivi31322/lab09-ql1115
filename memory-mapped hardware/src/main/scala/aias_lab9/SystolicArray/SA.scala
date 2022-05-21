package aias_lab9.SystolicArray

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile

// import scala.io.Source

import aias_lab9.SystolicArray.localMem._

class SA(val rows:Int = 4,
           val cols:Int = 4,
           val bits:Int = 8) extends Module{
    val io = IO(new Bundle{

        val a_rdata = Output(UInt(32.W))
        val b_rdata = Output(UInt(32.W))
        val c_rdata = Output(UInt(32.W))
        val check = Output(Bool())
        val output = Output(Vec(4,Valid(UInt((2*bits).W))))

        val mmio = Flipped(new MMIO)
    })

    // Module Declaration
    val lm = Module(new LocalMem)
    val a_b = Module(new buffer)
    val tile = Module(new tile)

    //counter declaration
    val cnt = RegInit(0.U(4.W))    //used for random start
    val w_cnt = RegInit(0.U(4.W))  //used for "weight" data access 
    val i_cnt = RegInit(0.U(4.W))  //used for "input" data access 
    val o_cnt = RegInit(0.U(4.W))  //used for "input" data access 

    // Read Memory Wiring
    
    val a_base_addr = WireDefault((0*rows*cols).U(32.W))
    val b_base_addr = WireDefault((1*rows*cols).U(32.W))
    val c_base_addr = WireDefault((2*rows*cols).U(32.W))
    
    val a_rdata = WireDefault(0.U(32.W))
    val b_rdata = WireDefault(0.U(32.W))
    val c_rdata = WireDefault(0.U(32.W))

    lm.io.raddr(0) := a_base_addr + (i_cnt << 2)
    lm.io.raddr(1) := b_base_addr + (w_cnt << 2)
    lm.io.raddr(2) := c_base_addr + (o_cnt << 2)

    a_rdata := lm.io.rdata(0)    
    b_rdata := lm.io.rdata(1)
    c_rdata := lm.io.rdata(2)
    

    // Write Memory Wiring
    val c_waddr_bits = RegInit(VecInit(Seq.fill(cols)(0.U(log2Ceil(rows*cols).W))))

    // *cols === *4 === <<2
    lm.io.waddr(0).bits := (c_waddr_bits(0) << 2) + 0.U + c_base_addr
    lm.io.waddr(1).bits := (c_waddr_bits(1) << 2) + 1.U + c_base_addr 
    lm.io.waddr(2).bits := (c_waddr_bits(2) << 2) + 2.U + c_base_addr 
    lm.io.waddr(3).bits := (c_waddr_bits(3) << 2) + 3.U + c_base_addr 

    when(io.output(0).valid){c_waddr_bits(0) := c_waddr_bits(0) + 1.U}
    when(io.output(1).valid){c_waddr_bits(1) := c_waddr_bits(1) + 1.U}
    when(io.output(2).valid){c_waddr_bits(2) := c_waddr_bits(2) + 1.U}
    when(io.output(3).valid){c_waddr_bits(3) := c_waddr_bits(3) + 1.U}

    lm.io.waddr(0).valid := io.output(0).valid
    lm.io.waddr(1).valid := io.output(1).valid
    lm.io.waddr(2).valid := io.output(2).valid
    lm.io.waddr(3).valid := io.output(3).valid

    lm.io.wdata(0) <> tile.io.output(0)
    lm.io.wdata(1) <> tile.io.output(1)
    lm.io.wdata(2) <> tile.io.output(2)
    lm.io.wdata(3) <> tile.io.output(3)

    lm.io.finish := io.mmio.ENABLE

    //ENABLE register
    val ENABLE = RegNext(io.mmio.ENABLE)

    //state declaration
    val sIdle :: sReady  :: sPreload :: sPropagate :: sCheck :: sFinish :: Nil = Enum(6)
    val state = RegInit(sIdle)

    // prl : preload 
    val prl = WireDefault(false.B)
    prl := state === sPreload

    // ready signal from tile
    val ready = WireDefault(false.B)
    ready := state === sReady

    // check signal for checking the correctness of the data in c_mem
    io.check := state === sCheck

    //tile wiring
    tile.io.input <> a_b.io.output

    tile.io.weight(0).bits := lm.io.rdata(1)(31,24)
    tile.io.weight(1).bits := lm.io.rdata(1)(23,16)
    tile.io.weight(2).bits := lm.io.rdata(1)(15,8)
    tile.io.weight(3).bits := lm.io.rdata(1)(7,0)

    tile.io.weight(0).valid := Mux(state===sPreload,true.B,false.B)
    tile.io.weight(1).valid := Mux(state===sPreload,true.B,false.B)
    tile.io.weight(2).valid := Mux(state===sPreload,true.B,false.B)
    tile.io.weight(3).valid := Mux(state===sPreload,true.B,false.B)

    io.output <> tile.io.output
    tile.io.preload := prl

    //buffer wiring
    a_b.io.input(0).bits := Mux(state === sPropagate && i_cnt < cols.U , a_rdata(7,0), 0.U )
    a_b.io.input(1).bits := Mux(state === sPropagate && i_cnt < cols.U , a_rdata(15,8), 0.U )
    a_b.io.input(2).bits := Mux(state === sPropagate && i_cnt < cols.U , a_rdata(23,16), 0.U )
    a_b.io.input(3).bits := Mux(state === sPropagate && i_cnt < cols.U , a_rdata(31,24), 0.U )

    a_b.io.input(0).valid := Mux(state === sPropagate && i_cnt < cols.U, true.B, false.B)
    a_b.io.input(1).valid := Mux(state === sPropagate && i_cnt < cols.U, true.B, false.B)
    a_b.io.input(2).valid := Mux(state === sPropagate && i_cnt < cols.U, true.B, false.B)
    a_b.io.input(3).valid := Mux(state === sPropagate && i_cnt < cols.U, true.B, false.B)



    when(state===sIdle){
        val rnd_start = 3.U
        cnt := cnt + 1.U
        state := Mux(cnt === rnd_start,sReady,sIdle)
    }
    when(state===sReady){
        state := Mux(io.mmio.ENABLE,sPreload,sReady)
    }
    when(state===sPreload){
        when(w_cnt === (rows-1).U){
            state := sPropagate
        }
        w_cnt := Mux(w_cnt === (rows-1).U, 0.U, w_cnt + 1.U)
    }
    when(state===sPropagate){
        when(!tile.io.output(cols-2).valid && tile.io.output(cols-1).valid){
            state := sCheck
        }
        i_cnt := Mux(i_cnt===cols.U, i_cnt, i_cnt+1.U)
    }
    when(state===sCheck){
        o_cnt := Mux(o_cnt===(rows-1).U , 0.U, o_cnt + 1.U)
        when(o_cnt===(rows-1).U){
            state := sFinish
        }
    }

    io.mmio.STATUS := Mux(state===sFinish,true.B,false.B)


    //--------------------------------------------------------------------
    io.a_rdata := a_rdata
    io.b_rdata := b_rdata
    io.c_rdata := c_rdata

}

object SA extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(
        new SA(),
        args
    )
}