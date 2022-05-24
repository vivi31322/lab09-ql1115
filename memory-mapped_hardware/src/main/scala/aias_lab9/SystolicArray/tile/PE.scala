package aias_lab9.SystolicArray

import chisel3._
import chisel3.util._

class PE(val bits:Int = 8) extends Module{
    val io = IO(new Bundle{
        // input propagation (fwd: forwarded)
        val input  = Input(Valid(UInt(bits.W)))
        val fwd_input = Output(Valid(UInt(bits.W)))
        
        // weight propagation
        val weight  = Input(Valid(UInt(bits.W)))
        val fwd_weight = Output(Valid(UInt(bits.W)))

        val preload = Input(Bool())

        // partial sum propagation (ps: partial sum)
        val ps = Input(UInt((bits*2).W))
        val fwd_ps = Output(Valid(UInt((bits*2).W)))
    })

    val weightReg  = RegInit(0.U(bits.W))
    weightReg := Mux(io.preload,io.weight.bits,weightReg)

    io.fwd_weight <> RegNext(io.weight)
    io.fwd_input <> RegNext(io.input)

    io.fwd_ps.valid := RegNext(io.input.valid)
    io.fwd_ps.bits := RegNext(io.ps + weightReg * io.input.bits)
}

object PE extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(
        new PE(8),
        Array("-td","generated/PE")
    )
}