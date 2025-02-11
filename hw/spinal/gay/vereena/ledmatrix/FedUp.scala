package gay.vereena.ledmatrix

import scala.math._

import spinal.lib._
import spinal.core._
import spinal.core.sim._

import gay.vereena.ledmatrix.misc._
import scala.annotation.tailrec



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

    private def createStripROM(memory_size: Int) = {
        val xs = Pride.prideSeq.map(U(_, 8 bits))
        val init = Seq.tabulate(memory_size)(i => if(i < xs.size) xs(i) else U(0, 8 bits))
        Mem(UInt(8 bits), init)
    }
}


case class FedUp(initialRamData: Option[Seq[Int]]) extends Component {
    import FedUp._

    val io = new Bundle {
        val uart                    = UartBus()
        
        val gpio_strip_dout         = out Bool()
        val gpio_matrix_dout        = out Bool()
        
        val gpio_enc_key            = in Bool()
        val gpio_enc_b              = in Bool()
        val gpio_enc_a              = in Bool()
        
        val gpio_til311_data        = out UInt(4 bits)
        val gpio_til311_strobe      = out Bool()
        val gpio_til311_blank       = out Bool()
    }
    import io._




    val brightness = new Area {
        val encoder                 = RotaryEncoder()
        encoder.io.enc_a            := gpio_enc_a
        encoder.io.enc_b            := gpio_enc_b

        val keyDebouncer            = Debouncer(true)
        keyDebouncer.io.din         := gpio_enc_key

        val stripEnabled            = Reg(Bool()) init(True)
        when(keyDebouncer.io.dout.fall()) {
            stripEnabled            := ~stripEnabled
        }

        val valueCnt                = Reg(UInt(4 bits)) init(7)
        when(encoder.io.count_cw) {
            valueCnt                := valueCnt +| 1
        } elsewhen(encoder.io.count_ccw) {
            valueCnt                := valueCnt -| 1
        }

        val value                   = Mux(stripEnabled, valueCnt, U(0))

        val til311                  = TIL311()
        til311.io.value             := value
        gpio_til311_data            := til311.io.data
        gpio_til311_strobe          := til311.io.strobe
        gpio_til311_blank           := Mux(stripEnabled, til311.io.blank, True)
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
        gpio_matrix_dout            := ledMatrix.io.dout
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
        

        val ledStrip                = LedStrip(stripCfg)
        gpio_strip_dout             := ledStrip.io.dout
        rom_read                    := ledStrip.io.mem_read
        ledStrip.io.mem_rdata       := rom_rdata
        ledStrip.io.brightness      := brightness.value

        val max_addr                = Pride.prideSeq.size
        val offset                  = Reg(UInt(log2Up(max_addr) bits)) init(0)
        val offsetClkDiv            = Counter(10_000_000) // 10_000_000 * 10ns = 0.1s
        val decOffset               = Reg(Bool()) init(False)
        when(decOffset && ledStrip.io.is_resetting) {
            decOffset               := False
            when(offset === 0) {
                offset              := max_addr
            } otherwise {
                offset              := offset - 3
            }
        } elsewhen(!decOffset) {
            offsetClkDiv.increment()
            when(offsetClkDiv.willOverflow) {
                decOffset           := True
            }
        }
        
        val paddr                   = ledStrip.io.mem_raddr.resize(stripCfg.addr_width + 1)
        val iaddr                   = paddr //+ offset
        when(iaddr < max_addr) {
            rom_raddr               := iaddr.resized
        } otherwise {
            rom_raddr               := (iaddr - max_addr).resized
        }
    }
}