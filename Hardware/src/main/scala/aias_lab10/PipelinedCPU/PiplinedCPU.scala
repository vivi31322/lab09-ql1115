package aias_lab10.PiplinedCPU

import chisel3._
import chisel3.util._

import aias_lab10.Memory._
import aias_lab10.MemIF._
import aias_lab10.PiplinedCPU.StageRegister._
import aias_lab10.PiplinedCPU.Controller._
import aias_lab10.PiplinedCPU.Datapath._
import aias_lab10.PiplinedCPU.pc_sel_map._
import aias_lab10.PiplinedCPU.wb_sel_map._
import aias_lab10.PiplinedCPU.forwarding_sel_map._
import aias_lab10.PiplinedCPU.opcode_map._

class PiplinedCPU(memAddrWidth: Int, memDataWidth: Int) extends Module {
    val io = IO(new Bundle{
        //InstMem
        val InstMem = new MemIF_CPU(memAddrWidth) 
        
        //DataMem
        val DataMem = new MemIF_CPU(memAddrWidth) 

        //System
        val regs = Output(Vec(32,UInt(32.W)))
        val Hcf = Output(Bool())

        // Test
        val E_Branch_taken = Output(Bool())
        val Flush = Output(Bool())
        val Stall_MA = Output(Bool())
        val Stall_DH = Output(Bool())
        val ID_PC = Output(UInt(memAddrWidth.W))
        val EXE_PC = Output(UInt(memAddrWidth.W))
        val MEM_PC = Output(UInt(memAddrWidth.W))
        val WB_PC = Output(UInt(memAddrWidth.W))
        val EXE_src1 = Output(UInt(32.W))
        val EXE_src2 = Output(UInt(32.W))
        val EXE_src1_sel = Output(UInt(2.W))
        val EXE_src2_sel = Output(UInt(2.W))
        val EXE_alu_out = Output(UInt(32.W))
        val WB_rd = Output(UInt(5.W))
        val WB_wdata = Output(UInt(32.W))
        val EXE_Jump = Output(Bool())
        val EXE_Branch = Output(Bool())
    })
    
    //Module
    val ct = Module(new Controller(memAddrWidth))
    val pc = Module(new PC(memAddrWidth))
    val ig = Module(new ImmGen())
    val rf = Module(new RegFile(2))
    val alu = Module(new ALU())
    val bc = Module(new BranchComp())

    /*****  Pipeline Stages Registers Module for holding data *****/
    // Instuction Fetch (IF) don't need stage Registers
    val stage_ID = Module(new Reg_ID(memAddrWidth))
    val stage_EXE = Module(new Reg_EXE(memAddrWidth))
    val stage_MEM = Module(new Reg_MEM(memAddrWidth))
    val stage_WB = Module(new Reg_WB(memAddrWidth))
    val stage_WB_data = Module(new Reg_WB_data())

    //PC
    val EXE_target_pc = alu.io.out(memAddrWidth-1,0)
    pc.io.Stall := (ct.io.Stall_DH || ct.io.Stall_MA)
    pc.io.PCSel := ct.io.PCSel
    pc.io.EXE_Target_pc := EXE_target_pc
    pc.io.Predict_Target_pc := 0.U // Branch Prediction
    pc.io.EXE_pc := stage_EXE.io.pc
    pc.io.Hcf := ct.io.Hcf
    
    //Insruction Memory
    io.InstMem.Mem_R := ct.io.IM_Mem_R
    io.InstMem.Mem_W :=  ct.io.IM_Mem_W
    io.InstMem.Length :=  ct.io.IM_Length
    io.InstMem.raddr := pc.io.pc
    io.InstMem.waddr := 0.U // not used
    io.InstMem.wdata := 0.U // not used

