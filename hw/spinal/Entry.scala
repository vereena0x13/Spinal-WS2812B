import scala.collection.mutable.ArrayBuffer
import scala.math

import spinal.lib._
import spinal.core._
import spinal.core.sim._

import Util._
import FedUp._



object GenerateSOC extends App {
    spinalConfig().generateVerilog(FedUp())
}


object SimulateSOC extends App {
    SimConfig
        .withWave
        .withConfig(spinalConfig())
        .compile {
            val soc = FedUp()
            
            soc.io.uart.rdata.simPublic()
            soc.io.uart.txe.simPublic()
            soc.io.uart.rxf.simPublic()
            soc.io.gpio_a13.simPublic()
            
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


            for(i <- 0 until 850000) {
                clk.clockToggle()
                sleep(1)
                clk.clockToggle()
                sleep(1)
            }
        }
}