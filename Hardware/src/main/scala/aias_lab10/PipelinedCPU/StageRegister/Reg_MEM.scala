package aias_lab10.PiplinedCPU.StageRegister

import chisel3._
import chisel3.util._

class Reg_MEM(addrWidth:Int) extends Module {
    val io = IO(new Bundle{
        val Stall = Input(Bool())
        
        val pc_in = Input(UInt(addrWidth.W))
        val inst_in = Input(UInt(32.W))
        val alu_out_in = Input(UInt(32.W))
        val rs2_rdata_in = Input(UInt(32.W))

        val pc = Output(UInt(addrWidth.W))
        val inst = Output(UInt(32.W))
        val alu_out = Output(UInt(32.W))
        val rs2_rdata = Output(UInt(32.W))
    })
    
    // stage Registers
    val InstReg = RegInit(0.U(32.W))
    val pcReg =  RegInit(0.U(addrWidth.W))
    val aluReg = RegInit(0.U(32.W))
    val rs2Reg = RegInit(0.U(32.W))

    /*** stage Registers Action ***/
    when(io.Stall){
        InstReg := InstReg
        pcReg := pcReg
        aluReg := aluReg
        rs2Reg := rs2Reg
    }.otherwise{
        InstReg := io.inst_in
        pcReg := io.pc_in
        aluReg := io.alu_out_in
        rs2Reg := io.rs2_rdata_in
    }
 
    io.inst := InstReg
    io.pc := pcReg
    io.alu_out := aluReg
    io.rs2_rdata := rs2Reg
}
