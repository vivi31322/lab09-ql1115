package aias_lab9.SystolicArray

import chisel3._
import chisel3.iotesters.{Driver,PeekPokeTester}
import scala.language.implicitConversions
import java.io._


class SATest(dut:SA) extends PeekPokeTester(dut){

    implicit def bigint2bool(bi:BigInt) = if (bi!=0) true else false

    def mmul(a:Array[Array[Int]], b:Array[Array[Int]]): Array[Array[Int]]={
        for(row_vec <- a) yield {
            for(col_vec <- b.transpose) yield{
                (row_vec zip col_vec).map{case(x,y)=>x*y}.reduce(_+_)
            }
        }
    }

    def matInit(rows: Int, cols:Int, rseed: Int) : Array[Array[Int]] = {
        val maxval = 5
        val rnd = new scala.util.Random(rseed)
        //randomly generate the n*n matrix, the range of each element is 1~5
        Array.tabulate(rows){ _ => Array.tabulate(cols){_ => rnd.nextInt(maxval)+1}}
    }

    def printmat(m: Array[Array[Int]]): Unit = {
        m.foreach{
            r => r.foreach{v => print(f"$v%4d")} 
            println() 
        }
        println()
    }

    val rows = dut.rows
    val cols = dut.cols
    val a_mat = matInit(rows,cols,0)
    val b_mat = matInit(rows,cols,1)
    val c_mat = mmul(a_mat,b_mat)

    print("==A_mat \n")
    printmat(a_mat)
    print("==B_mat \n")
    printmat(b_mat)
    print("==C_mat \n")
    printmat(c_mat)

    val localMem_writer = new PrintWriter(new File("./src/main/resource/SystolicArray/LocalMem.hex" ))

    (a_mat.flatten).foreach{element=>localMem_writer.println(element.toHexString)}
    (b_mat.flatten).foreach{element=>localMem_writer.println(element.toHexString)}

    localMem_writer.close

    step(10)
    poke(dut.io.mmio.ENABLE,true.B)

    while(!(peek(dut.io.check))){
        step(1)
    }

    c_mat.foreach{row=>
        val row_data = row.reduce(_*256+_)
        expect(dut.io.c_rdata,row_data)
        step(1)
    }

    step(1)
}

object SATest extends App{
    Driver.execute(args,()=>new SA(4,4,8)){
        c:SA => new SATest(c)
    }
}