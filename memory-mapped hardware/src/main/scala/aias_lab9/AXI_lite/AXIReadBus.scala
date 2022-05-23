package aias_lab9.AXILite

import chisel3._
import chisel3.util._
import aias_lab9.AXI.ADDR_MAP._

class readMaster extends Bundle{
  val readAddr = Flipped(Decoupled(new AXILiteAddress(32)))
  val readData = Decoupled(new AXILiteReadData(64))
}
class readSlave extends Bundle{
  val readAddr = Decoupled(new AXILiteAddress(32))
  val readData = Flipped(Decoupled(new AXILiteReadData(64)))
}

class AXIReadBus(mSlaves:Int)extends Module{
  val io = IO(new Bundle{
    val master = new readMaster
    val slave = Vec(mSlaves,new readSlave)
    //val decerror =Output(UInt(2.W))
   
  })
  val read_port=WireDefault(0.U(1.W))
  val read_port_reg=RegInit(0.U(1.W))
  val read_addr_reg=RegInit(0.U((32).W))
  val read_addr_reg_valid=RegInit(false.B)
  val slave_read_startAddr = Wire(Vec(mSlaves,UInt(32.W)))
  val slave_read_endAddr = Wire(Vec(mSlaves,UInt(32.W)))

  slave_read_startAddr(0):=SRAM_START_ADDR
  slave_read_endAddr(0):=SRAM_END_ADDR
  slave_read_startAddr(1) := ACCELERATOR_START_ADDR
  slave_read_endAddr(1) := ACCELERATOR_END_ADDR

   for(i <-0 until mSlaves){
    io.slave(i).readAddr.valid:=false.B
    io.slave(i).readData.ready:=false.B
    io.slave(i).readAddr.bits.addr:=0.U
    when(slave_read_startAddr(i)<=io.master.readAddr.bits.addr&&io.master.readAddr.bits.addr<slave_read_endAddr(i)){
      read_port:=i.U    //找出slave的port
    }
  }

  io.master.readData.valid:=false.B
  io.master.readAddr.ready:=false.B
  io.master.readData.bits.data:=0.U


  when(io.master.readAddr.valid&&read_addr_reg_valid===false.B){
    read_port_reg:=read_port
    read_addr_reg:=io.master.readAddr.bits.addr
    read_addr_reg_valid:=true.B
  }.otherwise{
    read_addr_reg:=read_addr_reg
    read_addr_reg_valid:=read_addr_reg_valid
  }

  when(read_addr_reg_valid){
    io.master.readAddr.ready:=false.B
  }.otherwise{
    io.master.readAddr.ready:=true.B
  }
  
  when(io.slave(read_port_reg).readData.valid&&read_addr_reg_valid){
    io.master.readData.valid:=true.B
    io.master.readData.bits.data:=io.slave(read_port_reg).readData.bits.data
    io.master.readData.bits.resp:=io.slave(read_port_reg).readData.bits.resp
    when(io.master.readData.ready){
      read_addr_reg_valid:=false.B
      io.slave(read_port_reg).readData.ready:=true.B
    }.otherwise{
      io.master.readData.valid:=false.B
      io.slave(read_port_reg).readData.ready:=false.B
    }
  }.otherwise{
    io.master.readData.valid:=false.B
    io.master.readData.bits.data:=0.U
    io.master.readData.bits.resp:=0.U
    io.slave(read_port_reg).readData.ready:=false.B
  }    
  
  io.slave(read_port_reg).readAddr.bits.addr:=read_addr_reg
  io.slave(read_port_reg).readAddr.valid:=read_addr_reg_valid
  

}


