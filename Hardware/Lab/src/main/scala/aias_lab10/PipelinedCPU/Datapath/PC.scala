package aias_lab10.PiplinedCPU.Datapath

import chisel3._
import chisel3.util._
import aias_lab10.PiplinedCPU.pc_sel_map._

class PC(addrWidth:Int) extends Module {
    val io = IO(new Bundle{
        // Hcf
        val Hcf = Input(Bool())
        // stall
        val Stall = Input(Bool())
        val PCSel = Input(UInt(2.W))
        val Predict_Target_pc = Input(UInt(addrWidth.W))
        val EXE_Target_pc = Input(UInt(addrWidth.W))
        val EXE_pc = Input(UInt(addrWidth.W))
        
        val pc = Output(UInt(addrWidth.W))
    })
    
    val pcReg = RegInit(0.U(addrWidth.W))

    when(!io.Hcf && !io.Stall){
        pcReg := MuxLookup(io.PCSel, (pcReg + 4.U(addrWidth.W)), Seq(
            IF_PC_PLUS_4 -> (pcReg + 4.U(addrWidth.W)),
            IF_P_T_PC -> io.Predict_Target_pc,
            EXE_PC_PLUS_4 -> (io.EXE_pc + 4.U(addrWidth.W)),
            EXE_T_PC -> io.EXE_Target_pc
        ))
    }.otherwise{
        pcReg := pcReg
    }
    
    io.pc := pcReg
}
