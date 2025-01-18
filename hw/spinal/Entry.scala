import java.io._

import scala.collection.mutable.ArrayBuffer
import scala.math
import scala.util._

import spinal.lib._
import spinal.core._
import spinal.core.sim._

import gay.vereena.ledmatrix.Util._
import gay.vereena.ledmatrix.FedUp



object GenerateSOC extends App {
    spinalConfig()
        .generateVerilog(FedUp(None))
        .printPruned()
        .printPrunedIo()
}


object SimulateSOC extends App {
    SimConfig
        .withWave
        .withConfig(spinalConfig())
        .compile {
            // TODO: socConfig, so we can uh-hardcode the matrix size
            val ramData: Seq[Int] = List.tabulate(16*16)(i => {
                val y   = 15 - (i / 16)
                val xx  = i % 16
                val x   = if((y & 1) == 0) 15 - xx
                          else             xx
                val j = x + y * 16
                (j & 0b00111111)
            }).flatMap(j => List(j, j | 1, j | 2))
            
            val soc = FedUp(Some(ramData))
            
            soc.io.uart.rdata.simPublic()
            soc.io.uart.txe.simPublic()
            soc.io.uart.rxf.simPublic()
            soc.io.gpio_a13.simPublic()
            soc.ram.simPublic()
            soc.matrix.io.simPublic()
            soc.matrix.io.dout.simPublic()
            soc.matrix.timer.simPublic()
            soc.matrix.px.simPublic()
            soc.matrix.py.simPublic()
            soc.matrix.pbyte.simPublic()
            soc.matrix.pbit.simPublic()
            

            soc
        }
        .doSim { soc =>
            soc.io.uart.rdata #= 0
            soc.io.uart.txe   #= false
            soc.io.uart.rxf   #= true
            
            val clk = soc.clockDomain

            clk.fallingEdge()
            sleep(0)

            clk.assertReset()
            for(_ <- 0 until 10) {
                clk.clockToggle()
                sleep(1)
            }
            clk.deassertReset()
            sleep(1)


            def runCycles(n: Int, onDout: (Boolean) => Unit): Unit = {
                val timer   = soc.matrix.timer
                val dout    = soc.matrix.io.dout
                var did     = false
                for(i <- 0 until n) {
                    clk.clockToggle()
                    sleep(1)
                    clk.clockToggle()
                    sleep(1)

                    if(!did && (timer.toLong == 85 || timer.toLong == 45)) {
                        did = true
                        onDout(dout.toBoolean)                        
                    } else if(did && timer.toLong == 124) {
                        did = false
                    }
                }
            }

            val N       = 1000 * 250
            var col     = 0
            /*
            val w = new PrintWriter(new FileWriter("dout_expected.txt"))
            var col = 0
            runCycles(N, v => {
                w.write(if(v) '1' else '0')
                col += 1
                if(col == 16) {
                    col = 0
                    w.write('\n')
                }
            })
            w.close()
            */
            val matrix  = soc.matrix
            val r       = new BufferedReader(new FileReader("dout_expected.txt"))
            runCycles(N, v => {
                val w = if(v) '1' else '0'
                assert(w == r.read(), {
                    Seq(
                        s"px: ${matrix.px.toLong}",
                        s"py: ${matrix.py.toLong}",
                        s"pbyte: ${matrix.pbyte.toLong}",
                        s"pbit: ${matrix.pbit.toLong}"
                    ).map(_ + '\n').reduce(_+_)
                })
                col += 1
                if(col == 16) {
                    col = 0
                    r.read()
                }
            })
            r.close()
        }
}