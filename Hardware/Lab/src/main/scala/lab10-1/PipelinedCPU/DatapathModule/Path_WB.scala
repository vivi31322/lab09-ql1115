package lab10-1.PiplinedCPU.DatapathModule

import chisel3._
import chisel3.util._
import lab10-1.PiplinedCPU.wb_sel_map._

class Path_WB(addrWidth:Int) extends Module {
    val io = IO(new Bundle{

        val WB_pc_plus4_in = Input(UInt(addrWidth.W))
        val WB_alu_out_in = Input(UInt(32.W))
        val WB_ld_data_in = Input(UInt(32.W))
        val W_WBSel = Input(UInt(2.W))

        val WB_wdata = Output(UInt(32.W))
    })
    
    // WB Wire
    val wb_data_wire = Wire(UInt(32.W))
    wb_data_wire := MuxLookup(io.W_WBSel, 0.U, Seq(
            PC_PLUS_4 -> stage_WB.io.pc_plus4,  //from PC (+4)
            ALUOUT -> stage_WB.io.alu_out,   //from ALU
            LD_DATA ->  stage_WB.io.ld_data, //from DataMemory
        ))
}
