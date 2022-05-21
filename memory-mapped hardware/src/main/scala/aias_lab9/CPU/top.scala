package aias_lab9

import chisel3._
import chisel3.util.{Arbiter=>_,_}

import aias_lab9.Single_Cycle._
import aias_lab9.Memory._
import aias_lab9.AXILiteDefs._

class top extends Module {
    val io = IO(new Bundle{
        val pc = Output(UInt(15.W))
        val regs = Output(Vec(32,UInt(32.W)))
        val Hcf = Output(Bool())
    })
    

    // the text segment and data segment are totally 64kB === 1<<16, and they share half of it for each
    val sc = Module(new Single_Cycle(32,32))
    val im = Module(new InstMem(15))
    val dm = Module(new DataMem(15,32,32))
    val arbiter = Module(new Arbiter(1,1))
    
    //Single_Cycle
    sc.io.rinst := im.io.inst
    
    
    //Insruction Memory
    im.io.raddr := sc.io.pc
    
    //AXI between Single Cycle and Data Memory
    sc.io.master <> arbiter.io.masters(0)
    dm.io.slave <> arbiter.io.slaves(0)

    //System
    io.pc := sc.io.pc
    io.regs := sc.io.regs
    io.Hcf := sc.io.Hcf
    dm.io.Hcf := sc.io.Hcf
}


import chisel3.stage.ChiselStage
object top extends App {
  (
    new chisel3.stage.ChiselStage).emitVerilog(
      new top(),
      Array("-td","generated/top")
  )
}