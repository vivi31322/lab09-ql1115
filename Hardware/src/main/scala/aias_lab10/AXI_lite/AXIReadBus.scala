package aias_lab10.AXILite

import chisel3._
import chisel3.util._

class readMaster(val addrWidth: Int, val dataWidth: Int) extends Bundle {
  val readAddr = Flipped(Decoupled(new AXILiteAddress(addrWidth)))
  val readData = Decoupled(new AXILiteReadData(dataWidth))
}

class readSlave(val addrWidth: Int, val dataWidth: Int) extends Bundle {
  val readAddr = Decoupled(new AXILiteAddress(addrWidth))
  val readData = Flipped(Decoupled(new AXILiteReadData(dataWidth)))
}

class AXIReadBus(val mSlaves: Int, val addrWidth: Int, val dataWidth: Int, val addrMap: List[(UInt, UInt)]) extends Module {
  val io = IO(new Bundle {
    val master = new readMaster(addrWidth, dataWidth)
    val slave = Vec(mSlaves, new readSlave(addrWidth, dataWidth))
  })

  val read_port = WireDefault(0.U(1.W))
  val read_port_reg = RegInit(0.U(1.W))
  val read_addr_reg = RegInit(0.U((addrWidth).W))
  val read_addr_reg_valid = RegInit(false.B)
  val slave_read_startAddr = Wire(Vec(mSlaves, UInt(addrWidth.W)))
  val slave_read_endAddr = Wire(Vec(mSlaves, UInt(dataWidth.W)))

  for (i <- 0 until addrMap.length) {
    slave_read_startAddr(i) := addrMap(i)._1
    slave_read_endAddr(i) := addrMap(i)._2
  }

  for (i <- 0 until mSlaves) {
    io.slave(i).readAddr.valid := false.B
    io.slave(i).readData.ready := false.B
    io.slave(i).readAddr.bits.addr := 0.U
    when(slave_read_startAddr(i) <= io.master.readAddr.bits.addr && io.master.readAddr.bits.addr < slave_read_endAddr(i)) {
      read_port := i.U // 找出slave的port
    }
  }

  io.master.readData.valid := false.B
  io.master.readAddr.ready := false.B
  io.master.readData.bits.data := 0.U

  when(io.master.readAddr.valid && read_addr_reg_valid === false.B) {
    read_port_reg := read_port
    read_addr_reg := io.master.readAddr.bits.addr
    read_addr_reg_valid := true.B
  }.otherwise {
    read_addr_reg := read_addr_reg
    read_addr_reg_valid := read_addr_reg_valid
  }

  when(read_addr_reg_valid) {
    io.master.readAddr.ready := false.B
  }.otherwise {
    io.master.readAddr.ready := true.B
  }

  when(io.slave(read_port_reg).readData.valid && read_addr_reg_valid) {
    io.master.readData.valid := true.B
    io.master.readData.bits.data := io.slave(read_port_reg).readData.bits.data
    io.master.readData.bits.resp := io.slave(read_port_reg).readData.bits.resp
    when(io.master.readData.ready) {
      read_addr_reg_valid := false.B
      io.slave(read_port_reg).readData.ready := true.B
    }.otherwise {
      io.master.readData.valid := false.B
      io.slave(read_port_reg).readData.ready := false.B
    }
  }.otherwise {
    io.master.readData.valid := false.B
    io.master.readData.bits.data := 0.U
    io.master.readData.bits.resp := 0.U
    io.slave(read_port_reg).readData.ready := false.B
  }

  io.slave(read_port_reg).readAddr.bits.addr := read_addr_reg
  io.slave(read_port_reg).readAddr.valid := read_addr_reg_valid
}
