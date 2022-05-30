package aias_lab9.SystolicArray

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

class tile(rows:Int,
           cols:Int,
           bits:Int) extends Module{
    val io = IO(new Bundle{
        val input  = Input(Vec(rows,Valid(UInt(bits.W))))
        val weight = Input(Vec(cols,Valid(UInt(bits.W))))
        val preload = Input(Bool())
        val output = Output(Vec(cols,Valid(UInt((2*bits).W))))
    })

    // sa: systolic array
    val sa = Array.fill(rows,cols)(Module(new PE(bits)).io)

    for(i <- 0 until rows){
        for(j <- 0 until cols){
            
            //Wiring: input
            if(j==0){
                sa(i)(j).input <> io.input(i)
            }else{
                sa(i)(j).input <> sa(i)(j-1).fwd_input
            }

            //Wiring: preload
            sa(i)(j).preload := io.preload
            
            //Wiring: weight & partial sum
            if(i==0){
                sa(i)(j).weight <> io.weight(j)
                sa(i)(j).ps:=0.U
            }else{
                sa(i)(j).weight <> sa(i-1)(j).fwd_weight
                sa(i)(j).ps := sa(i-1)(j).fwd_ps.bits
            }
        }   
    }

    List.range(0,cols).map{x => io.output(x) <> sa(rows-1)(x).fwd_ps}
}