    // ID stage reg
    stage_ID.io.Flush := ct.io.Flush
    stage_ID.io.Stall := (ct.io.Stall_DH || ct.io.Stall_MA || ct.io.Hcf)
    stage_ID.io.inst_in := io.InstMem.rdata
    stage_ID.io.pc_in := pc.io.pc
    stage_ID.io.BP_taken_in := false.B //-- Branch Prediction --

    // ID Decode
    val rs1_ID = stage_ID.io.inst(19,15)
    val rs2_ID = stage_ID.io.inst(24,20)

    // WB Wire
    val wb_data_wire = Wire(UInt(32.W))
    wb_data_wire := MuxLookup(ct.io.W_WBSel, 0.U, Seq(
            PC_PLUS_4 -> stage_WB.io.pc_plus4,  //from PC (+4)
            ALUOUT -> stage_WB.io.alu_out,   //from ALU
            LD_DATA ->  stage_WB.io.ld_data, //from DataMemory
        ))

    //RegFile
    rf.io.raddr(0) := stage_ID.io.inst(19,15)
    rf.io.raddr(1) := stage_ID.io.inst(24,20)
    rf.io.waddr := stage_WB.io.inst(11,7)
    rf.io.wen := ct.io.W_RegWEn
    rf.io.wdata := wb_data_wire

    //ImmGen
    ig.io.ImmSel := ct.io.D_ImmSel
    ig.io.inst_31_7 := stage_ID.io.inst(31,7)
    
    // EXE stage reg
    stage_EXE.io.Flush := (ct.io.Flush || ct.io.Stall_DH) // Make a Bubble (nop)
    stage_EXE.io.Stall := (ct.io.Stall_MA || ct.io.Hcf)
    stage_EXE.io.pc_in := stage_ID.io.pc
    stage_EXE.io.inst_in := stage_ID.io.inst
    stage_EXE.io.imm_in := ig.io.imm
    stage_EXE.io.rs1_rdata_in := rf.io.rdata(0)
    stage_EXE.io.rs2_rdata_in := rf.io.rdata(1)
    stage_EXE.io.BP_taken_in := stage_ID.io.BP_taken //-- Branch Prediction --


    // Reg Data Forwarding
    val E_rs1_rdata = Wire(UInt(32.W))
    val E_rs2_rdata = Wire(UInt(32.W))

    E_rs1_rdata := MuxLookup(ct.io.E_rs1_data_sel, stage_EXE.io.rs1_rdata, Seq(
            EXE_STAGE -> stage_EXE.io.rs1_rdata,  
            MEM_STAGE -> stage_MEM.io.alu_out,   
            WB_STAGE ->  wb_data_wire, 
            WBD_STAGE ->  stage_WB_data.io.wb_data,
        ))
    
    E_rs2_rdata := MuxLookup(ct.io.E_rs2_data_sel, stage_EXE.io.rs2_rdata, Seq(
            EXE_STAGE -> stage_EXE.io.rs2_rdata, 
            MEM_STAGE -> stage_MEM.io.alu_out,   
            WB_STAGE ->  wb_data_wire, 
            WBD_STAGE ->  stage_WB_data.io.wb_data,
        ))

    //Branch Comparator
    bc.io.BrUn := ct.io.E_BrUn
    bc.io.src1 := E_rs1_rdata
    bc.io.src2 := E_rs2_rdata

    //ALU
    alu.io.src1 := MuxLookup(ct.io.E_ASel,E_rs1_rdata,Seq(
        0.U -> E_rs1_rdata,
        1.U -> stage_EXE.io.pc,
        2.U -> 0.U(32.W)
    ))
    alu.io.src2 := MuxLookup(ct.io.E_BSel,E_rs2_rdata,Seq(
        0.U -> E_rs2_rdata,
        1.U -> stage_EXE.io.imm,
    ))
    alu.io.ALUSel := ct.io.E_ALUSel
    
    // MEM stage reg
    stage_MEM.io.Stall := (ct.io.Stall_MA || ct.io.Hcf)
    stage_MEM.io.pc_in := stage_EXE.io.pc
    stage_MEM.io.inst_in := stage_EXE.io.inst
    stage_MEM.io.rs2_rdata_in := E_rs2_rdata
    stage_MEM.io.alu_out_in := alu.io.out

