import java.io._

import scala.collection.mutable.ArrayBuffer
import scala.math
import scala.util._

import spinal.lib._
import spinal.core._
import spinal.core.sim._

import gay.vereena.ledmatrix._
import gay.vereena.ledmatrix.Util._



object GenerateSOC extends App {
    spinalConfig()
        .generateVerilog(FedUp(None))
        .printPruned()
        .printPrunedIo()
}


object SimulateSOC extends App {
    SimConfig
        .withFstWave
        .withConfig(spinalConfig())
        .compile {
            // TODO: socConfig, so we can uh-hardcode the matrix size
            val ramData: Seq[Int] = List.tabulate(32*16)(i => {
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
            soc.matrix.ram.simPublic()
            soc.matrix.ledMatrix.io.simPublic()
            soc.matrix.ledMatrix.io.dout.simPublic()
            soc.matrix.ledMatrix.timer.simPublic()
            soc.matrix.ledMatrix.px.simPublic()
            soc.matrix.ledMatrix.py.simPublic()
            soc.matrix.ledMatrix.pbyte.simPublic()
            soc.matrix.ledMatrix.pbit.simPublic()
            

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
                val timer   = soc.matrix.ledMatrix.timer
                val dout    = soc.matrix.ledMatrix.io.dout
                var did     = false
                for(i <- 0 until n) {
                    if(i % 10_000 == 0) println(i)

                    clk.clockToggle()
                    sleep(1)
                    clk.clockToggle()
                    sleep(1)

                    if(!did && timer.toLong == 45) {
                        did = true
                        onDout(dout.toBoolean)                        
                    } else if(did && timer.toLong == 124) {
                        did = false
                    }
                }
            }

            val matrix  = soc.matrix
            val N       = 1000 * 1000
            var col     = 0
            runCycles(N, _ => ())

            
            /*          
            val w = new PrintWriter(new FileWriter("dout_expected.txt"))
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

            /*
            val r       = new BufferedReader(new FileReader("dout_expected.txt"))
            runCycles(N, v => {
                val w = if(v) '1' else '0'
                val ex = r.read().toChar
                assert(w == ex, {
                    Seq(
                        "",
                        s"expected ${ex}, got ${w}",
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
            */
        }
}


object SimulateUartHandler extends App {
    SimConfig
        .withFstWave
        .withConfig(spinalConfig())
        .compile {
            val dut = UartHandler(LedMatrixConfig(16, 16, 1, 1))

            dut.io.uart.rxf.simPublic()
            dut.io.uart.rd.simPublic()
            dut.io.uart.rdata.simPublic()
            dut.io.mem_waddr.simPublic()
            dut.io.mem_wdata.simPublic()
            dut.io.mem_write.simPublic()

            dut
        }
        .doSim { dut =>
            dut.io.uart.rdata #= 0
            dut.io.uart.rxf   #= true
            
            val clk = dut.clockDomain

            clk.fallingEdge()
            sleep(0)

            clk.assertReset()
            for(_ <- 0 until 10) {
                clk.clockToggle()
                sleep(1)
            }
            clk.deassertReset()
            sleep(1)
            

            def tickOnce(): Unit = {
                clk.clockToggle()
                sleep(1)
                clk.clockToggle()
                sleep(1)
            }

            def tick(n: Int): Unit = { for(_ <- 0 until n) { tickOnce() } }
            def tickUntil(c: () => Boolean): Unit = { while(!c()) tickOnce() }

            tick(5)


            import dut.io._
            import dut.io.uart._

            for(i <- 0 until 5) {
                rxf     #= false
                tickUntil(() => rd.toBoolean)

                rdata   #= 42 + i
                tick(4)

                rxf     #= true 
                tick(4)
            }

            tick(30)
        }
}


object SimulateDebouncer extends App {
    SimConfig
        .withFstWave
        .withConfig(spinalConfig())
        .compile {
            val dut = Debouncer(false, 4)

            dut.io.din.simPublic()
            dut.io.dout.simPublic()

            dut
        }
        .doSim { dut =>
            val clk = dut.clockDomain

            def tickOnce(): Unit = {
                clk.clockToggle()
                sleep(1)
                clk.clockToggle()
                sleep(1)
            }

            def tick(n: Int): Unit = { for(_ <- 0 until n) { tickOnce() } }


            import dut.io._
            
            din #= false
            tick(6)


            din #= true
            tick(5)
            
            din #= false
            tick(3)
            din #= true
            tick(5)


            din #= false
            tick(5)


            din #= true
            tick(3)
            din #= false
            tick(12)

            din #= true
            tick(12)
        }
}