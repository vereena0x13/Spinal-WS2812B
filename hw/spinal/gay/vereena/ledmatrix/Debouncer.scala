package gay.vereena.ledmatrix

import spinal.lib._
import spinal.core._


object Debouncer {
    val DEFAULT_CYCLES      = 500_000 // 500_000 * 10ns = 5ms
}

case class Debouncer(val idle: Boolean, val cycles: Int = Debouncer.DEFAULT_CYCLES) extends Component {
    val io = new Bundle {
        val din             = in Bool()
        val dout            = out Bool() 
    }
    import io._


    dout.setAsReg() init(idle)

    
    val timeout             = Reg(UInt(log2Up(cycles) bits)) init(0)
    val dinPrev             = RegNext(din) init(idle)

    when(din =/= dinPrev) {
        timeout             := cycles - 1
    } otherwise {
        when(timeout > 0) {
            timeout         := timeout - 1
        } otherwise {
            dout            := din
        }
    }
}