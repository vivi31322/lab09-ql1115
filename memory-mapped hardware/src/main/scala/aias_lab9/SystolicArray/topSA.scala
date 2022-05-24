package aias_lab9.SystolicArray

import chisel3._
import chisel3.util._

import aias_lab9.AXI._

class topSA extends Module{
    val io = IO(new Bundle{
        val slave = new AXILiteSlaveIF
    })

    val sa = Module(new SA)
    val mm = Module(new Memory_Mapped)

    io.slave <> mm.io.slave
    mm.io.mmio <> sa.io.mmio
    mm.io.raddr <> sa.io.raddr    
    mm.io.rdata <> sa.io.rdata
    mm.io.waddr <> sa.io.waddr
    mm.io.wdata <> sa.io.wdata
    mm.io.wen <> sa.io.wen
    mm.io.finish <> sa.io.finish
}

object topSA extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(
        new topSA,
        Array("-td","./generated/topSA")
    )
}