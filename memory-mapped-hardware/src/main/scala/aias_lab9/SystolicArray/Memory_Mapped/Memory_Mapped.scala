package aias_lab9.SystolicArray

import chisel3._
import chisel3.util._

import aias_lab9.AXILite._

class Memory_Mapped(mem_size:Int,
                    addr_width:Int,
                    data_width:Int,
                    reg_width:Int) extends Module{
    val io = IO(new Bundle{
        //for CPU to access the Reg and Memory
        val slave = new AXILiteSlaveIF(addr_width, data_width)

        //for reg to access SA
        val mmio = new MMIO(reg_width)

        //for SA to read/write LocalMem when it still a slave
        val raddr = Input(UInt(addr_width.W))
        val rdata = Output(UInt(data_width.W))

        val wen   = Input(Bool())
        val waddr = Input(UInt(addr_width.W))
        val wdata = Input(UInt(data_width.W))
        val wstrb = Input(UInt((data_width>>3).W))

        // for making localMem print the value
        val finish = Input(Bool())
    })

    val rf = Module(new MMIO_Regfile(addr_width,reg_width))
    val lm = Module(new LocalMem(mem_size,addr_width,data_width))

    val ACCEL_REG_BASE_ADDR = 0x100000
    val ACCEL_MEM_BASE_ADDR = 0x200000
    val byte = 8

    //slave port deafault value
        //WriteData channel
        io.slave.writeData.ready := false.B
        //WriteAddr channel
        io.slave.writeAddr.ready := false.B
        //ReadData channel
        io.slave.readData.bits.data := 0.U
        io.slave.readData.valid := false.B
        io.slave.readData.bits.resp := false.B
        //ReadAddr channel
        io.slave.readAddr.ready := false.B
        //WriteResp channel
        io.slave.writeResp.bits := false.B
        io.slave.writeResp.valid := false.B


    // rf default wiring
        rf.io.mmio <> io.mmio
        rf.io.raddr := 0.U
        rf.io.waddr := 0.U
        rf.io.wdata := 0.U
        rf.io.wen := false.B

    // lm default wiring
        lm.io.raddr := 0.U
        lm.io.waddr := 0.U
        lm.io.wdata := 0.U
        lm.io.wstrb := 0.U
        lm.io.wen := false.B
        lm.io.finish := io.finish

    //r/w port default value
        io.rdata := 0.U
    
    // the Regs used for CPU dominated
    val RAReg = RegInit(0.U(addr_width.W))
    val RAReadyReg = RegInit(false.B)
    
    val RDReg = RegInit(0.U(data_width.W))
    val RRReg = RegInit(false.B)
    val RDValidReg = RegInit(false.B)

    val canDoRead = WireDefault(io.slave.readAddr.valid && !RAReadyReg)
    // seems weird because read behavior of reg and SyncReadMem through AXI are different...
    val DoRead = RegNext(io.slave.readAddr.valid && io.slave.readAddr.ready && !RDValidReg)

    val WAReg = RegInit(0.U(addr_width.W))
    val WAReadyReg = RegInit(false.B)

    val WDReg = RegInit(0.U(data_width.W))
    val WSReg = RegInit(0.U((data_width>>3).W))
    val WDReadyReg = RegInit(false.B)

    val WRValidReg = RegInit(false.B)

    val canDoWrite = WireDefault((io.slave.writeAddr.valid && !WAReadyReg) &&      
                                    (io.slave.writeData.valid && !WDReadyReg))

    val DoWrite = WireDefault((io.slave.writeAddr.valid && io.slave.writeAddr.ready) &&
                                (io.slave.writeData.valid && io.slave.writeData.ready))


    // CPU dominated
    when(!io.mmio.ENABLE_OUT){
        // read behavior
        RAReadyReg := canDoRead
        io.slave.readAddr.ready := RAReadyReg
        RAReg := Mux(canDoRead,io.slave.readAddr.bits.addr,RAReg)
        
        rf.io.raddr := Mux((ACCEL_REG_BASE_ADDR.U <= RAReg && RAReg < ACCEL_MEM_BASE_ADDR.U), (RAReg - ACCEL_REG_BASE_ADDR.U) >> 2, 0.U)
        lm.io.raddr := Mux((ACCEL_MEM_BASE_ADDR.U <= RAReg),(RAReg - ACCEL_MEM_BASE_ADDR.U),0.U)

        RDValidReg := DoRead
        RRReg := DoRead
        io.slave.readData.valid := RDValidReg

        when(RAReg < ACCEL_MEM_BASE_ADDR.U){
            RDReg := Mux(DoRead,Cat(0.U(32.W),rf.io.rdata),0.U)
        }.otherwise{
            RDReg := Mux(DoRead,lm.io.rdata,0.U)
        }
        io.slave.readData.bits.data := RDReg
        io.slave.readData.bits.resp := RRReg

        //==================================================================================================================

        //write behavior
        WAReadyReg := canDoWrite
        WDReadyReg := canDoWrite

        io.slave.writeAddr.ready := WAReadyReg
        io.slave.writeData.ready := WDReadyReg

        WAReg := Mux(canDoWrite,io.slave.writeAddr.bits.addr,0.U)
        WDReg := Mux(canDoWrite,io.slave.writeData.bits.data,0.U)
        WSReg := Mux(canDoWrite,io.slave.writeData.bits.strb,0.U)

        when(DoWrite){
            rf.io.waddr := WAReg >> 2
            lm.io.waddr := WAReg - ACCEL_MEM_BASE_ADDR.U

            rf.io.wdata := WDReg(31,0)
            lm.io.wdata := WDReg

            lm.io.wstrb := WSReg

            rf.io.wen := Mux(io.slave.writeAddr.bits.addr < ACCEL_MEM_BASE_ADDR.U,true.B,false.B)
            lm.io.wen := Mux(io.slave.writeAddr.bits.addr < ACCEL_MEM_BASE_ADDR.U,false.B,true.B)
        }

        WRValidReg := DoWrite && !WRValidReg
        io.slave.writeResp.valid  := WRValidReg
    }
    // SA dominated
    .otherwise{

        //reset the unused Regs
        RAReg := 0.U
        RAReadyReg := false.B
        RDReg := 0.U
        RRReg := false.B
        RDValidReg := false.B
        WAReg := 0.U
        WAReadyReg := false.B
        WDReg := 0.U
        WDReadyReg := false.B
        WRValidReg := false.B


        // read value from localMem
        lm.io.raddr := io.raddr
        io.rdata := lm.io.rdata

        // write value to localMem
        lm.io.waddr := io.waddr
        lm.io.wdata := io.wdata
        lm.io.wstrb := io.wstrb
        lm.io.wen := io.wen

        // write status and enable to Regfile
        rf.io.mmio.WEN := io.mmio.WEN
        rf.io.mmio.ENABLE_IN := io.mmio.ENABLE_IN
        rf.io.mmio.STATUS_IN := io.mmio.STATUS_IN
    }
}
