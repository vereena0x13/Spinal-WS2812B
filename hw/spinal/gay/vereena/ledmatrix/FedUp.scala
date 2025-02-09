package gay.vereena.ledmatrix

import spinal.lib._
import spinal.core._
import spinal.core.sim._

import Util._



object FedUp {
    private def createMatrixRAM(size: Int, initialRamData: Option[Seq[Int]]) = {
        val init = initialRamData match {
            case Some(data) => {
                require(size == data.size, s"${size} != ${data.size}")
                data.map(U(_))
            }
            case None => Seq.fill(size)(U(0, 8 bits))
        }
        Mem(UInt(8 bits), init)
    }

    private def createStripROM(size: Int) = {
        val xs = Pride.prideSeq.flatten
        val init = Seq.tabulate(size)(x => xs(x % xs.size))
        Mem(UInt(8 bits), init)
    }
}


case class FedUp(initialRamData: Option[Seq[Int]]) extends Component {
    import FedUp._

    val io = new Bundle {
        val uart                    = UartBus()
        
        val gpio_a13                = out Bool()    // led strip dout
        val gpio_h4                 = out Bool()    // led matrix dout
        val gpio_v12                = in Bool()     // enc_key
        val gpio_r11                = in Bool()     // enc_b
        val gpio_u13                = in Bool()     // enc_a
    }
    import io._




    val brightness = new Area {
        val encoder                 = RotaryEncoder()
        encoder.io.enc_a            := gpio_u13
        encoder.io.enc_b            := gpio_r11

        val value                   = Reg(UInt(4 bits)) init(7)

        when(encoder.io.count_cw) {
            value                   := value +| 1
        } elsewhen(encoder.io.count_ccw) {
            value                   := value -| 1
        }
    }




    val matrix = new Area {
        val cfg = LedMatrixConfig(
            tile_width              = 16,
            tile_height             = 16,
            tiles_x                 = 2,
            tiles_y                 = 1
        )
        println(cfg)


        // TODO: double buffer?
        val ram                     = createMatrixRAM(cfg.memory_size, initialRamData)

        val ram_waddr               = cfg.atype()
        val mem_wdata               = UInt(8 bits)
        val ram_write               = Bool()

        val ram_raddr               = cfg.atype()
        val ram_read                = Bool()

        ram.write(
            enable                  = ram_write,
            address                 = ram_waddr,
            data                    = mem_wdata
        )

        val ram_rdata               = ram.readSync(
            address                 = ram_raddr,
            enable                  = ram_read
        )


        val uartHandler             = UartHandler(cfg)
        uartHandler.io.uart         <> uart
        ram_waddr                   := uartHandler.io.mem_waddr
        mem_wdata                   := uartHandler.io.mem_wdata
        ram_write                   := uartHandler.io.mem_write


        val ledMatrix               = LedMatrix(cfg)
        gpio_h4                     := ledMatrix.io.dout
        ram_raddr                   := ledMatrix.io.mem_raddr
        ram_read                    := ledMatrix.io.mem_read
        ledMatrix.io.mem_rdata      := ram_rdata
    }




    val strip = new Area {
        val stripCfg = new LedStripConfig(
            pixels                  = 300
        )
        println(stripCfg)


        val stripRom                = createStripROM(stripCfg.memory_size)

        val rom_raddr               = stripCfg.atype()
        val rom_read                = Bool()

        val rom_rdata               = stripRom.readSync(
            address                 = rom_raddr,
            enable                  = rom_read
        )
        
        /*
        TODO: this code belongs here rather than in LedStrip, obviously, but
              having LedStrip be responsible for addressing the individual bytes
              within each pixel makes it annoying to put it here, so, until I
              refactor things and get shit decoupled, it's there instead.

        val offset                  = Reg(stripCfg.atype())
        val offsetClkDiv            = CounterFreeRun(10_000_000) // 10_000_000 * 10ns = 0.1s
        when(offsetClkDiv.willOverflow) {
            offset                  := offset + 1
        }
        */

        val ledStrip                = LedStrip(stripCfg)
        gpio_a13                    := ledStrip.io.dout
        rom_raddr                   := ledStrip.io.mem_raddr
        rom_read                    := ledStrip.io.mem_read
        ledStrip.io.mem_rdata       := rom_rdata
        ledStrip.io.brightness      := brightness.value
    }
}