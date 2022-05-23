package aias_lab9.SystolicArray

import chisel3._
import chisel3.util._

import aias_lab9.AXILiteDefs._

class MMIO extends Bundle{
    val ENABLE              = Output(Bool())
    val STATUS              = Input(Bool())
    val MATA_SIZE           = Output(UInt(32.W))
    val MATB_SIZE           = Output(UInt(32.W))
    val MATC_SIZE           = Output(UInt(32.W))
    val MATA_MEM_ADDR       = Output(UInt(32.W))
    val MATB_MEM_ADDR       = Output(UInt(32.W))
    val MATC_MEM_ADDR       = Output(UInt(32.W))
    val MAT_MEM_STRIDE      = Output(UInt(32.W))
    val MAT_GOLDEN_MEM_ADDR = Output(UInt(32.W))
}

class MMIO_Regfile extends Module{
    val io = IO(new Bundle{
        //for SystolicArray
        val mmio = new MMIO

        val raddr = Input(UInt(32.W))
        val rdata = Output(UInt(32.W))
        val waddr = Input(UInt(32.W))
        val wen = Input(Bool())
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

    io.mmio.ENABLE              := RegFile(0)            
    io.mmio.MATA_SIZE           := RegFile(2)
    io.mmio.MATB_SIZE           := RegFile(3)
    io.mmio.MATC_SIZE           := RegFile(4)
    io.mmio.MATA_MEM_ADDR       := RegFile(5)
    io.mmio.MATB_MEM_ADDR       := RegFile(6)
    io.mmio.MATC_MEM_ADDR       := RegFile(7)
    io.mmio.MAT_MEM_STRIDE      := RegFile(8)
    io.mmio.MAT_GOLDEN_MEM_ADDR := RegFile(9)

    RegFile(1)                  := io.mmio.STATUS.asUInt


    io.rdata := RegFile(io.raddr)
    when(io.wen){
        RegFile(io.waddr) :=  io.wdata
    }
    

}

object MMIO_Regfile extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(
        new MMIO_Regfile,
        args
    )
}
