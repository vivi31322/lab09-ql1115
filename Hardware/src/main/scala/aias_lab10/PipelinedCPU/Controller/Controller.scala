package aias_lab10.PiplinedCPU.Controller

import chisel3._
import chisel3.util._

import aias_lab10.PiplinedCPU.opcode_map._
import aias_lab10.PiplinedCPU.condition._
import aias_lab10.PiplinedCPU.inst_type._
import aias_lab10.PiplinedCPU.alu_op_map._
import aias_lab10.PiplinedCPU.pc_sel_map._
import aias_lab10.PiplinedCPU.wb_sel_map._
import aias_lab10.PiplinedCPU.forwarding_sel_map._

class Controller(memAddrWidth: Int) extends Module {
  val io = IO(new Bundle {
    // Memory control signal interface
    val IM_Mem_R = Output(Bool()) 
    val IM_Mem_W = Output(Bool()) 
    val IM_Length = Output(UInt(3.W))
    val IM_Valid = Input(Bool()) 

    val DM_Mem_R = Output(Bool()) 
    val DM_Mem_W = Output(Bool()) 
    val DM_Length = Output(UInt(3.W))
    val DM_Valid = Input(Bool()) 

    // branch Comp.
    val E_BrEq = Input(Bool())
    val E_BrLT = Input(Bool())

    // Branch Prediction
    val BP_taken = Input(Bool())
    val E_Branch_taken = Output(Bool())
    val E_En = Output(Bool())

    val ID_pc = Input(UInt(memAddrWidth.W))
    val EXE_BP_taken = Input(Bool())
    val EXE_target_pc = Input(UInt(memAddrWidth.W))

    // Flush
    val Flush = Output(Bool()) //TBD

    // Stall
    val Stall_DH = Output(Bool())  // Data Hazard (Stall IF/ID/EXE) //TBD
    val Stall_MA = Output(Bool())  // Memory Access (Stall all)   //TBD

    // inst
    val IF_Inst = Input(UInt(32.W))
    val ID_Inst = Input(UInt(32.W))
    val EXE_Inst = Input(UInt(32.W))
    val MEM_Inst = Input(UInt(32.W))
    val WB_Inst = Input(UInt(32.W))

    // Data Forwarding Mux sel
    val E_rs1_data_sel = Output(UInt(2.W)) //TBD
    val E_rs2_data_sel = Output(UInt(2.W)) //TBD

    // WB Data Hazard
    val W_wb_data_hazard = Output(UInt(2.W)) //TBD
    val WBD_wb_data_hazard = Input(UInt(2.W))  //TBD

    // sel
    val PCSel = Output(UInt(2.W))
    val D_ImmSel = Output(UInt(3.W))
    val W_RegWEn = Output(Bool())
    val E_BrUn = Output(Bool())
    val E_ASel = Output(UInt(2.W))
    val E_BSel = Output(UInt(1.W))
    val E_ALUSel = Output(UInt(15.W))
    val W_WBSel = Output(UInt(2.W))

    val Hcf = Output(Bool())
  })
  // Inst Decode for each stage 
  val IF_opcode = io.IF_Inst(6, 0)

  val ID_opcode = io.ID_Inst(6, 0)
  val ID_rs1 = io.ID_Inst(19,15)
  val ID_rs2 = io.ID_Inst(24,20)

  val EXE_opcode = io.EXE_Inst(6, 0)
  val EXE_funct3 = io.EXE_Inst(14, 12)
  val EXE_rs1 = io.EXE_Inst(19,15)
  val EXE_rs2 = io.EXE_Inst(24,20)
  val EXE_funct7 = io.EXE_Inst(31, 25)

  val MEM_opcode = io.MEM_Inst(6, 0)
  val MEM_rd = io.MEM_Inst(11, 7)
  val MEM_funct3 = io.MEM_Inst(14, 12)

  val WB_opcode = io.WB_Inst(6, 0)
  val WB_rd = io.WB_Inst(11, 7)

