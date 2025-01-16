import scala.collection.mutable.ArrayBuffer
import scala.math

import spinal.lib._
import spinal.core._
import spinal.core.sim._

import Util._

import java.io.File
import javax.imageio.ImageIO



object FedUp {
    private val WIDTH               = 16
    private val HEIGHT              = 16
    private val PIXELS              = WIDTH * HEIGHT
    private val BYTES_PER_PIXEL     = 3
    private val ADDR_WIDTH          = log2Up(PIXELS * BYTES_PER_PIXEL)

    def atype()                     = UInt(ADDR_WIDTH bits)

    private def createRAM() = {
        val ramSize = WIDTH * HEIGHT * 3
        val img = ImageIO.read(new File("v.jpg"))
        val init = new Array[UInt](ramSize)
        for(y <- 0 until HEIGHT) {
            for(x <- 0 until WIDTH) {
                val p = img.getRGB(x, y)
                val i = (x + y * WIDTH) * 3
                val r = (p & 0xFF0000) >> 16
                val g = (p & 0x00FF00) >> 8
                val b = p & 0x0000FF
                init(i + 0) = U(r >> 5, 8 bits)
                init(i + 1) = U(g >> 5, 8 bits)
                init(i + 2) = U(b >> 5, 8 bits)
            }
        }
        Mem(UInt(8 bits), init)
    }
}


case class FedUp() extends Component {
    import FedUp._

    val io = new Bundle {
        val uart = UartBus()
        val gpio_a13 = out(Bool())
    }
    import io._


    uart.wdata      := 0
    uart.wr         := False
    uart.rd         := False



    val ram         = createRAM() //Mem(UInt(8 bits), WIDTH * HEIGHT & BYTES_PER_PIXEL)

    val ram_waddr   = atype()
    val ram_din     = UInt(8 bits)
    val ram_write   = Bool()

    val ram_raddr   = atype()
    val ram_read    = Bool()

    ram.write(
        enable      = ram_write,
        address     = ram_waddr,
        data        = ram_din
    )

    val ram_dout = ram.readSync(
        address     = ram_raddr,
        enable      = ram_read
    )

    ram_waddr       := 0
    ram_din         := 0
    ram_write       := False



    val dout        = Reg(Bool()) init(True)
    gpio_a13        := dout

    val timer       = Reg(UInt(32 bits)) init(0)
    val prst        = Reg(Bool()) init(False)
    val px          = Reg(UInt(8 bits)) init(0)
    val py          = Reg(UInt(8 bits)) init(0)
    val pbit        = Reg(UInt(5 bits)) init(0)

    val pg          = pbit < 8
    val pr          = pbit >= 8 && pbit < 16
    val pb          = pbit >= 16

    val apx         = UInt(8 bits)
    when(py(0)) {
        apx         := WIDTH - 1 - px
    } otherwise {
        apx         := px
    }
    val apy         = HEIGHT - 1 - py


    ram_raddr       := 0
    ram_read        := !prst && timer < 100

    val want        = False
    val bit         = 7 - pbit(2 downto 0)
    when(pr) {
        val idx     = (apx + apy * WIDTH) * 3
        ram_raddr   := idx(9 downto 0)
        want        := ram_dout(bit)
    } elsewhen(pg) {
        val idx     = ((apx + apy * WIDTH) * 3) + 1
        ram_raddr   := idx(9 downto 0)
        want        := ram_dout(bit)
    } elsewhen(pb) {
        val idx     = ((apx + apy * WIDTH) * 3) + 2
        ram_raddr   := idx(9 downto 0)
        want        := ram_dout(bit)
    }
    

    when(prst) {
        when(timer === 39999) {
            timer := 0
            prst := False
            dout := True
        } otherwise {
            timer := timer + 1
        }
    } otherwise {
        when(timer === 124) {
            timer := 0
            
            when(pbit === 23) {
                pbit := 0

                when(px === WIDTH - 1) {
                    px := 0
                    when(py === HEIGHT - 1) {
                        py := 0
                        prst := True
                    } otherwise {
                        py := py + 1
                        dout := True
                    }
                } otherwise {
                    px := px + 1
                    dout := True
                }
            } otherwise {
                pbit := pbit + 1
                dout := True
            }
        } otherwise {
            timer := timer + 1

            when(want && timer === 84) {
                dout := False
            } elsewhen(!want && timer === 44) {
                dout := False
            }
        }
    }
}



object GenerateSOC extends App {
    spinalConfig.generateVerilog(FedUp())
}



object SimulateSOC extends App {
    SimConfig
        .withWave
        .withConfig(spinalConfig)
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