package aias_lab9.SystolicArray

import chisel3._
import chisel3.util._

import aias_lab9.AXILite._

class topSA(addr_width: Int, data_width: Int) extends Module{
    val io = IO(new Bundle{
        val slave = new AXILiteSlaveIF(addr_width, data_width)
    })

    val sa = Module(new SA(4,4,addr_width, data_width))
    val mm = Module(new Memory_Mapped(0x8000,addr_width, data_width))

    io.slave <> mm.io.slave
    mm.io.mmio <> sa.io.mmio
    mm.io.raddr <> sa.io.raddr
    mm.io.rdata <> sa.io.rdata
    mm.io.waddr <> sa.io.waddr
    mm.io.wdata <> sa.io.wdata
    mm.io.wen <> sa.io.wen
    mm.io.finish <> sa.io.finish
}
