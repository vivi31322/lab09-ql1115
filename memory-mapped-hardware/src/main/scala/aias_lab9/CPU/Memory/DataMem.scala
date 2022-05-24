package aias_lab9.Single_Cycle.Memory

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile

import aias_lab9.AXILite.AXILiteSlaveIF

object wide {
  val Byte = 0.U
  val Half = 1.U
  val Word = 2.U
  val UByte = 4.U
  val UHalf = 5.U
}

import wide._

class DataMem(size:Int, addrWidth: Int, dataWidth: Int) extends Module {
  val io = IO(new Bundle{
    val Hcf   = Input(Bool())
    val slave = new AXILiteSlaveIF(addrWidth, dataWidth)
  })

  val DATA_OFFSET = 1<<size

  val memory = SyncReadMem((1<<(size)), UInt(8.W))
  loadMemoryFromFile(memory, "./src/main/resource/CPU/data.hex")

  val srdata = Wire(SInt(32.W))
  val wa = WireDefault(0.U(addrWidth.W))
  val wd = WireDefault(0.U(32.W))

  //AXI area=========================================================================
  // register for write address channel ready signal
  val writeAddrReadyReg = RegInit(false.B)
  val canDoWrite = io.slave.writeAddr.valid && io.slave.writeData.valid && !writeAddrReadyReg
  writeAddrReadyReg := canDoWrite
  io.slave.writeAddr.ready  := writeAddrReadyReg


  // register for write data channel ready signal
  val writeDataReadyReg = RegNext(canDoWrite)
  io.slave.writeData.ready := writeDataReadyReg

  // register for keeping write address
  val writeAddrReg = RegInit(0.U(16.W))
  when (canDoWrite) {writeAddrReg := io.slave.writeAddr.bits.addr}

  // register bank write
  val doWrite = writeDataReadyReg && io.slave.writeData.valid && writeAddrReadyReg && io.slave.writeAddr.valid
  val writeRegSelect = writeAddrReg & ~(3.U(addrWidth.W))
  // note that we write the entire word (no byte select using write strobe)
  when (doWrite) {
    memory(writeRegSelect+0.U(16.W)) := io.slave.writeData.bits.data(7,0)
    memory(writeRegSelect+1.U(16.W)) := io.slave.writeData.bits.data(15,8)
    memory(writeRegSelect+2.U(16.W)) := io.slave.writeData.bits.data(23,16)
    memory(writeRegSelect+3.U(16.W)) := io.slave.writeData.bits.data(31,24)
  }

  // write response generation
  io.slave.writeResp.bits   := 0.U     // always OK
  val writeRespValidReg = RegInit(false.B)
  writeRespValidReg := doWrite && !writeRespValidReg
  io.slave.writeResp.valid  := writeRespValidReg

  // read address ready generation
  val readAddrReadyReg = RegInit(false.B)
  val canDoRead = !readAddrReadyReg && io.slave.readAddr.valid
  readAddrReadyReg := canDoRead
  io.slave.readAddr.ready := readAddrReadyReg

  // read address latching
  val readAddrReg = RegInit(0.U(16.W))
  when (canDoRead) { readAddrReg := io.slave.readAddr.bits.addr }

  // read data valid and response generation
  val readDataValidReg = RegInit(false.B)
  val doRead = readAddrReadyReg && io.slave.readAddr.valid && !readDataValidReg
  readDataValidReg := doRead

  io.slave.readData.valid := readDataValidReg

  // register bank read
  val readRegSelect = io.slave.readAddr.bits.addr & ~(3.U(addrWidth.W))
  val outputReg = RegInit(0.U(dataWidth.W))
  outputReg := Mux((io.slave.readAddr.ready && io.slave.readAddr.valid),srdata.asUInt,0.U)
  io.slave.readData.bits.data   := outputReg

  srdata := Cat(
                memory(readRegSelect +3.U),
                memory(readRegSelect +2.U),
                memory(readRegSelect +1.U),
                memory(readRegSelect +0.U)
                ).asSInt














  //software area in order to printout the mem value
  when(io.Hcf){
    printf("\n\t\tData Memory Value: (Unit:Word) \n")

    for(i <- 0 until 100){
      var data = Cat(memory(4*i+3),memory(4*i+2),memory(4*i+1),memory(4*i))
      var index = String.format("%" + 2 + "s", i.toString).replace(' ', '0')
      printf(p"\t\tdata[${index}] = 0x${Hexadecimal(data)}\n")
    }
    printf("\n")
  }

}

object DataMem extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(
      new DataMem(15,32,32),
      Array("-td","./generated/DataMem")
    )
}
