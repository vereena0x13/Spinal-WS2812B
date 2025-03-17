package gay.vereena.ledmatrix

import scala.math.max

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.fsm._

import gay.vereena.ledmatrix.util.FSMExtensions._



case class LedMatrixConfig(
    tile_width: Int,
    tile_height: Int,
    tiles_x: Int,
    tiles_y: Int
) {
    val tiles                   = tiles_x       *   tiles_y

    val total_width             = tile_width    *   tiles_x
    val total_height            = tile_height   *   tiles_y
    
    val tile_pixels             = tile_width    *   tile_height
    val total_pixels            = total_width   * total_height

    val bytes_per_pixel         = 3
    val memory_size             = total_pixels  *   bytes_per_pixel
    val addr_width              = log2Up(memory_size)

    def atype()                 = UInt(addr_width bits)

    override def toString(): String = {
        val sb = new StringBuilder
        sb.append("LedMatrixConfig(\n")
        sb.append(s"    tiles               = $tiles,\n")
        sb.append(s"    total_width         = $total_width,\n")
        sb.append(s"    total_height        = $total_height,\n")
        sb.append(s"    tile_pixels         = $tile_pixels,\n")
        sb.append(s"    total_pixels        = $total_pixels,\n")
        sb.append(s"    bytes_per_pixel     = $bytes_per_pixel,\n")
        sb.append(s"    memory_size         = $memory_size,\n")
        sb.append(s"    addr_width          = $addr_width\n")
        sb.append(")")
        sb.toString
    }
}

case class LedMatrix(cfg: LedMatrixConfig) extends Component {
    import WS2812B._
    import cfg._

    val io = new Bundle {
        val dout                = out(Bool())

        val mem_raddr           = out(atype())
        val mem_read            = out(Bool())
        val mem_rdata           = in(UInt(8 bits))

        val blank               = in(Bool())
    }
    import io._

    
    // TODO: don't constantly refresh at max speed; only on change
    dout.setAsReg() init(True)

    val timer                   = Reg(UInt(log2Up(TRST) bits)) init(0)
    val tx                      = Reg(UInt(max(log2Up(tiles_x), 1) bits)) init(0)
    val ty                      = Reg(UInt(max(log2Up(tiles_y), 1) bits)) init(0)
    val px                      = Reg(UInt(log2Up(tile_width) bits)) init(0)
    val py                      = Reg(UInt(log2Up(tile_height) bits)) init(0)
    val pbyte                   = Reg(UInt(2 bits)) init(0)
    val pbit                    = Reg(UInt(3 bits)) init(0)
    val curByte                 = Reg(UInt(8 bits))
    

    // NOTE TODO: calculation of apx and apy should be configurable
    val apx                     = Mux(py(0), tile_width - 1 - px, px)
    val apy                     = tile_height - 1 - py


    // NOTE TODO: what should be the source of pixel data? what if
    //            someone bits to generate pixels on the fly w/o mem, etc.?
    //            perhaps we have a PixelBus that can emit read requests (from here)
    //            and get pixels as a response?
    val pbytem                  = pbyte.muxDc(
                                    0 -> U(1, 2 bits),
                                    1 -> U(0, 2 bits),
                                    2 -> U(2, 2 bits)
                                )
    val gx                      = apx + tx * tile_width
    val gy                      = apy + ty * tile_height
    val pidx                    = gx + gy * total_width
    val paddr                   = pbytem + pidx * bytes_per_pixel
    mem_raddr                   := paddr((addr_width - 1) downto 0)
    mem_read                    := False

    val bit                     = Mux(blank, False, curByte(7 - pbit))
    

    val fsm                     = new StateMachine {
        val readNextByte        = new State with EntryPoint
        val outputBitShape      = new State
        val bitComplete         = new State
        val byteComplete        = new State
        val pixelComplete       = new State
        val rowComplete         = new State
        val tileComplete        = new State
        val tileRowComplete     = new State
        val outputRst           = new State
 
        readNextByte.counting(      timer,  1,              outputBitShape                      ).whenIsActive(mem_read := True).onExit(curByte := mem_rdata)
        outputBitShape.counting(    timer,  TBIT,           bitComplete                         ).onEntry(dout := True)
        bitComplete.counting(       pbit,   7,              byteComplete,       readNextByte    )
        byteComplete.counting(      pbyte,  2,              pixelComplete,      readNextByte    )
        pixelComplete.counting(     px,     tile_width-1,   rowComplete,        readNextByte    )
        rowComplete.counting(       py,     tile_height-1,  tileComplete,       readNextByte    )
        tileComplete.counting(      tx,     tiles_x-1,      tileRowComplete,    readNextByte    )
        tileRowComplete.counting(   ty,     tiles_y-1,      outputRst,          readNextByte    )
        outputRst.counting(         timer,  TRST,           readNextByte                        ).onEntry(dout := False)

        outputBitShape.whenIsActive {
            val t = Mux(bit, U(T1H), U(T0H))
            when(timer === t) { dout := False }
        }
    }
}