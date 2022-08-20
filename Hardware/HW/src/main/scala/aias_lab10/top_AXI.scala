package aias_lab10

import chisel3._
import chisel3.util._

import aias_lab10.PiplinedCPU._
import aias_lab10.Memory._
import aias_lab10.MemIF._
import aias_lab10.AXI._

object config {
  val addr_width = 16
  val data_width = 32
  val addr_map = List(("h8000".U, "h10000".U))
  val data_mem_size = 15 // power of 2 in byte
  val inst_mem_size = 15 // power of 2 in byte
  val data_hex_path = "./src/main/resource/data.hex"
}

import config._

class top_AXI extends Module {
    val io = IO(new Bundle{
        val pc = Output(UInt(addr_width.W))
        val regs = Output(Vec(data_width,UInt(data_width.W)))
        val Hcf = Output(Bool())

        /* HW: Modification of Vector Extension */
        //val vector_regs  = Output(Vec(32,UInt(64.W)))

        //for sure that IM and DM will be synthesized
        val inst = Output(UInt(32.W))
        val rdata = Output(UInt(32.W))
        val wdata  = Output(UInt(32.W))

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
        val EXE_Jump = Output(Bool())
        val EXE_Branch = Output(Bool())

        val dm_is_Write = Output(Bool())
        val dm_is_Read = Output(Bool())
        val dm_addr = Output(UInt(addr_width.W))
        var dm_wdata = Output(UInt(data_width.W)) 
        var dm_rdata = Output(UInt(data_width.W)) 

    })

    val cpu = Module(new PiplinedCPU(addr_width,64))
    val bus = Module(new AXIXBar(1, addr_map.length, addr_width, data_width, addr_map))
    val im = Module(new InstMem(inst_mem_size))
    val dm = Module(new DataMem_AXI(data_mem_size, addr_width, data_width, "./src/main/resource/data.hex"))
    val axi_if = Module(new AXI_IF(addr_width,64,data_width))
    
    // Insruction - CPU
    cpu.io.InstMem.rdata := im.io.inst
    cpu.io.InstMem.Valid := true.B // Direct to Mem
    im.io.raddr := cpu.io.InstMem.raddr(inst_mem_size-1,0)
    
    // CPU - AXI BUS
    cpu.io.DataMem <> axi_if.io.memIF
    axi_if.io.bus_master <> bus.io.masters(0)

    // Data Memory - AXI BUS
    bus.io.slaves(0) <> dm.io.bus_slave

    //System
    io.pc := cpu.io.InstMem.raddr // (PC)
    io.regs := cpu.io.regs
    io.Hcf := cpu.io.Hcf
    io.inst := im.io.inst
    io.rdata := cpu.io.DataMem.rdata(data_width-1,0)
    io.wdata := cpu.io.DataMem.wdata(data_width-1,0)
    /* HW: Modification of Vector Extension */
    //io.vector_regs := cpu.io.vector_regs

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
    io.EXE_Jump := cpu.io.EXE_Jump
    io.EXE_Branch := cpu.io.EXE_Branch

    io.dm_is_Write := dm.io.is_Write
    io.dm_is_Read := dm.io.is_Read
    io.dm_addr := dm.io.addr
    io.dm_wdata := dm.io.wdata
    io.dm_rdata := dm.io.rdata
}


import chisel3.stage.ChiselStage
object top_AXI extends App {
  (
    new chisel3.stage.ChiselStage).emitVerilog(
      new top_AXI(),
      Array("-td","generated/top_AXI")
  )
}