package aias_lab9.SystolicArray

import scala.io.Source
import chisel3.iotesters.{PeekPokeTester, Driver}
import scala.language.implicitConversions

class LocalMemTest(dut: LocalMem) extends PeekPokeTester(dut) {
    step(10)

}

object LocalMemTest extends App {
  Driver.execute(
    Array("-tbn", "verilator"),
    () => new LocalMem
  ) { c: LocalMem =>
    new LocalMemTest(c)
  }
}