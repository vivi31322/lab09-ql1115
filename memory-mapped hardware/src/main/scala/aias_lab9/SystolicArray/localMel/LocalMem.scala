package aias_lab9.SystolicArray.localMem

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile

class LocalMem(val matrix:Int = 3,
               val rows:Int = 4,
               val cols:Int = 4,
               val bits:Int = 8) extends Module{
    val io = IO(new Bundle{
        val raddr = Input(Vec(3,UInt(32.W)))
        val rdata = Output(Vec(3,UInt(32.W)))

        val waddr = Input(Vec(cols,Valid(UInt(32.W))))
        val wdata = Input(Vec(cols,Valid(UInt(bits.W))))

        val finish = Input(Bool())
    })

    val localMem = Mem(matrix*(rows*cols),UInt(8.W))
    loadMemoryFromFile(localMem,"src/main/resource/SystolicArray/LocalMem.hex")

    List.range(0,matrix).map{x=>
        io.rdata(x) := Cat(localMem(io.raddr(x)),
                           localMem(io.raddr(x)+1.U),
                           localMem(io.raddr(x)+2.U),
                           localMem(io.raddr(x)+3.U))
    }

    (io.waddr zip io.wdata).map{case(addr,data)=>
        when(addr.valid & data.valid){
            localMem(addr.bits) := data.bits
        }
    }

    when(io.finish){
        printf("\n\t\tLocal Memory Value: (Unit:Word) \n")

        for(i <- 0 until 3*rows){
        var data = Cat(localMem(4*i+3),localMem(4*i+2),localMem(4*i+1),localMem(4*i))
        var index = String.format("%" + 2 + "s", i.toString).replace(' ', '0')
        printf(p"\t\tdata[${index}] = 0x${Hexadecimal(data)}\n")
        }
        printf("\n")
    }

}
