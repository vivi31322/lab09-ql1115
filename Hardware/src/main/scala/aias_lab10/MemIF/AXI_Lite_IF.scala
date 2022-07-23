package aias_lab10.MemIF

import chisel3._
import chisel3.util._

import aias_lab10.AXILite._

/* Direct Memory InterFace */
class AXI_Lite_IF(memAddrWidth: Int, memDataWidth: Int) extends Module {
  val io = IO(new Bundle {
    val memIF = new MemIF_MEM(memAddrWidth)

    // AXI
    val bus_master = new AXILiteMasterIF(memAddrWidth, memDataWidth)
  })

  // TBD
/*
  io.Mem_RW := io.memIF.Mem_R || io.memIF.Mem_W
  io.Length = io.memIF.Length  

  io.memIF.Valid := true.B

	io.raddr := io.memIF.raddr
	io.memIF.rdata := io.rdata

	io.waddr := io.memIF.waddr
	io.wdata := io.memIF.wdata
  */
} 
    


/*
val sNormal :: sAXIReadSend :: sAXIReadWait :: sAXIWriteSend :: sAXIWriteWait :: Nil = Enum(5)

  val DataMemAccessState = RegInit(sNormal)

  val isDataLoad = Wire(Bool())
  val isDataStore = Wire(Bool())
  isDataLoad := (opcode === LOAD | opcode === VL)
  isDataStore := (opcode === STORE | opcode === VS)

  // DataMemAccessState - next state decoder
  switch(DataMemAccessState) {
    is(sNormal) {
      when(isDataLoad) {
        DataMemAccessState := Mux(io.readAddr.ready, sAXIReadWait, sAXIReadSend)
      }.elsewhen(isDataStore) {
        DataMemAccessState := Mux((io.writeAddr.ready & io.writeData.ready), sAXIWriteWait, sAXIWriteSend)
      }.otherwise {
        DataMemAccessState := sNormal
      }
    }
    is(sAXIReadSend) {
      DataMemAccessState := Mux(io.readAddr.ready, sAXIReadWait, sAXIReadSend)
    }
    is(sAXIReadWait) {
      DataMemAccessState := Mux(io.readData.valid, sNormal, sAXIReadWait)
    }
    is(sAXIWriteSend) {
        DataMemAccessState := Mux((io.writeAddr.ready & io.writeData.ready), sAXIWriteWait, sAXIWriteSend)
    }
    is(sAXIWriteWait) {
      DataMemAccessState := Mux(io.writeResp.valid, sNormal, sAXIWriteWait)
    }
  }

  // AXI output gnenrator
  io.readAddr.valid := false.B
  io.readAddr.bits.addr := 0.U
  io.readData.ready := false.B
  io.writeAddr.valid := false.B
  io.writeAddr.bits.addr := 0.U
  io.writeData.valid := false.B
  io.writeData.bits.data := 0.U
  io.writeData.bits.strb := 0.U
  io.writeResp.ready := false.B

  switch(DataMemAccessState) {
    is(sNormal) {
      io.readAddr.valid := isDataLoad
      io.writeAddr.valid := (isDataStore & io.writeAddr.ready & io.writeData.ready)
      io.writeData.valid := (isDataStore & io.writeAddr.ready & io.writeData.ready)
    }
    is(sAXIReadSend) {
      io.readAddr.valid := Mux(io.readAddr.ready, true.B, false.B)
    }
    is(sAXIReadWait) {
      io.readData.ready := true.B
    }
    is(sAXIWriteSend) {
      io.writeAddr.valid := (isDataStore & io.writeAddr.ready & io.writeData.ready)
      io.writeData.valid := (isDataStore & io.writeAddr.ready & io.writeData.ready)
    }
    is(sAXIWriteWait) {
      io.writeResp.ready := true.B
    }
  }
  */