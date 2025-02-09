package gay.vereena.ledmatrix.misc

import spinal.lib._
import spinal.core._


case class TIL311() extends Component {
    val io = new Bundle {
        val value           = in UInt(4 bits)

        val data            = out UInt(4 bits)
        val strobe          = out Bool()    
        val blank           = out Bool()  
    }
    import io._


    val valuePrev           = RegNext(value)

    data                    := value
    strobe                  := value =/= valuePrev
    blank                   := False
}