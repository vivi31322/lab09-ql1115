package aias_lab9.AXILite

import chisel3._
import chisel3.iotesters.{Driver,PeekPokeTester}

class InterfaceTest(dut:Interface) extends PeekPokeTester(dut){
    poke(dut.io.slave.writeData.valid,false.B)
    step(30)
}

object InterfaceTest extends App{
    Driver.execute(args,()=>new Interface){
        c:Interface => new InterfaceTest(c)
    }
}
