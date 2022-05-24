package aias_lab9.Memory

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile

import aias_lab9.AXILite._

class DataMem(size: Int, addrWidth: Int, dataWidth: Int, binaryFile: String) extends Module {
  val io = IO(new Bundle {
    val bus_slave = new AXILiteSlaveIF(addrWidth, dataWidth)
  })

  val sIdle :: sReadResp :: sWriteResp :: Nil = Enum(3)

  val stateReg = RegInit(sIdle)
  val memory = Mem((1 << (size)), UInt(8.W))

  loadMemoryFromFile(memory, binaryFile)

  // Next state decoder
  switch(stateReg) {
    is(sIdle) {
      when(io.bus_slave.readAddr.valid) {
        stateReg := sReadResp
      }.elsewhen(io.bus_slave.writeAddr.valid & io.bus_slave.writeData.valid) {
        stateReg := sWriteResp
      }.otherwise {
        stateReg := sIdle
      }
    }
    is(sReadResp) {
      stateReg := Mux((io.bus_slave.readData.ready), sIdle, sReadResp)
    }
    is(sWriteResp) {
      stateReg := Mux((io.bus_slave.writeResp.ready), sIdle, sReadResp)
    }
  }

  // AXI slave interface output - ready / valid
  io.bus_slave.readAddr.ready := false.B
  io.bus_slave.readData.valid := false.B
  io.bus_slave.writeAddr.ready := false.B
  io.bus_slave.writeData.ready := false.B
  io.bus_slave.writeResp.valid := false.B

  switch(stateReg) {
    is(sIdle) {
      io.bus_slave.readAddr.ready := true.B
      io.bus_slave.writeAddr.ready := true.B
      io.bus_slave.writeData.ready := true.B
    }
    is(sReadResp) {
      io.bus_slave.readData.valid := true.B
    }
    is(sWriteResp) {
      io.bus_slave.writeResp.valid := true.B
    }
  }

  // Handle request
  val addrReg = RegInit(0.U(addrWidth.W))

  switch(stateReg) {
    is(sIdle) {
      addrReg := Mux(io.bus_slave.readAddr.valid, io.bus_slave.readAddr.bits.addr - "h8000".U, addrReg)
      when(io.bus_slave.writeAddr.valid & io.bus_slave.writeData.valid) {
        for (i <- 0 until (dataWidth / 8)) {
          memory((io.bus_slave.writeAddr.bits.addr - "h8000".U) + i.U) := Mux(
            (io.bus_slave.writeData.bits.strb(i) === 1.U),
            io.bus_slave.writeData.bits.data(8 * (i + 1) - 1, 8 * i),
            memory((io.bus_slave.writeAddr.bits.addr - "h8000".U) + i.U)
          )
        }
      }
    }
    is(sReadResp) {
      addrReg := addrReg
    }
    is(sWriteResp) {
      addrReg := addrReg
    }
  }

  io.bus_slave.readData.bits.data := Cat(
    memory(addrReg + 7.U), memory(addrReg + 6.U), memory(addrReg + 5.U), memory(addrReg + 4.U),
    memory(addrReg + 3.U), memory(addrReg + 2.U), memory(addrReg + 1.U), memory(addrReg)
  )
  io.bus_slave.readData.bits.resp := 0.U
  io.bus_slave.writeResp.bits := 0.U
}
