package aias_lab9.SystolicArray

import chisel3._
import chisel3.util._

import aias_lab9.AXILite._

class MMIO(data_width:Int) extends Bundle{
    val ENABLE_OUT          = Output(Bool())
    val STATUS_OUT          = Output(Bool())
    val MATA_SIZE           = Output(UInt(data_width.W))
    val MATB_SIZE           = Output(UInt(data_width.W))
    val MATC_SIZE           = Output(UInt(data_width.W))
    val MATA_MEM_ADDR       = Output(UInt(data_width.W))
    val MATB_MEM_ADDR       = Output(UInt(data_width.W))
    val MATC_MEM_ADDR       = Output(UInt(data_width.W))
    val MAT_MEM_STRIDE      = Output(UInt(data_width.W))
    val MAT_GOLDEN_MEM_ADDR = Output(UInt(data_width.W))

    val WEN                 = Input(Bool())
    val ENABLE_IN           = Input(Bool())
    val STATUS_IN           = Input(Bool())
}

class MMIO_Regfile(addr_width:Int,data_width:Int) extends Module{
    val io = IO(new Bundle{
        // for SystolicArray MMIO
        val mmio = new MMIO(data_width)

        // for Memory Mapped to r/w reg value
        val raddr = Input(UInt(addr_width.W))
        val rdata = Output(UInt(data_width.W))

        val wen   = Input(Bool())
        val waddr = Input(UInt(addr_width.W))
        val wdata = Input(UInt(data_width.W))
    })

    val initial_table = Seq(
        0.U(data_width.W),              // ENABLE             
        0.U(data_width.W),              // STATUS             
        "h00040004".U(data_width.W),    // MATA_SIZE          
        "h00040004".U(data_width.W),    // MATB_SIZE          
        "h00040004".U(data_width.W),    // MATC_SIZE          
        0.U(data_width.W),              // MATA_MEM_ADDR                   
        16.U(data_width.W),             // MATB_MEM_ADDR      
        32.U(data_width.W),             // MATC_MEM_ADDR      
        "h010101".U(data_width.W),      // MAT_MEM_STRIDE     
        48.U(data_width.W)              // MAT_GOLDEN_MEM_ADDR
    )

    val RegFile = RegInit(VecInit(initial_table))

    // MMIO circuit declaration
        //Output
        io.mmio.ENABLE_OUT          := RegNext(RegFile(0)(0).asBool)
        io.mmio.STATUS_OUT          := RegNext(RegFile(1)(0).asBool)     
        io.mmio.MATA_SIZE           := RegNext(RegFile(2))
        io.mmio.MATB_SIZE           := RegNext(RegFile(3))
        io.mmio.MATC_SIZE           := RegNext(RegFile(4))
        io.mmio.MATA_MEM_ADDR       := RegNext(RegFile(5))
        io.mmio.MATB_MEM_ADDR       := RegNext(RegFile(6))
        io.mmio.MATC_MEM_ADDR       := RegNext(RegFile(7))
        io.mmio.MAT_MEM_STRIDE      := RegNext(RegFile(8))
        io.mmio.MAT_GOLDEN_MEM_ADDR := RegNext(RegFile(9))

    //Input
    when(io.mmio.WEN){
        RegFile(1) := io.mmio.STATUS_IN.asUInt
        RegFile(0) := io.mmio.ENABLE_IN.asUInt
    }
        
    
    // r/w function declaration
        io.rdata := RegFile(io.raddr)
        when(io.wen){RegFile(io.waddr) := io.wdata}
}

// object MMIO_Regfile extends App{
//     (new chisel3.stage.ChiselStage).emitVerilog(
//         new MMIO_Regfile,
//         args
//     )
// }
