package aias_lab10.MemIF

import chisel3._
import chisel3.util._

/* Direct Memory InterFace */
class Cache_IF(memAddrWidth: Int, memDataWidth: Int) extends Module {
  val io = IO(new Bundle {
    val memIF = new MemIF_MEM(memAddrWidth)

    // TBD

  })

  // TBD
  
} 
    
