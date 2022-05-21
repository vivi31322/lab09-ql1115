package aias_lab9.SystolicArray.localMem

import chisel3.iotesters.{Driver,PeekPokeTester}

class memTest(dut:LocalMem)extends PeekPokeTester(dut){
    step(1)
}

object memTest extends App{
    Driver.execute(args,()=>new LocalMem){
        c => new memTest(c)
    }
}