package aias_lab9.SystolicArray

import chisel3._
import chisel3.util._

import aias_lab9.AXILite._

class Memory_Mapped(val addrWidth:Int=32,
                    val dataWidth:Int=32) extends Module{
    val io = IO(new Bundle{
        //for CPU to access the Reg and Memory
        val slave = new AXILiteSlaveIF(addrWidth, dataWidth)

        //for reg to access SA
        val mmio = new MMIO

        //for SA to read/write LocalMem when it still a slave
        val raddr = Input(UInt(32.W))
        val rdata = Output(UInt(32.W))

        val wen   = Input(Bool())
        val waddr = Input(UInt(32.W))
        val wdata = Input(UInt(32.W))

        // for making localMem print the value
        val finish = Input(Bool())
    })

    val rf = Module(new MMIO_Regfile)
    val lm = Module(new LocalMem)

    val ACCEL_REG_BASE_ADDR = 0x100000
    val ACCEL_MEM_BASE_ADDR = 0x200000


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
        lm.io.wen := false.B
        lm.io.finish := io.finish

    //r/w port default value
        io.rdata := 0.U

    // the Regs used for CPU dominated
    val RAReg = RegInit(0.U(32.W))
    val RAReadyReg = RegInit(false.B)
    
    val RDReg = RegInit(0.U(32.W))
    val RRReg = RegInit(false.B)
    val RDValidReg = RegInit(false.B)

    val canDoRead = WireDefault(io.slave.readAddr.valid && !RAReadyReg)
    val DoRead = WireDefault(io.slave.readAddr.valid && io.slave.readAddr.ready && !RDValidReg)

    val WAReg = RegInit(0.U(32.W))
    val WAReadyReg = RegInit(false.B)

    val WDReg = RegInit(0.U(32.W))
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
        when(canDoRead){RAReg := io.slave.readAddr.bits.addr & ~(3.U(addrWidth.W))}

        // which module is read depends on addr
        when(RAReg < ACCEL_MEM_BASE_ADDR.U){
            rf.io.raddr := RAReg >> 2
        }.otherwise{
            lm.io.raddr := RAReg - ACCEL_MEM_BASE_ADDR.U
        }

        RDValidReg := DoRead
        RRReg := DoRead
        io.slave.readData.valid := RDValidReg

        when(RAReg < ACCEL_MEM_BASE_ADDR.U){
            RDReg := Mux(DoRead,rf.io.rdata,0.U)
        }.otherwise{
            RDReg := Mux(DoRead,lm.io.rdata,0.U)
        }
        io.slave.readData.bits.data := RDReg

        
        //write behavior
        WAReadyReg := canDoWrite
        WDReadyReg := canDoWrite

        io.slave.writeAddr.ready := WAReadyReg
        io.slave.writeData.ready := WDReadyReg

        when (canDoWrite){
            WAReg := io.slave.writeAddr.bits.addr & ~(3.U(addrWidth.W))
            WDReg := io.slave.writeData.bits.data
        }

        
        when(DoWrite){
            rf.io.waddr := WAReg >> 2
            lm.io.waddr := WAReg - ACCEL_MEM_BASE_ADDR.U
            rf.io.wdata := WDReg
            lm.io.wdata := WDReg
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
        lm.io.wen := io.wen

        // write status and enable to Regfile
        rf.io.mmio.WEN := io.mmio.WEN
        rf.io.mmio.ENABLE_IN := io.mmio.ENABLE_IN
        rf.io.mmio.STATUS_IN := io.mmio.STATUS_IN
    }
}

object Memory_Mapped extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(
        new Memory_Mapped,
        Array("-td","./generated/Memory_Mapped")
    )
}
