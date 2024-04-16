package acal_lab09.PiplinedCPU.Controller

import chisel3._
import chisel3.util._

import acal_lab09.PiplinedCPU.opcode_map._
import acal_lab09.PiplinedCPU.condition._
import acal_lab09.PiplinedCPU.inst_type._
import acal_lab09.PiplinedCPU.alu_op_map._
import acal_lab09.PiplinedCPU.pc_sel_map._
import acal_lab09.PiplinedCPU.wb_sel_map._
import acal_lab09.PiplinedCPU.forwarding_sel_map._

class Controller(memAddrWidth: Int) extends Module {
  val io = IO(new Bundle {
    // Memory control signal interface
    val IM_Mem_R = Output(Bool())
    val IM_Mem_W = Output(Bool())
    val IM_Length = Output(UInt(4.W))
    val IM_Valid = Input(Bool())

    val DM_Mem_R = Output(Bool())
    val DM_Mem_W = Output(Bool())
    val DM_Length = Output(UInt(4.W))
    val DM_Valid = Input(Bool())

    // branch Comp.
    val E_BrEq = Input(Bool())
    val E_BrLT = Input(Bool())

    // Branch Prediction
    val E_Branch_taken = Output(Bool())
    val E_En = Output(Bool())

    val ID_pc = Input(UInt(memAddrWidth.W))
    val EXE_target_pc = Input(UInt(memAddrWidth.W))

    // Flush
    val Flush_WB_ID_DH = Output(Bool()) //TBD
    val Flush_BH = Output(Bool()) //TBD

    // Stall
    // To Be Modified
    val Stall_WB_ID_DH = Output(Bool()) //TBD
    val Stall_MA = Output(Bool()) //TBD

    // inst
    val IF_Inst = Input(UInt(32.W))
    val ID_Inst = Input(UInt(32.W))
    val EXE_Inst = Input(UInt(32.W))
    val MEM_Inst = Input(UInt(32.W))
    val WB_Inst = Input(UInt(32.W))

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

  val EXE_opcode = io.EXE_Inst(6, 0)
  val EXE_funct3 = io.EXE_Inst(14, 12)
  val EXE_funct7 = io.EXE_Inst(31, 25)

  val MEM_opcode = io.MEM_Inst(6, 0)
  val MEM_funct3 = io.MEM_Inst(14, 12)

  val WB_opcode = io.WB_Inst(6, 0)

  // Control signal - Branch/Jump
  val E_En = Wire(Bool())
  E_En := (EXE_opcode===BRANCH || EXE_opcode===JALR || EXE_opcode===JAL)         // To Be Modified
  val E_Branch_taken = Wire(Bool())
  E_Branch_taken := MuxLookup(EXE_opcode, false.B, Seq(
          BRANCH -> MuxLookup(EXE_funct3, false.B, Seq(
            "b000".U(3.W) -> io.E_BrEq.asUInt,
          )),
          JAL -> true.B, // 因為 JAL 和 JALR 一定會 taken
          JALR -> true.B,
        ))    // To Be Modified

  io.E_En := E_En
  io.E_Branch_taken := E_Branch_taken

  // pc predict miss signal
  val Predict_Miss = Wire(Bool())
  Predict_Miss := (E_En && E_Branch_taken && io.ID_pc=/=io.EXE_target_pc)

  // Control signal - PC
  when(Predict_Miss){
    io.PCSel := EXE_T_PC
  }.otherwise{
    io.PCSel := IF_PC_PLUS_4
  }

  // Control signal - Branch comparator
  io.E_BrUn := (io.EXE_Inst(13) === 1.U)

  // Control signal - Immediate generator
  io.D_ImmSel := MuxLookup(ID_opcode, 0.U, Seq(
    OP_IMM -> I_type,
    LOAD -> I_type,
    BRANCH -> B_type,
    LUI -> U_type,
    AUIPC -> U_type,
    JAL -> J_type,
    JALR -> I_type, // 小心 JALR 是 I-type，imm 只有 12 位
  )) // To Be Modified

  // Control signal - Scalar ALU
  // E_ASel: 0.U代表來自 rs1，1.U 代表來自 PC，2.U 代表來自zero
  io.E_ASel := MuxLookup(EXE_opcode, 0.U, Seq(
    BRANCH -> 1.U,
    LUI -> 2.U,
    AUIPC -> 1.U,
    JAL -> 1.U, // ql rd = cur_pc + 4; next_pc = cur_pc + (imm * 2)
    JALR -> 0.U // ql rd = cur_pc + 4; next_pc = rs1 + imm & (~0x1) // 小心 alignment 問題
  ))    // To Be Modified
  // io.E_BSel := 1.U // To Be Modified
  io.E_BSel := Mux(EXE_opcode === OP, 0.U, 1.U) // ql OP(0b0110011) 代表 R-type，否則是 imm

  io.E_ALUSel := MuxLookup(EXE_opcode, (Cat(0.U(7.W), "b11111".U, 0.U(3.W))), Seq(
    OP -> (Cat(EXE_funct7, "b11111".U, EXE_funct3)),
    OP_IMM -> (Cat(0.U(7.W), "b11111".U, EXE_funct3))
  )) // To Be Modified

  // Control signal - Data Memory
  io.DM_Mem_R := (MEM_opcode===LOAD)
  io.DM_Mem_W := (MEM_opcode===STORE)
  io.DM_Length := Cat(0.U(1.W),MEM_funct3) // length

  // Control signal - Inst Memory
  io.IM_Mem_R := true.B // always true
  io.IM_Mem_W := false.B // always false
  io.IM_Length := "b0010".U // always load a word(inst)

  // Control signal - Scalar Write Back
  io.W_RegWEn := MuxLookup(WB_opcode, false.B, Seq(
    OP_IMM -> true.B,
    LOAD -> true.B,
    LUI -> true.B,
    AUIPC -> true.B,
    OP -> true.B, // ql R-type
    JAL -> true.B, // ql JAL 和 JALR 會將 pc + 4 寫入 rd
    JALR -> true.B,
  ))  // To Be Modified


  io.W_WBSel := MuxLookup(WB_opcode, ALUOUT, Seq(
    LOAD -> LD_DATA,
    JAL -> PC_PLUS_4, // ql JAL 和 JALR 會將 pc + 4 寫入 rd
    JALR -> PC_PLUS_4, 
  )) // To Be Modified

  // Control signal - Others
  io.Hcf := (IF_opcode === HCF)

  /****************** Data Hazard ******************/

  // Control signal - Stall
  io.Stall_WB_ID_DH := false.B // Stall for Data Hazard
  io.Stall_MA := false.B // Stall for Waiting Memory Access
  // Control signal - Flush
  io.Flush_WB_ID_DH := false.B
  io.Flush_BH := false.B // ql original
  // io.Flush_BH := (ID_opcode === JAL || ID_opcode === JALR) // QA ql 當 unconditional jump 的時候，不用 flush 掉嗎？

  // Control signal - Data Forwarding (Bonus)

  /****************** Data Hazard End******************/


}
