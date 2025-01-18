import scala.collection.mutable.ArrayBuffer
import scala.math

import spinal.lib._
import spinal.core._
import spinal.core.sim._

import gay.vereena.ledmatrix.Util._
import gay.vereena.ledmatrix.FedUp



object GenerateSOC extends App {
    spinalConfig().generateVerilog(FedUp(None))
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


            for(i <- 0 until 1000 * 85) {
                clk.clockToggle()
                sleep(1)
                clk.clockToggle()
                sleep(1)
            }
        }
}