  // Control signal - Branch Prediction
  val E_En = Wire(Bool())
  E_En := (EXE_opcode===BRANCH || EXE_opcode===JAL || EXE_opcode===JALR ) // Branch Tatget Enable
  val E_Branch_taken = Wire(Bool())
  E_Branch_taken := MuxLookup(EXE_opcode, false.B, Seq(
          BRANCH -> MuxLookup(EXE_funct3, false.B, Seq(
            "b000".U(3.W) -> io.E_BrEq.asUInt,
            "b001".U(3.W) -> ~io.E_BrEq.asUInt,
            "b100".U(3.W) -> io.E_BrLT.asUInt,
            "b101".U(3.W) -> ~io.E_BrLT.asUInt,
            "b110".U(3.W) -> io.E_BrLT.asUInt,
            "b111".U(3.W) -> ~io.E_BrLT.asUInt
          )),
          JALR -> true.B,
          JAL -> true.B
        ))
        
  io.E_En := E_En
  io.E_Branch_taken := E_Branch_taken

  // pc predict miss signal
  val Predict_Miss = Wire(Bool())
  Predict_Miss := (E_En && ((io.EXE_BP_taken =/= E_Branch_taken) || (E_Branch_taken && io.ID_pc=/=io.EXE_target_pc)))

  // Control signal - PC
  val BP_En = Wire(Bool())
  BP_En := (IF_opcode===BRANCH || IF_opcode===JAL || IF_opcode===JALR ) // Branch Predict Enable

  when(Predict_Miss){
    when(E_Branch_taken){
      io.PCSel := EXE_T_PC
    }.otherwise{
      io.PCSel := EXE_PC_PLUS_4
    }
  }.otherwise{
    when(io.BP_taken && BP_En){
      io.PCSel := IF_P_T_PC
    }.otherwise{
      io.PCSel := IF_PC_PLUS_4
    }
  }

  // Control signal - Branch comparator
  io.E_BrUn := (io.EXE_Inst(13) === 1.U)

  // Control signal - Immediate generator
  io.D_ImmSel := MuxLookup(ID_opcode, 0.U, Seq(
    OP_IMM -> I_type,
    LOAD -> I_type,
    STORE -> S_type,
    BRANCH -> B_type,
    JALR -> I_type,
    JAL -> J_type,
    LUI -> U_type,
    AUIPC -> U_type
  ))

  // Control signal - Scalar ALU
  io.E_ASel := MuxLookup(EXE_opcode, 0.U, Seq(
    BRANCH -> 1.U,
    JAL -> 1.U,
    AUIPC -> 1.U,
    LUI -> 2.U,
  ))
  io.E_BSel := MuxLookup(EXE_opcode, 1.U, Seq(
    OP -> 0.U,
    OP_IMM -> 1.U,
  ))
  io.E_ALUSel := MuxLookup(EXE_opcode, (Cat(0.U(7.W), "b11111".U, 0.U(3.W))), Seq(
    OP -> (Cat(EXE_funct7, "b11111".U, EXE_funct3)),
    OP_IMM -> MuxLookup(EXE_funct3,Cat(0.U(7.W), "b11111".U, EXE_funct3),Seq(
      "b001".U -> Cat(EXE_funct7, "b11111".U, EXE_funct3),
      "b101".U -> Cat(EXE_funct7, "b11111".U, EXE_funct3)
    ))
  ))

  // Memory Access FSM
  val sNormal :: sWait :: sIM_Done :: sDM_Done :: Nil = Enum(4)
  val Mem_state = RegInit(sNormal)

  val IM_to_Read = true.B
  val IM_to_Write = false.B
  val DM_to_Read = (MEM_opcode===LOAD)
  val DM_to_Write = (MEM_opcode===STORE)

  val IM_Done =  ((~IM_to_Read && ~IM_to_Write) || (io.IM_Valid)) // No op, or done within a cycle.
  val DM_Done =  ((~DM_to_Read && ~DM_to_Write) || (io.DM_Valid)) // No op, or done within a cycle.

