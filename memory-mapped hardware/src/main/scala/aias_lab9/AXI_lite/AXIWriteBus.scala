package aias_lab9.AXILite

import chisel3._
import chisel3.util._

object ADDR_MAP{
  val SRAM_START_ADDR = "h8000".U
  val SRAM_END_ADDR = "h10000".U
  val ACCELERATOR_START_ADDR = "h10000".U
  val ACCELERATOR_END_ADDR = "h20000".U
}

import ADDR_MAP._

class writeMaster extends Bundle{
    val writeAddr =Flipped(Decoupled(new AXILiteAddress(32)))
    val writeData =Flipped(Decoupled(new AXILiteWriteData(64)))
    val writeResp = Decoupled(UInt(2.W))
}
class writeSlave extends Bundle{
    val writeAddr = Decoupled(new AXILiteAddress(32))
    val writeData = Decoupled(new AXILiteWriteData(64))
    val writeResp = Flipped(Decoupled(UInt(2.W)))
}

class AXIWriteBus(mSlaves:Int)extends Module{
  val io = IO(new Bundle{
    val master = new writeMaster
    val slave = Vec(mSlaves,new writeSlave)
    //val decerror =Output(UInt(2.W))
  })
  val write_port=WireDefault(0.U(1.W))
  val write_port_reg=RegInit(0.U(1.W))
  val write_addr_reg=RegInit(0.U((32).W))
  val write_addr_reg_valid=RegInit(false.B)
  val write_data_reg=RegInit(0.U((64).W))
  val write_data_reg_valid=RegInit(false.B)
  val write_data_reg_strb=RegInit(0.U((64/8).W))
  val slave_write_startAddr = Wire(Vec(mSlaves,UInt(32.W)))
  val slave_write_endAddr = Wire(Vec(mSlaves,UInt(32.W)))
  slave_write_startAddr(0) := SRAM_START_ADDR
  slave_write_endAddr(0) := SRAM_END_ADDR
  slave_write_startAddr(1) := ACCELERATOR_START_ADDR
  slave_write_endAddr(1) := ACCELERATOR_END_ADDR


  for(i <-0 until mSlaves){
    io.slave(i).writeData.valid:=false.B
    io.slave(i).writeData.bits.data:=0.U
    io.slave(i).writeData.bits.strb:=0.U
    io.slave(i).writeAddr.valid:=false.B
    io.slave(i).writeAddr.bits.addr:=0.U
    io.slave(i).writeResp.ready:=false.B

    when(slave_write_startAddr(i)<=io.master.writeAddr.bits.addr&&io.master.writeAddr.bits.addr<slave_write_endAddr(i)){
      
     
      write_port:=i.U    //找出slave的port
    }
  
  }
  io.master.writeData.ready:=false.B
  io.master.writeAddr.ready:=false.B
  io.master.writeResp.valid:=false.B
  io.master.writeResp.bits:=0.U


  when(io.master.writeAddr.valid&&write_addr_reg_valid===false.B){//
    write_port_reg:=write_port
    write_addr_reg:=io.master.writeAddr.bits.addr
    write_addr_reg_valid:=true.B
  }.otherwise{
    write_addr_reg:=write_addr_reg
    write_addr_reg_valid:=write_addr_reg_valid
  }

  when(write_addr_reg_valid){
    io.master.writeAddr.ready:=false.B
  }.otherwise{
    io.master.writeAddr.ready:=true.B
  }

  when(io.master.writeData.valid&&write_data_reg_valid===false.B){
    write_data_reg_strb:=io.master.writeData.bits.strb
    write_data_reg:=io.master.writeData.bits.data
    write_data_reg_valid:=true.B
  }.otherwise{
    write_data_reg:=write_data_reg
    write_data_reg_valid:=write_data_reg_valid
    write_data_reg_strb:=write_data_reg_strb
  }

  when(write_data_reg_valid){
    io.master.writeData.ready:=false.B
  }.otherwise{
    io.master.writeData.ready:=true.B
  }

  when(io.slave(write_port_reg).writeResp.valid&&write_data_reg_valid===true.B){
    io.master.writeResp.bits:=io.slave(write_port_reg).writeResp.bits
    io.master.writeResp.valid:=true.B
    when(io.master.writeResp.ready){
      io.slave(write_port_reg).writeResp.ready:=true.B
      write_data_reg_valid:=false.B
      write_addr_reg_valid:=false.B
    }
  }.otherwise{
    io.master.writeResp.bits:=0.U
    io.slave(write_port_reg).writeResp.ready:=false.B
    io.master.writeResp.valid:=false.B
  }
  

  io.slave(write_port_reg).writeAddr.bits.addr:=write_addr_reg
  io.slave(write_port_reg).writeAddr.valid:=write_addr_reg_valid
  io.slave(write_port_reg).writeData.bits.data:=write_data_reg
  io.slave(write_port_reg).writeData.bits.strb:=write_data_reg_strb
  io.slave(write_port_reg).writeData.valid:=write_data_reg_valid

}

