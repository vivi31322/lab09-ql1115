package aias_lab9.SystolicArray

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile

class LocalMem(mem_size:Int,
               addr_width:Int,
               data_width:Int) extends Module{
    val io = IO(new Bundle{
        // from SA(slave) or CPU(master)
        val raddr = Input(UInt(addr_width.W))
        val rdata = Output(UInt(data_width.W))
        
        val wen   = Input(Bool())
        val waddr = Input(UInt(addr_width.W))
        val wdata = Input(UInt(data_width.W))
<<<<<<< Updated upstream
<<<<<<< Updated upstream
=======
        val wstrb = Input(UInt((data_width>>3).W))
>>>>>>> Stashed changes
=======
        val wstrb = Input(UInt((data_width>>3).W))
>>>>>>> Stashed changes

        // for printing need
        val finish = Input(Bool())
    })

    val byte = 8
<<<<<<< Updated upstream
<<<<<<< Updated upstream

    val localMem = SyncReadMem(mem_size,UInt(byte.W))
    loadMemoryFromFile(localMem,"src/main/resource/SystolicArray/LocalMem.hex")
=======
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes

    val localMem = SyncReadMem(mem_size,UInt(byte.W))
    // loadMemoryFromFile(localMem,"src/main/resource/SystolicArray/LocalMem.hex")

<<<<<<< Updated upstream
<<<<<<< Updated upstream
=======
    val raddr_aligned = WireDefault(io.raddr & ~(7.U(data_width.W)))
    val waddr_aligned = WireDefault(io.waddr & ~(7.U(data_width.W)))
>>>>>>> Stashed changes
=======
    val raddr_aligned = WireDefault(io.raddr & ~(7.U(data_width.W)))
    val waddr_aligned = WireDefault(io.waddr & ~(7.U(data_width.W)))
>>>>>>> Stashed changes
    
    
    val rdata = WireDefault(
        List.range(0,data_width>>3).map{x=>
            //data_width>>3 === 64/8=8
<<<<<<< Updated upstream
<<<<<<< Updated upstream
            localMem(io.raddr+x.U) << (((data_width>>3)-1-x)*byte)
=======
            localMem(raddr_aligned+x.U) << (((data_width>>3)-1-x)*byte)
>>>>>>> Stashed changes
=======
            localMem(raddr_aligned+x.U) << (((data_width>>3)-1-x)*byte)
>>>>>>> Stashed changes
        }.reduce(_+_)
    )

    io.rdata := rdata

    when(io.wen){
        List.range(0,data_width>>3).map{x=>
<<<<<<< Updated upstream
<<<<<<< Updated upstream
            localMem(io.waddr + (x.U)) := io.wdata((x+1)*byte-1,x*byte)
=======
            when(io.wstrb(x)===1.U){
                localMem(waddr_aligned + (x.U)) := io.wdata((x+1)*byte-1,x*byte)
            }
>>>>>>> Stashed changes
=======
            when(io.wstrb(x)===1.U){
                localMem(waddr_aligned + (x.U)) := io.wdata((x+1)*byte-1,x*byte)
            }
>>>>>>> Stashed changes
        }
    }
        
    

    





















    
    when(io.finish){
        printf("\n\t\tLocal Memory Value: (Unit:Word) \n")

        for(i <- 0 until 6){
            var data = Cat(localMem(8*i+7),
                           localMem(8*i+6),
                           localMem(8*i+5),
                           localMem(8*i+4),
                           localMem(8*i+3),
                           localMem(8*i+2),
                           localMem(8*i+1),
                           localMem(8*i)
                        )
            var index = String.format("%" + 2 + "s", i.toString).replace(' ', '0')
            printf(p"\t\tdata[${index}] = 0x${Hexadecimal(data)}\n")
        }
        printf("\n")
    }

}
