package aias_lab9.Single_Cycle.Datapath

import chisel3._
import chisel3.util._

class PC extends Module {
    val io = IO(new Bundle{
        val Hcf = Input(Bool())
        val PCSel = Input(Bool())
        val alu_out = Input(UInt(32.W))
        val pc = Output(UInt(15.W))

        //AXI
        val pc_stall = Input(Bool())
        val pc_go = Input(Bool())
    })
    
    val pcReg = RegInit(0.U(32.W))

    when(!io.Hcf){
        when(io.PCSel){
            io.alu_out & ~((3.U)(32.W))
        }.elsewhen(io.pc_go){
            pcReg := pcReg + 4.U
        }.elsewhen(io.pc_stall){
            pcReg := pcReg
        }.otherwise{
            pcReg := pcReg + 4.U
        }
        // pcReg := Mux(io.PCSel, io.alu_out & ~((3.U)(32.W)), pcReg + 4.U)
    }.otherwise{
        pcReg := pcReg
    }
    
    
    io.pc := pcReg
}
