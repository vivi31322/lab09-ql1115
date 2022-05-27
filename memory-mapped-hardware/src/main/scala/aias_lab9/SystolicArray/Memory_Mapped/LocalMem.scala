package aias_lab9.SystolicArray

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile

class LocalMem(val matrix:Int = 3,
               val rows:Int = 4,
               val cols:Int = 4,
               val bits:Int = 8) extends Module{
    val io = IO(new Bundle{
        // from SA(slave) or CPU(master)
        val raddr = Input(UInt(32.W))
        val rdata = Output(UInt(32.W))
        
        val wen   = Input(Bool())
        val waddr = Input(UInt(32.W))
        val wdata = Input(UInt(32.W))

        // for printing need
        val finish = Input(Bool())
    })

    val localMem = SyncReadMem(matrix*(rows*cols),UInt(8.W))
    loadMemoryFromFile(localMem,"src/main/resource/SystolicArray/LocalMem.hex")


    io.rdata := Cat(localMem(io.raddr),
                localMem(io.raddr+1.U),
                localMem(io.raddr+2.U),
                localMem(io.raddr+3.U))

    when(io.wen){
        localMem(io.waddr + (0.U)) := io.wdata(7,0)
        localMem(io.waddr + (1.U)) := io.wdata(15,8)
        localMem(io.waddr + (2.U)) := io.wdata(23,16)
        localMem(io.waddr + (3.U)) := io.wdata(31,24)
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
