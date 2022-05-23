package aias_lab9.AXILite

import chisel3._
import chisel3.util._

class writeOut extends Bundle{
    val writeResp = Flipped(Decoupled(UInt(2.W)))                            // response from slave to check write status
    val writeAddr = Decoupled(new AXILiteAddress(32))                   // output address to slave for writing data
    val writeData = Decoupled(new AXILiteWriteData(64))                 // output data to write into slave
}
class writeIns extends Bundle{
    val writeAddr = Flipped(Decoupled(new AXILiteAddress(32)))     // input address from writebus
    val writeData = Flipped(Decoupled(new AXILiteWriteData(64)))   // input write data from  writebus
    val writeResp = Decoupled(UInt(2.W))                              // output write response(response from slave) to writebus
}

class AXISlaveWriteMux(nMasters:Int)extends Module{
    val io = IO(new Bundle{
        val out = new writeOut
        val ins = Vec(nMasters,new writeIns)
    }) 
    val arbiter = Module(new RRArbiter(new AXILiteAddress(32),nMasters))
    
    for(i <- 0 until nMasters){
        arbiter.io.in(i) <> io.ins(i).writeAddr
    }
    //arbiter.io.in <> io.ins
    io.out.writeAddr <> arbiter.io.out

    for(i <- 0 until nMasters){
        io.ins(i).writeData.ready := false.B
        io.ins(i).writeResp.valid := false.B
        io.ins(i).writeResp.bits := 0.U

    }
    io.out.writeData <> io.ins(arbiter.io.chosen.asUInt).writeData
    io.ins(arbiter.io.chosen.asUInt).writeResp <> io.out.writeResp
 
}

