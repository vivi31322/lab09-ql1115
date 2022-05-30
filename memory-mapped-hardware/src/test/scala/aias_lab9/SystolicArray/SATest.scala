package aias_lab9.SystolicArray

import scala.io.Source
import chisel3.iotesters.{PeekPokeTester, Driver}
import scala.language.implicitConversions

class SATest(dut: SA) extends PeekPokeTester(dut) {
    step(5)
    while(peek(dut.io.mmio.STATUS_IN) != 1){
      poke(dut.io.mmio.ENABLE_OUT,true)
      step(1)
    }
    
    

}

object SATest extends App {
  Driver.execute(
    Array("-tbn", "verilator"),
    () => new SA(4,4,32,64)
  ) { c: SA =>
    new SATest(c)
  }
}