package aias_lab9.AXILite

import chisel3._
import chisel3.util._
import aias_lab9.AXILite.AXILITE_PARAMS._


class AXILiteXBar(nMasters: Int, mSlaves: Int) extends Module{
    val io = IO(new Bundle{
        val masters = Flipped(Vec(nMasters, new AXILiteMasterIF(ADDR_WIDTH, DATA_WIDTH)))
        val slaves = Flipped(Vec(mSlaves, new AXILiteSlaveIF(ADDR_WIDTH, DATA_WIDTH)))
    })

    //read channels
    val readBuses = List.fill(nMasters){ Module(new AXIReadBus(mSlaves))}
    val readMuxes = List.fill(mSlaves){ Module(new AXISlaveReadMux(nMasters))}
    //write channels
    val writeBuses = List.fill(nMasters){ Module(new AXIWriteBus(mSlaves))}
    val writeMuxes = List.fill(mSlaves){ Module(new AXISlaveWriteMux(nMasters))}

    // val Buses = Cat(writeBuses.flatten,readBuses.flatten)
    // val Muxes = Cat(writeMuxes.flatten,readMuxes.flatten)
    
    for(i<-0 until nMasters){
        readBuses(i).io.master.readAddr <> io.masters(i).readAddr    
        io.masters(i).readData <> readBuses(i).io.master.readData
        writeBuses(i).io.master.writeAddr <>  io.masters(i).writeAddr   
        writeBuses(i).io.master.writeData <> io.masters(i).writeData    
        io.masters(i).writeResp <> writeBuses(i).io.master.writeResp
    }
    for(i<-0 until mSlaves){
        io.slaves(i).readAddr <> readMuxes(i).io.out.readAddr
        readMuxes(i).io.out.readData <> io.slaves(i).readData  
        io.slaves(i).writeAddr <> writeMuxes(i).io.out.writeAddr    
        io.slaves(i).writeData <> writeMuxes(i).io.out.writeData    
        writeMuxes(i).io.out.writeResp <> io.slaves(i).writeResp    
    }
    // (Buses.map(b=> b.io.master) zip io.masters) foreach { case(b, m) => b<>m }
    // (Muxes.map(m=> m.io.out) zip io.slaves) foreach { case (x, s) => x<>s }
    
    for (m<- 0 until nMasters; s<- 0 until mSlaves) yield {
        readBuses(m).io.slave(s) <> readMuxes(s).io.ins(m)
    }
    
    for (m<- 0 until nMasters; s<- 0 until mSlaves) yield {
        writeBuses(m).io.slave(s) <> writeMuxes(s).io.ins(m)
    }
    
}

