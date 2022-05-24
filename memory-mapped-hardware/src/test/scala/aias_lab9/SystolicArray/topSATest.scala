package aias_lab9.SystolicArray

import scala.io.Source
import chisel3.iotesters.{PeekPokeTester, Driver}
import scala.language.implicitConversions

class topSATest(dut: topSA) extends PeekPokeTester(dut) {
    poke(dut.io.slave.writeAddr.bits.addr,0x0)
    poke(dut.io.slave.writeAddr.valid,true)
    poke(dut.io.slave.writeData.bits.data,0x1)
    poke(dut.io.slave.writeData.valid,true)
    poke(dut.io.slave.writeResp.ready,true)
    step(2)
    poke(dut.io.slave.writeAddr.bits.addr,0x0)
    poke(dut.io.slave.writeAddr.valid,false)
    poke(dut.io.slave.writeData.bits.data,0x0)
    poke(dut.io.slave.writeData.valid,false)
    step(1)
    poke(dut.io.slave.writeResp.ready,false)

    step(22)

}

object topSATest extends App {
  Driver.execute(
    Array("-tbn", "verilator"),
    () => new topSA
  ) { c: topSA =>
    new topSATest(c)
  }
}