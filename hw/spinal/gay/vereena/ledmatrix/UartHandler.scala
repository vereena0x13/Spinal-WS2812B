package gay.vereena.ledmatrix

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._

import FSMExtensions._


// TODO: eventually we'd like this to be far more general;
//       we'd like to support a more proper packetized protocol,
//       supporting different packets with different data payloads,
//       as well as responrdatag to said packets.
//       furthermore, it should not be the uart handling code's
//       responsibility to handle writing data into RAM...
case class UartHandler(cfg: LedMatrixConfig) extends Component {
    import cfg._

    val io = new Bundle {
        val uart                = UartBus()

        val mem_waddr           = out(atype())
        val mem_wdata           = out(UInt(8 bits))
        val mem_write           = out(Bool())
    }
    import io._


    uart.rd.setAsReg() init(False)
    uart.wr                     := False
    uart.wdata                  := 0

    mem_waddr.setAsReg() init(0)
    mem_wdata.setAsReg() init(0)
    mem_write.setAsReg() init(False)
    
    val buffer_size             = 5
    val buffer                  = Vec.fill(buffer_size)(Reg(UInt(8 bits)))
    val index                   = Reg(UInt(log2Up(buffer_size) bits)) init(0)


    val fsm                     = new StateMachine {
        val waitByte            = new State with EntryPoint
        val recvDelay           = new State
        val recvByte            = new State
        val waitRxf             = new State

        val writeByte           = new State
        val writeLoop           = new State

        waitByte.whenIsActive {
            when(!uart.rxf) {
                uart.rd         := True
                goto(recvDelay)
            }
        }

        recvDelay.whenIsActive(goto(recvByte))

        recvByte.whenIsActive {
            buffer(index)       := uart.rdata.asUInt
            uart.rd             := False
            goto(waitRxf)
        }

        waitRxf.counting(index, buffer_size-1, writeByte, waitByte, cond = Some(uart.rxf))
            
        writeByte.whenIsActive {
            val addr            = ((buffer(0) + buffer(1) * matrixWidth) * 3) + index
            mem_waddr           := addr.resized
            mem_wdata           := buffer(index + 2)
            mem_write           := True
            goto(writeLoop)
        }

        writeLoop.counting(index, 2, waitByte, writeByte).whenIsActive(mem_write := False)
    }
}