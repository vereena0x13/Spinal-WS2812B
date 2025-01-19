package gay.vereena.ledmatrix

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.fsm._

import FSMExtensions._


/*
case class Pixel() extends Bundle {
    val r = UInt(8 bits)
    val g = UInt(8 bits)
    val b = UInt(8 bits)
}
*/


case class LedMatrixConfig(
    width: Int,
    height: Int,
) {
    val pixels          = width * height
    val bytes_per_pixel = 3
    val memory_size     = pixels * bytes_per_pixel
    val addr_width      = log2Up(memory_size)

    def atype()         = UInt(addr_width bits)
}

object LedMatrix {
    val TRST            = 39999
    val T0H             = 44
    val T1H             = 84
    val TBIT            = 124
}

case class LedMatrix(cfg: LedMatrixConfig) extends Component {
    import LedMatrix._
    import cfg._

    val io = new Bundle {
        val dout        = out(Bool())

        val mem_raddr   = out(atype())
        val mem_read    = out(Bool())
        val mem_rdata   = in(UInt(8 bits))
    }
    import io._

    
    // TODO: don't constantly refresh at max speed; only on change
    dout.setAsReg() init(True)

    val timer           = Reg(UInt(32 bits)) init(0)
    val px              = Reg(UInt(log2Up(cfg.width) bits)) init(0)
    val py              = Reg(UInt(log2Up(cfg.height) bits)) init(0)
    val pbyte           = Reg(UInt(2 bits)) init(0)
    val pbit            = Reg(UInt(3 bits)) init(0)

    // NOTE TODO: calculation of apx and apy should eventually be 
    //            configuratble, once we factor this stuff into its own Component
    val apx             = Mux(py(0), width - 1 - px, px)
    val apy             = height - 1 - py


    // NOTE TODO: what should be the source of pixel data? what if
    //            someone bits to generate pixels on the fly w/o mem, etc.?
    //            perhaps we have a PixelBus that can emit read requests (from here)
    //            and get pixels as a response?
    val pbytem          = pbyte.muxDc(
                            0 -> U(1, 2 bits),
                            1 -> U(0, 2 bits),
                            2 -> U(2, 2 bits)
                        )
    val paddr           = apx + apy * width
    val pbaddr          = pbytem + paddr * 3
    mem_raddr           := pbaddr(9 downto 0)
    mem_read            := False

    val curByte         = Reg(UInt(8 bits))
    val bit             = curByte(7 - pbit)
    

    val fsm             = new StateMachine {
        val readNextByte    = new State with EntryPoint
        val outputBitShape  = new State
        val bitComplete     = new State
        val byteComplete    = new State
        val pixelComplete   = new State
        val rowComplete     = new State
        val outputRst       = new State
 
        readNextByte.counting(  timer,  1,          outputBitShape                  ).whenIsActive(mem_read := True).onExit(curByte := mem_rdata)
        outputBitShape.counting(timer,  TBIT,       bitComplete                     ).onEntry(dout := True)
        bitComplete.counting(   pbit,   7,          byteComplete,   readNextByte    )
        byteComplete.counting(  pbyte,  2,          pixelComplete,  readNextByte    )
        pixelComplete.counting( px,     width - 1,  rowComplete,    readNextByte    )
        rowComplete.counting(   py,     height - 1, outputRst,      readNextByte    )
        outputRst.counting(     timer,  TRST,       readNextByte                    ).onEntry(dout := False)

        outputBitShape.whenIsActive {
            val t = Mux(bit, U(T1H), U(T0H))
            when(timer === t) { dout := False }
        }
    }
}