package aias_lab10.AXILite

import chisel3._
import chisel3.util._

// the required signals on an AXI4-Lite interface
// 0. Global
        // ACLK
        // ARESETn
// 1. Write address channel
        // AWVALID
        // AWREADY
        // AWADDR
        // AWPROT
// 2. Write data channel
        // WVALID
        // WREADY
        // WDATA
        // WSTRB
// 3. Write response channel
        // BVALID
        // BREADY
        // BRESP
// 4. Read address channel
        // ARVALID
        // ARREADY
        // ARADDR
        // ARPROT
// 5. Read data channel
        // RVALID
        // RREADY
        // RDATA
        // RRESP

class AXILiteAddress(val addrWidth: Int) extends Bundle {
  val addr = UInt(addrWidth.W)
  override def clone = { new AXILiteAddress(addrWidth).asInstanceOf[this.type] }
}

class AXILiteWriteData(val dataWidth: Int) extends Bundle {
  val data = UInt(dataWidth.W)
  val strb = UInt((dataWidth / 8).W) // byte masked
  override def clone = { new AXILiteWriteData(dataWidth).asInstanceOf[this.type] }
}

class AXILiteReadData(val dataWidth: Int) extends Bundle {
  val data = UInt(dataWidth.W)
  val resp = UInt(2.W)
  override def clone = { new AXILiteReadData(dataWidth).asInstanceOf[this.type] }
}

class AXILiteSlaveIF(val addrWidth: Int, val dataWidth: Int) extends Bundle {
  // write address channel
  val writeAddr = Flipped(Decoupled(new AXILiteAddress(addrWidth)))

  // write data channel
  val writeData = Flipped(Decoupled(new AXILiteWriteData(dataWidth)))

  // write response channel
  val writeResp = Decoupled(UInt(2.W))

  // read address channel
  val readAddr = Flipped(Decoupled(new AXILiteAddress(addrWidth)))

  // read data channel
  val readData = Decoupled(new AXILiteReadData(dataWidth))

  override def clone = { new AXILiteSlaveIF(addrWidth, dataWidth).asInstanceOf[this.type] }
}

class AXILiteMasterIF(val addrWidth: Int, val dataWidth: Int) extends Bundle {
  // write address channel
  val writeAddr = Decoupled(new AXILiteAddress(addrWidth))

  // write data channel
  val writeData = Decoupled(new AXILiteWriteData(dataWidth))

  // write response channel
  val writeResp = Flipped(Decoupled(UInt(2.W)))

  // read address channel
  val readAddr = Decoupled(new AXILiteAddress(addrWidth))

  // read data channel
  val readData = Flipped(Decoupled(new AXILiteReadData(dataWidth)))

  override def clone: AXILiteMasterIF = { new AXILiteMasterIF(addrWidth, dataWidth).asInstanceOf[this.type] }
}

class Interface extends MultiIOModule {
  val io = IO(new Bundle {
    val slave = new AXILiteSlaveIF(8, 32)
    val master = new AXILiteMasterIF(8, 32)
  })

  io.slave.writeData.ready := false.B
  io.slave.writeAddr.ready := false.B
  io.slave.readAddr.ready := false.B

  io.slave.writeResp.bits := 0.U
  io.slave.writeResp.valid := false.B

  io.slave.readData.bits.data := 0.U
  // io.slave.readData.bits.resp := 0.U
  io.slave.readData.valid := false.B

  io.master.writeAddr.bits.addr := 0.U
  io.master.readData.ready := false.B
  io.master.writeData.valid := false.B
  io.master.writeAddr.valid := false.B
  io.master.readAddr.bits.addr := 0.U
  io.master.writeResp.ready := false.B
  io.master.writeData.bits.data := 0.U
  io.master.readAddr.valid := false.B
}

object Interface extends App {
  (new stage.ChiselStage).emitVerilog(
    new Interface(),
    Array("-td", "./generated/Interface")
  )
}