    //Data Memory
    io.DataMem.Mem_R := ct.io.DM_Mem_R
    io.DataMem.Mem_W :=  ct.io.DM_Mem_W
    io.DataMem.Length :=  ct.io.DM_Length
    io.DataMem.raddr := stage_MEM.io.alu_out(memAddrWidth-1,0)
    io.DataMem.waddr := stage_MEM.io.alu_out(memAddrWidth-1,0)
    io.DataMem.wdata := stage_MEM.io.rs2_rdata

    // WB stage reg
    stage_WB.io.Stall := (ct.io.Stall_MA || ct.io.Hcf)
    stage_WB.io.pc_plus4_in := (stage_MEM.io.pc + 4.U)
    stage_WB.io.inst_in := stage_MEM.io.inst
    stage_WB.io.alu_out_in := stage_MEM.io.alu_out
    stage_WB.io.ld_data_in := io.DataMem.rdata

    // WB data stage reg
    stage_WB_data.io.Stall := (ct.io.Stall_MA || ct.io.Hcf)
    stage_WB_data.io.WB_Hazard_in := ct.io.W_wb_data_hazard
    stage_WB_data.io.wb_data_in := wb_data_wire

    //Controller
    ct.io.IF_Inst := io.InstMem.rdata
    ct.io.ID_Inst := stage_ID.io.inst
    ct.io.EXE_Inst := stage_EXE.io.inst
    ct.io.MEM_Inst := stage_MEM.io.inst
    ct.io.WB_Inst := stage_WB.io.inst

    ct.io.E_BrEq := bc.io.BrEq
    ct.io.E_BrLT := bc.io.BrLT

    ct.io.ID_pc := stage_ID.io.pc
    ct.io.EXE_BP_taken := stage_EXE.io.BP_taken
    
    ct.io.EXE_target_pc := EXE_target_pc

    ct.io.WBD_wb_data_hazard := stage_WB_data.io.WB_Hazard

    ct.io.IM_Valid := io.InstMem.Valid
    ct.io.DM_Valid := io.DataMem.Valid

    
    /*** Branch Prediction Part ***/
    ct.io.BP_taken := false.B
    // (E_En)           ct.io.E_En
    // (E_Branch_taken) ct.io.E_Branch_taken
    // (EXE_target_pc)  EXE_target_pc 
    // (EXE_pc)         stage_EXE.io.pc
    // (IF_pc)          pc.io.pc


    //System
    io.regs := rf.io.regs
    io.Hcf := ct.io.Hcf

    // Test 
    io.E_Branch_taken := ct.io.E_Branch_taken
    io.Flush := ct.io.Flush
    io.Stall_MA := ct.io.Stall_MA
    io.Stall_DH := ct.io.Stall_DH
    io.ID_PC := stage_ID.io.pc
    io.EXE_PC := stage_EXE.io.pc
    io.MEM_PC := stage_MEM.io.pc
    io.WB_PC := Mux(stage_WB.io.pc_plus4 > 0.U ,stage_WB.io.pc_plus4 - 4.U,stage_WB.io.pc_plus4)
    io.EXE_alu_out := alu.io.out
    io.EXE_src1 := E_rs1_rdata
    io.EXE_src2 := E_rs2_rdata
    io.EXE_src1_sel := ct.io.E_rs1_data_sel
    io.EXE_src2_sel := ct.io.E_rs2_data_sel
    io.WB_wdata := wb_data_wire
    io.WB_rd := stage_WB.io.inst(11,7)
    io.EXE_Jump := (stage_EXE.io.inst(6, 0)===JAL) || (stage_EXE.io.inst(6, 0)===JALR)
    io.EXE_Branch := (stage_EXE.io.inst(6, 0)===BRANCH)
}