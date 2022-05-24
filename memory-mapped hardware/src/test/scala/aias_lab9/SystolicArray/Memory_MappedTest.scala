package aias_lab9.SystolicArray

import scala.io.Source
import chisel3.iotesters.{PeekPokeTester, Driver}
import scala.language.implicitConversions

class Memory_MappedTest(dut: Memory_Mapped) extends PeekPokeTester(dut) {
    poke(dut.io.slave.readAddr.bits.addr,0x100000)
    poke(dut.io.slave.readAddr.valid,true)
    poke(dut.io.slave.readData.ready,true)

    step(10)

}

object Memory_MappedTest extends App {
  Driver.execute(
    Array("-tbn", "verilator"),
    () => new Memory_Mapped
  ) { c: Memory_Mapped =>
    new Memory_MappedTest(c)
  }
}