  // Memory Access State
  switch(Mem_state){
    is(sNormal){
      when(IM_Done && DM_Done){
        Mem_state := sNormal
      }.elsewhen(IM_Done){
        Mem_state := sIM_Done
      }.elsewhen(DM_Done){
        Mem_state := sDM_Done
      }.otherwise{
        Mem_state := sWait
      }
    }
    is(sWait){
      when(IM_Done && DM_Done){
        Mem_state := sNormal
      }.elsewhen(IM_Done){
        Mem_state := sIM_Done
      }.elsewhen(DM_Done){
        Mem_state := sDM_Done
      }.otherwise{
        Mem_state := sWait
      }
    }
    is(sIM_Done){
      when(DM_Done){
        Mem_state := sNormal
      }.otherwise{
        Mem_state := sIM_Done
      }
    }
    is(sDM_Done){
      when(IM_Done){
        Mem_state := sNormal
      }.otherwise{
        Mem_state := sDM_Done
      }
    }
  }
  
  // Stall -- stall for Memory Access (All Stalled, related to FSM)
  when(Mem_state===sNormal && (~IM_Done || ~DM_Done)){
    io.Stall_MA := true.B
  }.elsewhen(Mem_state===sWait && (~IM_Done || ~DM_Done)){
    io.Stall_MA := true.B
  }.elsewhen(Mem_state===sDM_Done && (~IM_Done)){
    io.Stall_MA := true.B
  }.elsewhen(Mem_state===sIM_Done && (~DM_Done)){
    io.Stall_MA := true.B
  }otherwise{
    io.Stall_MA := false.B
  }

  // Control signal - Data Memory
  io.DM_Mem_R := (DM_to_Read && Mem_state=/=sDM_Done)
  io.DM_Mem_W := (DM_to_Write && Mem_state=/=sDM_Done)
  io.DM_Length := MEM_funct3 // length

  // Control signal - Inst Memory
  io.IM_Mem_R := (true.B && Mem_state=/=sIM_Done) // always true
  io.IM_Mem_W := (false.B && Mem_state=/=sIM_Done) // always false
  io.IM_Length := "b010".U // always load a word(inst)

  // Control signal - Scalar Write Back
  val W_reg_en = Wire(Bool())
  W_reg_en := MuxLookup(WB_opcode, false.B, Seq(
    OP -> true.B,
    OP_IMM -> true.B,
    LOAD -> true.B,
    JALR -> true.B,
    JAL -> true.B,
    AUIPC -> true.B,
    LUI -> true.B
  ))

  io.W_RegWEn := false.B
  when(io.Stall_MA){ 
    io.W_RegWEn := false.B //Mem Access
  }.otherwise{
    io.W_RegWEn := W_reg_en
  }
  
  io.W_WBSel := MuxLookup(WB_opcode, ALUOUT, Seq(
    LOAD -> LD_DATA,
    JALR -> PC_PLUS_4,
    JAL -> PC_PLUS_4
  ))

  // Control signal - Others
  io.Hcf := (IF_opcode === HCF)

  /****************** Data Hazard ******************/

  // Use rs in ID stage 
  val is_D_use_rs1 = Wire(Bool()) 
  val is_D_use_rs2 = Wire(Bool())
  is_D_use_rs1 := MuxLookup(ID_opcode,false.B,Seq(
    OP -> true.B,
    OP_IMM -> true.B,
    STORE -> true.B,
    LOAD -> true.B,
    BRANCH -> true.B,
    JALR -> true.B,
  ))
  is_D_use_rs2 := MuxLookup(ID_opcode,false.B,Seq(
    OP -> true.B,
    STORE -> true.B,
    BRANCH -> true.B,
  ))
  // Use rs in EXE stage 
  val is_E_use_rs1 = Wire(Bool()) 
  val is_E_use_rs2 = Wire(Bool())
  is_E_use_rs1 := MuxLookup(EXE_opcode,false.B,Seq(
    OP -> true.B,
    OP_IMM -> true.B,
    STORE -> true.B,
    LOAD -> true.B,
    BRANCH -> true.B,
    JALR -> true.B,
  ))
  is_E_use_rs2 := MuxLookup(EXE_opcode,false.B,Seq(
    OP -> true.B,
    STORE -> true.B,
    BRANCH -> true.B,
  ))
  // Use rd in MEM stage 
  val is_M_use_rd = Wire(Bool())
  is_M_use_rd := MuxLookup(MEM_opcode,false.B,Seq(
    OP -> true.B,
    OP_IMM -> true.B,
    LOAD -> true.B,  // Don't care, the reslut will be flushed
    JALR -> true.B,
    JAL -> true.B,
    AUIPC -> true.B,
    LUI -> true.B,
  ))
  // Use rd in WB stage 
  val is_W_use_rd = W_reg_en

