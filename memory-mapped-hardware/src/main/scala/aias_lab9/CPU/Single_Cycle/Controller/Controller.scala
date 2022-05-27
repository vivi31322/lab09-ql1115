package aias_lab9.Single_Cycle.Controller

import chisel3._
import chisel3.util._


object opcode_map {
    val LOAD      = "b0000011".U
    val STORE     = "b0100011".U
    val BRANCH    = "b1100011".U
    val JALR      = "b1100111".U
    val JAL       = "b1101111".U
    val OP_IMM    = "b0010011".U
    val OP        = "b0110011".U
    val AUIPC     = "b0010111".U
    val LUI       = "b0110111".U
    val HCF       = "b0001011".U
}

object ALU_op{
  val ADD  = 0.U
  val SLL  = 1.U
  val SLT  = 2.U
  val SLTU = 3.U
  val XOR  = 4.U
  val SRL  = 5.U
  val OR   = 6.U
  val AND  = 7.U
  val SUB  = 8.U
  val SRA  = 13.U
}

object condition{
  val EQ = "b000".U
  val NE = "b001".U
  val LT = "b100".U
  val GE = "b101".U
  val LTU = "b110".U
  val GEU = "b111".U
}

import opcode_map._,condition._,ALU_op._

class Controller extends Module {
    val io = IO(new Bundle{
        val Inst = Input(UInt(32.W))
        val BrEq = Input(Bool())
        val BrLT = Input(Bool())

        val PCSel = Output(Bool())
        val ImmSel = Output(UInt(3.W))
        val RegWEn = Output(Bool())
        val BrUn = Output(Bool())
        val BSel = Output(Bool())
        val ASel = Output(Bool())
        val ALUSel = Output(UInt(4.W))
        val MemRW = Output(Bool())
        val WBSel = Output(UInt(2.W))

        //new
        val Lui = Output(Bool())
        val Hcf = Output(Bool())

        //Axi
        val pc_stall = Output(Bool())
    })
    
    val opcode = Wire(UInt(7.W))
    opcode := io.Inst(6,0)

    val funct3 = Wire(UInt(3.W))
    funct3 := io.Inst(14,12)

    val funct7 = Wire(UInt(7.W))
    funct7 := io.Inst(31,25)

    //Control signal
    io.RegWEn := Mux(opcode===STORE||opcode===BRANCH||opcode===HCF,false.B,true.B)
    io.ASel := Mux(opcode===BRANCH || opcode===JAL || opcode===AUIPC,true.B,false.B)
    io.BSel := Mux(opcode===OP,false.B,true.B)
    io.BrUn := funct3(1)
    io.MemRW := Mux(opcode===STORE,true.B,false.B) //0:Read 1:Write
    // io.ctrl_Br := opcode === BRANCH
    io.Lui := opcode === LUI
    io.Hcf := opcode === HCF

    io.PCSel := false.B
    when(opcode === BRANCH){
        when(funct3===EQ && io.BrEq){io.PCSel := true.B}
        when(funct3===NE && !io.BrEq){io.PCSel := true.B}
        when(funct3===LT && io.BrLT){io.PCSel := true.B}
        when(funct3===GE && !io.BrEq && !io.BrLT){io.PCSel := true.B}
        when(funct3===LTU && io.BrLT){io.PCSel := true.B}
        when(funct3===GEU && !io.BrEq && !io.BrLT){io.PCSel := true.B}
    }.elsewhen(opcode === JAL || opcode === JALR ){
        io.PCSel := true.B
    }

    when(opcode===LOAD){io.WBSel := 0.U(2.W)} //from Data Memory
    .elsewhen(opcode===JAL||opcode===JALR){io.WBSel := 2.U(2.W)} // from PC+4
    .otherwise{io.WBSel:=1.U(2.W)} // from ALU
    

    io.ImmSel := MuxLookup(opcode,0.U,Seq(
        //R-type
        OP -> 0.U,
        //I-type
        OP_IMM -> 1.U,
        LOAD -> 1.U,
        //S-type
        STORE -> 2.U,
        //B-type
        BRANCH -> 3.U,
        //J-type
        JAL -> 4.U,
        JALR -> 4.U,
        //U-type
        LUI -> 5.U,
        AUIPC -> 5.U
    ))
    
    io.ALUSel := 0.U //add
    when(opcode===OP){
        io.ALUSel := Cat(Mux((funct3 === ADD || funct3 === SRL),funct7(5),0.U),funct3)
    }.elsewhen(opcode === OP_IMM){
        io.ALUSel := Cat(Mux((funct3 === SRL),funct7(5),0.U),funct3)
    }

    io.pc_stall := opcode===LOAD || opcode===STORE
}