package aias_lab9.Single_Cycle.Memory

import chisel3._
import chisel3.util._

import chisel3.iotesters.{Driver,PeekPokeTester}

import aias_lab9.AXILite.AXILiteSlaveIF

class DataMemTest(dut:DataMem) extends PeekPokeTester(dut){
    //WriteData
    poke(dut.io.slave.writeData.valid,true.B)
    poke(dut.io.slave.writeData.bits.data,8787)

    //WriteAddr
    poke(dut.io.slave.writeAddr.valid,true.B)
    poke(dut.io.slave.writeAddr.bits.addr,8)

    //WriteResp
    poke(dut.io.slave.writeResp.ready,true.B)

    step(5)

    // //ReadAddr
    // poke(dut.io.slave.readAddr.valid,true.B)
    // poke(dut.io.slave.readAddr.bits.addr,0)

    // //ReadData
    // poke(dut.io.slave.readData.ready,true.B)
}

object DataMemTest extends App{
    Driver.execute(args,()=>new DataMem(15,32,32)){
        c:DataMem => new DataMemTest(c)
    }
}
