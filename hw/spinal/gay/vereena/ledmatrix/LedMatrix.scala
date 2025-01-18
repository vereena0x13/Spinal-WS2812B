package gay.vereena.ledmatrix

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.fsm._


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
    val addr_width      = log2Up(pixels * bytes_per_pixel)
    val memory_size     = pixels * bytes_per_pixel

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

        val ram_raddr   = out(atype())
        val ram_read    = out(Bool())
        val ram_dout    = in(UInt(8 bits))
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


    // NOTE TODO: as should be the source of pixel data, ideally. what if
    //            someone bits to generate pixels on the fly w/o ram, etc.?
    val pbytem          = pbyte.muxDc(
                            0 -> U(1, 2 bits),
                            1 -> U(0, 2 bits),
                            2 -> U(2, 2 bits)
                        )
    val paddr           = apx + apy * width
    val pbaddr          = pbytem + paddr * 3
    ram_raddr           := pbaddr(9 downto 0)
    ram_read            := timer < 100 // NOTE: this condition is quite loose but it shouldn't matter

    val bit             = ram_dout(7 - pbit)
    

    val fsm             = new StateMachine {
        val outputBitShape  = new State with EntryPoint
        val bitComplete     = new State
        val byteComplete    = new State
        val pixelComplete   = new State
        val rowComplete     = new State
        val outputRst       = new State
 
        implicit class StateExt(val s: State) {
            def counting(ctr: UInt, lim: UInt, next: State, refrain: State = s) = s.whenIsActive {
                when(ctr === lim) {
                    ctr := 0
                    s.goto(next)
                } otherwise {
                    ctr := ctr + 1
                    if(s != refrain) s.goto(refrain)
                }
            }
        }
 
        outputBitShape.counting(timer,  TBIT,       bitComplete                     ).onEntry(dout := True)
        bitComplete.counting(   pbit,   7,          byteComplete,   outputBitShape  )
        byteComplete.counting(  pbyte,  2,          pixelComplete,  outputBitShape  )
        pixelComplete.counting( px,     width - 1,  rowComplete,    outputBitShape  )
        rowComplete.counting(   py,     height - 1, outputRst,      outputBitShape  )
        outputRst.counting(     timer,  TRST,       outputBitShape                  ).onEntry(dout := False)

        outputBitShape.whenIsActive {
            val t = Mux(bit, U(T1H), U(T0H))
            when(timer === t) { dout := False }
        }
    }
}