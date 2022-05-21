package aias_lab9.Single_Cycle

import chisel3._
import chisel3.util._

import aias_lab9.Memory._
import aias_lab9.Single_Cycle.Controller._
import aias_lab9.Single_Cycle.Datapath._
import aias_lab9.AXILiteDefs.AXILiteMasterIF

class Single_Cycle(addrWidth: Int, dataWidth: Int) extends Module {
    val io = IO(new Bundle{
        //AXI master : to DataMem
        val master = new AXILiteMasterIF(addrWidth, dataWidth)
        //InstMem
        val pc = Output(UInt(15.W))
        val rinst = Input(UInt(32.W))

        //System
        val regs = Output(Vec(32,UInt(32.W)))
        val Hcf = Output(Bool())
    })
    
    //Module
    val ct = Module(new Controller())
    val pc = Module(new PC())
    val ig = Module(new ImmGen())
    val rf = Module(new RegFile(2))
    val alu = Module(new ALU())
    val bc = Module(new BranchComp())

    //wire
    val rd        = Wire(UInt(5.W))
    val rs1       = Wire(UInt(5.W))
    val rs2       = Wire(UInt(5.W))
    val funct3    = Wire(UInt(3.W))
    val inst_31_7 = Wire(UInt(25.W))
    val pc_go = WireDefault(false.B)

    rd  := io.rinst(11,7)
    rs1 := io.rinst(19,15)
    rs2 := io.rinst(24,20)
    funct3 := io.rinst(14,12)
    inst_31_7 := io.rinst(31,7)
    
    //PC
    pc.io.PCSel := ct.io.PCSel
    pc.io.alu_out := alu.io.out
    pc.io.Hcf := ct.io.Hcf
    pc.io.pc_stall := ct.io.pc_stall
    pc.io.pc_go := pc_go
    
    //Insruction Memory
    io.pc := pc.io.pc

    //ImmGen
    ig.io.ImmSel := ct.io.ImmSel
    ig.io.inst_31_7 := inst_31_7
    
    //RegFile
    rf.io.raddr(0) := rs1
    rf.io.raddr(1) := rs2
    rf.io.waddr := rd
    rf.io.wen := ct.io.RegWEn
    
    when(ct.io.WBSel === 0.U && (io.master.readData.ready && io.master.readData.valid)){
        //HandShake
        rf.io.wdata := io.master.readData.bits.data
    } //from DataMemory
    .elsewhen(ct.io.WBSel === 1.U){rf.io.wdata := alu.io.out} //from ALU
    .elsewhen(ct.io.WBSel === 2.U){rf.io.wdata := pc.io.pc + 4.U} //from PC (+4)
    .otherwise{rf.io.wdata := 0.U} // Default

    //ALU
    val rdata_or_zero = Wire(UInt(32.W))
    rdata_or_zero := Mux(ct.io.Lui,0.U(32.W),rf.io.rdata(0))
    alu.io.src1 := Mux(ct.io.ASel,pc.io.pc,rdata_or_zero)
    alu.io.src2 := Mux(ct.io.BSel,ig.io.imm,rf.io.rdata(1))
    alu.io.ALUSel := ct.io.ALUSel
    
    //Data Memory (AXI version)
    //All signal
    io.master.writeData.valid := false.B
    io.master.writeData.bits.data := 0.U
    io.master.writeAddr.valid := false.B
    io.master.writeAddr.bits.addr := 0.U
    io.master.writeResp.ready := false.B
    io.master.readAddr.valid := false.B
    io.master.readAddr.bits.addr := 0.U
    io.master.readData.ready := false.B

    val sWait = 0.U(1.W)
    val sWrite = 1.U(1.W)
    val sRead = 1.U(1.W)

    val w_state = RegInit(sWait)

    when(w_state === sWait){
        when(io.master.writeAddr.ready && io.master.writeData.ready){w_state := sWrite}
        io.master.writeResp.ready := false.B

        io.master.writeAddr.valid := ct.io.MemRW //Ideally, When the instruction is store, the data and addr are already prepared!!
        io.master.writeAddr.bits.addr := Mux(ct.io.MemRW,alu.io.out(15,0),0.U)

        io.master.writeData.valid := ct.io.MemRW
        io.master.writeData.bits.data := rf.io.rdata(1)
    }
    when(w_state === sWrite){
        when(io.master.writeResp.valid && io.master.writeResp.ready){w_state := sWait}
        io.master.writeResp.ready := true.B

        io.master.writeAddr.valid := false.B
        io.master.writeAddr.bits.addr := 0.U

        io.master.writeData.valid := false.B
        io.master.writeData.bits.data := 0.U
    }

    val r_state = RegInit(sWait)

    when(r_state === sWait){
        when(io.master.readAddr.ready){r_state := sRead}

        io.master.readAddr.valid := !ct.io.MemRW && ct.io.WBSel === 0.U
        io.master.readAddr.bits.addr := Mux((!ct.io.MemRW && ct.io.WBSel === 0.U),alu.io.out(15,0),0.U)

        io.master.readData.ready := false.B
    }
    when(r_state === sRead){
        when(io.master.readData.valid){r_state := sWait}

        io.master.readAddr.valid := false.B
        io.master.readAddr.bits.addr := 0.U
        
        io.master.readData.ready := true.B
    }

    pc_go := (io.master.readData.ready && io.master.readData.valid) || io.master.writeResp.valid



    //Branch Comparator
    bc.io.BrUn := ct.io.BrUn
    bc.io.src1 := rf.io.rdata(0)
    bc.io.src2 := rf.io.rdata(1)

    //Controller
    ct.io.Inst := io.rinst
    ct.io.BrEq := bc.io.BrEq
    ct.io.BrLT := bc.io.BrLT

    //System
    io.regs := rf.io.regs
    io.Hcf := ct.io.Hcf
}