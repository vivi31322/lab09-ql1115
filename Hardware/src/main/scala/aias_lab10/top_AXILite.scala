package aias_lab10

import chisel3._
import chisel3.util._

import aias_lab10.PiplinedCPU._
import aias_lab10.Memory._
import aias_lab10.MemIF._

/*class top_AXILite extends Module {
    val io = IO(new Bundle{
        val pc = Output(UInt(15.W))
        val regs = Output(Vec(32,UInt(32.W)))
        val Hcf = Output(Bool())

        //for sure that IM and DM will be synthesized
        val inst = Output(UInt(32.W))
        val rdata = Output(UInt(32.W))

        // Test
        val E_Branch_taken = Output(Bool())
        val Flush = Output(Bool())
        val Stall_MA = Output(Bool())
        val Stall_DH = Output(Bool())
        val ID_PC = Output(UInt(32.W))
        val EXE_PC = Output(UInt(32.W))
        val MEM_PC = Output(UInt(32.W))
        val WB_PC = Output(UInt(32.W))
        val EXE_alu_out = Output(UInt(32.W))
        val EXE_src1 = Output(UInt(32.W))
        val EXE_src2 = Output(UInt(32.W))
        val EXE_src1_sel = Output(UInt(2.W))
        val EXE_src2_sel = Output(UInt(2.W))
        val raddr = Output(UInt(32.W))
        val WB_rd = Output(UInt(5.W))
        val WB_wdata = Output(UInt(32.W))

    })

    val cpu = Module(new PiplinedCPU(15,32))
    //val bus = Module(new AXILite)
    val im = Module(new InstMem(15))
    val dm = Module(new DataMem(15))
    
    // Piplined CPU
    cpu.io.InstMem.rdata := im.io.inst
    cpu.io.DataMem.rdata := dm.io.rdata 

    cpu.io.InstMem.Valid := true.B // Direct to Mem
    cpu.io.DataMem.Valid := true.B // Direct to Mem

    // Insruction Memory
    im.io.raddr := cpu.io.InstMem.raddr
    
    //Data Memory
    dm.io.funct3 := cpu.io.DataMem.Length
    dm.io.raddr := cpu.io.DataMem.raddr
    dm.io.wen := cpu.io.DataMem.Mem_W
    dm.io.waddr := cpu.io.DataMem.waddr
    dm.io.wdata := cpu.io.DataMem.wdata

    //System
    io.pc := cpu.io.InstMem.raddr // (PC)
    io.regs := cpu.io.regs
    io.Hcf := cpu.io.Hcf
    io.inst := im.io.inst
    io.rdata := dm.io.rdata

    // Test
    io.E_Branch_taken := cpu.io.E_Branch_taken
    io.Flush := cpu.io.Flush
    io.Stall_MA := cpu.io.Stall_MA
    io.Stall_DH := cpu.io.Stall_DH
    io.ID_PC := cpu.io.ID_PC
    io.EXE_PC := cpu.io.EXE_PC
    io.MEM_PC := cpu.io.MEM_PC
    io.WB_PC := cpu.io.WB_PC
    io.EXE_alu_out := cpu.io.EXE_alu_out
    io.EXE_src1 := cpu.io.EXE_src1
    io.EXE_src2 := cpu.io.EXE_src2
    io.EXE_src1_sel := cpu.io.EXE_src1_sel
    io.EXE_src2_sel := cpu.io.EXE_src2_sel
    io.raddr := cpu.io.DataMem.raddr
    io.WB_rd := cpu.io.WB_rd
    io.WB_wdata := cpu.io.WB_wdata
}


import chisel3.stage.ChiselStage
object top_AXILite extends App {
  (
    new chisel3.stage.ChiselStage).emitVerilog(
      new top_AXILite(),
      Array("-td","generated/top_AXILite")
  )
}*/