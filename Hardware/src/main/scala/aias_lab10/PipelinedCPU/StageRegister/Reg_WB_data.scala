package aias_lab10.PiplinedCPU.StageRegister

import chisel3._
import chisel3.util._

class Reg_WB_data extends Module {
    val io = IO(new Bundle{
        val Stall = Input(Bool())
        
        val WB_Hazard_in = Input(UInt(2.W)) // (rs2,rs1)
        val wb_data_in = Input(UInt(32.W))

        val WB_Hazard = Output(UInt(2.W))
        val wb_data = Output(UInt(32.W))
    })
    
    // stage Registers
    val WB_Hazard_Reg = RegInit(0.U(2.W))
    val wb_data_Reg = RegInit(0.U(32.W))

    /*** stage Registers Action ***/ 
    when(io.Stall){
        WB_Hazard_Reg := WB_Hazard_Reg
        wb_data_Reg := wb_data_Reg
    }.otherwise{
        WB_Hazard_Reg := io.WB_Hazard_in
        wb_data_Reg := io.wb_data_in
    }
 
    io.WB_Hazard := WB_Hazard_Reg
    io.wb_data := wb_data_Reg
}
