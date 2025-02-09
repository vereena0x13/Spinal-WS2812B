package gay.vereena.ledmatrix

import spinal.lib._
import spinal.core._


case class RotaryEncoder() extends Component {
    val io = new Bundle {
        val enc_a           = in Bool()
        val enc_b           = in Bool()

        val count_cw        = out Bool()
        val count_ccw       = out Bool()
    }
    import io._


    val a_sync              = BufferCC(enc_a, False)
    val b_sync              = BufferCC(enc_b, False)


    val dba                 = Debouncer(true)
    dba.io.din              := a_sync
    val a_db                = dba.io.dout

    val dbb                 = Debouncer(true)
    dbb.io.din              := b_sync
    val b_db                = dbb.io.dout


    val marker              = !(a_db | b_db)
    val markerPrev          = RegNext(marker)

    count_cw                := markerPrev & b_db
    count_ccw               := markerPrev & a_db
}