  // Hazard condition (rd, rs overlap)
  val is_E_rs1_M_rd_overlap = Wire(Bool())
  val is_E_rs2_M_rd_overlap = Wire(Bool())

  val is_E_rs1_W_rd_overlap = Wire(Bool())
  val is_E_rs2_W_rd_overlap = Wire(Bool())

  val is_D_rs1_W_rd_overlap = Wire(Bool())
  val is_D_rs2_W_rd_overlap = Wire(Bool())

  is_E_rs1_M_rd_overlap := is_E_use_rs1 && is_M_use_rd && (EXE_rs1 === MEM_rd) && (MEM_rd =/= 0.U(5.W))
  is_E_rs2_M_rd_overlap := is_E_use_rs2 && is_M_use_rd && (EXE_rs2 === MEM_rd) && (MEM_rd =/= 0.U(5.W))

  is_E_rs1_W_rd_overlap := is_E_use_rs1 && is_W_use_rd && (EXE_rs1 === WB_rd) && (WB_rd =/= 0.U(5.W))
  is_E_rs2_W_rd_overlap := is_E_use_rs2 && is_W_use_rd && (EXE_rs2 === WB_rd) && (WB_rd =/= 0.U(5.W))

  is_D_rs1_W_rd_overlap := is_D_use_rs1 && is_W_use_rd && (ID_rs1 === WB_rd) && (WB_rd =/= 0.U(5.W))
  is_D_rs2_W_rd_overlap := is_D_use_rs2 && is_W_use_rd && (ID_rs2 === WB_rd) && (WB_rd =/= 0.U(5.W))

  // WB Hazard (rs2, rs1) - to stage Reg
  io.W_wb_data_hazard := Cat(is_D_rs2_W_rd_overlap,is_D_rs1_W_rd_overlap)

  // Stall -- stall for Data Hazard (stall PC, stage ID, stage EXE. Flush stage MEM to make a Bubble)
  io.Stall_DH := (is_E_rs1_M_rd_overlap||is_E_rs2_M_rd_overlap) && (MEM_opcode === LOAD) 

  // Control signal - Flush
  //-- Flush for Predict_Miss (Flush stage ID, stage EXE, make 2 Bubbles)
  io.Flush := Predict_Miss

  // Control signal - Data Forwarding
  //-- rs1 - Select the newest data
  when(is_E_rs1_M_rd_overlap){
    io.E_rs1_data_sel := MEM_STAGE
  }.elsewhen(is_E_rs1_W_rd_overlap){
    io.E_rs1_data_sel := WB_STAGE
  }.elsewhen(io.WBD_wb_data_hazard(0)){
    io.E_rs1_data_sel := WBD_STAGE
  }.otherwise{
    io.E_rs1_data_sel := EXE_STAGE
  }

  //-- rs2 - Select the newest data
  when(is_E_rs2_M_rd_overlap){
    io.E_rs2_data_sel := MEM_STAGE
  }.elsewhen(is_E_rs2_W_rd_overlap){
    io.E_rs2_data_sel := WB_STAGE
  }.elsewhen(io.WBD_wb_data_hazard(1)){
    io.E_rs2_data_sel := WBD_STAGE
  }.otherwise{
    io.E_rs2_data_sel := EXE_STAGE
  }

  /****************** Data Hazard End******************/

  
}
