package aias_lab9.SystolicArray

import chisel3._
import chisel3.util._

import aias_lab9.AXILite._

class MMIO extends Bundle{
    val ENABLE_OUT          = Output(Bool())
    val STATUS_OUT          = Output(Bool())
    val MATA_SIZE           = Output(UInt(32.W))
    val MATB_SIZE           = Output(UInt(32.W))
    val MATC_SIZE           = Output(UInt(32.W))
    val MATA_MEM_ADDR       = Output(UInt(32.W))
    val MATB_MEM_ADDR       = Output(UInt(32.W))
    val MATC_MEM_ADDR       = Output(UInt(32.W))
    val MAT_MEM_STRIDE      = Output(UInt(32.W))
    val MAT_GOLDEN_MEM_ADDR = Output(UInt(32.W))

    val ENABLE_IN           = Input(Bool())
    val STATUS_IN           = Input(Bool())
}

class MMIO_Regfile extends Module{
    val io = IO(new Bundle{
        // for SystolicArray MMIO
        val mmio = new MMIO

        // for Memory Mapped to r/w reg value
        val raddr = Input(UInt(32.W))
        val rdata = Output(UInt(32.W))

        val wen   = Input(Bool())
        val waddr = Input(UInt(32.W))
        val wdata = Input(UInt(32.W))
    })

    val initial_table = Seq(
        0.U(32.W),              // ENABLE             
        0.U(32.W),              // STATUS             
        "h00040004".U(32.W),    // MATA_SIZE          
        "h00040004".U(32.W),    // MATB_SIZE          
        "h00040004".U(32.W),    // MATC_SIZE          
        0.U(32.W),              // MATA_MEM_ADDR                   
        16.U(32.W),             // MATB_MEM_ADDR      
        32.U(32.W),             // MATC_MEM_ADDR      
        "h010101".U(32.W),      // MAT_MEM_STRIDE     
        48.U(32.W)              // MAT_GOLDEN_MEM_ADDR
    )

    val RegFile = RegInit(VecInit(initial_table))

    // MMIO circuit declaration
        //Output
        io.mmio.ENABLE_OUT          := RegFile(0)
        io.mmio.STATUS_OUT          := RegFile(1)     
        io.mmio.MATA_SIZE           := RegFile(2)
        io.mmio.MATB_SIZE           := RegFile(3)
        io.mmio.MATC_SIZE           := RegFile(4)
        io.mmio.MATA_MEM_ADDR       := RegFile(5)
        io.mmio.MATB_MEM_ADDR       := RegFile(6)
        io.mmio.MATC_MEM_ADDR       := RegFile(7)
        io.mmio.MAT_MEM_STRIDE      := RegFile(8)
        io.mmio.MAT_GOLDEN_MEM_ADDR := RegFile(9)

        //Input
        RegFile(1) := io.mmio.STATUS_IN.asUInt
        RegFile(0) := io.mmio.ENABLE_IN.asUInt
    
    // r/w function declaration
        io.rdata := RegFile(io.raddr)
        when(io.wen){RegFile(io.waddr) := io.wdata}
}

object MMIO_Regfile extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(
        new MMIO_Regfile,
        args
    )
}
