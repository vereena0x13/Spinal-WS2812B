package gay.vereena.ledmatrix

import scala.math.max

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.fsm._

import gay.vereena.ledmatrix.util.FSMExtensions._


case class LedStripConfig(
    val pixels: Int
) {
    val bytes_per_pixel         = 3
    val memory_size             = pixels * bytes_per_pixel
    val addr_width              = log2Up(memory_size)

    def atype()                 = UInt(addr_width bits)

    override def toString(): String = {
        val sb = new StringBuilder
        sb.append("LedStripConfig(\n")
        sb.append(s"    pixels              = $pixels,\n")
        sb.append(s"    bytes_per_pixel     = $bytes_per_pixel,\n")
        sb.append(s"    memory_size         = $memory_size,\n")
        sb.append(s"    addr_width          = $addr_width\n")
        sb.append(")")
        sb.toString
    }
}

case class LedStrip(cfg: LedStripConfig) extends Component {
    import WS2812B._
    import cfg._

    val io = new Bundle {
        val dout                = out(Bool())

        val mem_raddr           = out(atype())
        val mem_read            = out(Bool())
        val mem_rdata           = in(UInt(8 bits))

        val brightness          = in(UInt(4 bits))
    }
    import io._

    
    // TODO: don't constantly refresh at max speed; only on change
    dout.setAsReg() init(True)

    val timer                   = Reg(UInt(log2Up(TRST) bits)) init(0)
    val pixel                   = Reg(UInt(log2Up(pixels) bits)) init(0)
    val pbyte                   = Reg(UInt(2 bits)) init(0)
    val pbit                    = Reg(UInt(3 bits)) init(0)
 
    val isResetting             = Bool()
    val curBrightness           = RegNextWhen(brightness, isResetting)


    // NOTE TODO: this "offset" thing doesn't belong in LedStrip; refactoring needed! :(
    val offset                  = Reg(atype()) init(0)
    val offsetClkDiv            = Counter(10_000_000) // 10_000_000 * 10ns = 0.1s
    val decOffset               = Reg(Bool()) init(False)
    when(decOffset && isResetting) {
        decOffset               := False
        when(offset === 0) {
            offset              := pixels - 1
        } otherwise {
            offset              := offset - 1
        }
    } elsewhen(!decOffset) {
        offsetClkDiv.increment()
        when(offsetClkDiv.willOverflow) {
            decOffset           := True
        }
    }


    // NOTE TODO: what should be the source of pixel data? what if
    //            someone bits to generate pixels on the fly w/o mem, etc.?
    //            perhaps we have a PixelBus that can emit read requests (from here)
    //            and get pixels as a response?
    val pbytem                  = pbyte.muxDc(
                                    0 -> U(1, 2 bits),
                                    1 -> U(0, 2 bits),
                                    2 -> U(2, 2 bits)
                                )
    
    val poff                    = pixel + offset
    val pidx                    = Mux(poff < pixels, poff, (poff - pixels)).resize(log2Up(pixels) bits)

    val paddr                   = pbytem + pidx * bytes_per_pixel
    mem_raddr                   := paddr((addr_width - 1) downto 0)

    val curByte                 = Reg(UInt(8 bits))
    val bit                     = curByte(7 - pbit)
    

    val gammaRom                = WS2812B.createGammaROM()


    val fsm                     = new StateMachine {
        val readNextByte        = new State
        val outputBitShape      = new State
        val bitComplete         = new State
        val byteComplete        = new State
        val pixelComplete       = new State
        val rowComplete         = new State
        val tileComplete        = new State
        val tileRowComplete     = new State
        val outputRst           = new State with EntryPoint
 
        readNextByte.counting(      timer,  1,              outputBitShape                      ).onExit(curByte := gammaRom((mem_rdata * curBrightness).resized))
        outputBitShape.counting(    timer,  TBIT,           bitComplete                         ).onEntry(dout := True)
        bitComplete.counting(       pbit,   7,              byteComplete,       readNextByte    )
        byteComplete.counting(      pbyte,  2,              pixelComplete,      readNextByte    )
        pixelComplete.counting(     pixel,  pixels-1,       outputRst,          readNextByte    )
        outputRst.counting(         timer,  TRST,           readNextByte                        ).onEntry(dout := False)

        outputBitShape.whenIsActive {
            val t = Mux(bit, U(T1H), U(T0H))
            when(timer === t) { dout := False }
        }


        mem_read                := isActive(readNextByte)
        isResetting             := isEntering(outputRst)
    }
}