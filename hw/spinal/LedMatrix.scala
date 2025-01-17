import spinal.lib._
import spinal.core._
import spinal.core.sim._


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

    def atype()         = UInt(addr_width bits)
}

case class LedMatrix(cfg: LedMatrixConfig) extends Component {
    import cfg._

    val io = new Bundle {
        val dout        = out(Bool())

        val ram_raddr   = out(atype())
        val ram_read    = out(Bool())
        val ram_dout    = in(UInt(8 bits))
    }
    import io._

    
    // TODO: don't constantly refresh at max speed; only on change
    val r_dout          = Reg(Bool()) init(True)
    dout                := r_dout

    val timer           = Reg(UInt(32 bits)) init(0)
    val prst            = Reg(Bool()) init(False)
    val px              = Reg(UInt(8 bits)) init(0)
    val py              = Reg(UInt(8 bits)) init(0)
    val pbit            = Reg(UInt(5 bits)) init(0)

    // TODO: this is stupid; just add another counter lol
    //       rather than having to check a condition for
    //       each 3 bytes of a pixel and handle them separately.
    val pg              = pbit < 8
    val pr              = pbit >= 8 && pbit < 16
    val pb              = pbit >= 16

    // NOTE TODO: calculation of apx and apy should eventually be 
    // configuratble, once we factor this stuff into its own Component
    val apx             = Mux(py(0), width - 1 - px, px)
    val apy             = height - 1 - py


    // NOTE: as should be the source of pixel data, ideally. what if
    //       someone wants to generate pixels on the fly w/o ram, etc.?
    ram_raddr           := 0
    ram_read            := !prst && timer < 100

    val want            = False
    val bit             = 7 - pbit(2 downto 0)
    when(pr) {
        val idx         = (apx + apy * width) * 3
        ram_raddr       := idx(9 downto 0)
        want            := ram_dout(bit)
    } elsewhen(pg) {
        val idx         = ((apx + apy * width) * 3) + 1
        ram_raddr       := idx(9 downto 0)
        want            := ram_dout(bit)
    } elsewhen(pb) {
        val idx         = ((apx + apy * width) * 3) + 2
        ram_raddr       := idx(9 downto 0)
        want            := ram_dout(bit)
    }
    

    // TODO: clean this up
    when(prst) {
        when(timer === 39999) {
            timer := 0
            prst := False
            r_dout := True
        } otherwise {
            timer := timer + 1
        }
    } otherwise {
        when(timer === 124) {
            timer := 0
            
            when(pbit === 23) {
                pbit := 0

                when(px === width - 1) {
                    px := 0
                    when(py === height - 1) {
                        py := 0
                        prst := True
                    } otherwise {
                        py := py + 1
                        r_dout := True
                    }
                } otherwise {
                    px := px + 1
                    r_dout := True
                }
            } otherwise {
                pbit := pbit + 1
                r_dout := True
            }
        } otherwise {
            timer := timer + 1

            when(want && timer === 84) {
                r_dout := False
            } elsewhen(!want && timer === 44) {
                r_dout := False
            }
        }
    }
}