import spinal.lib._
import spinal.core._
import spinal.core.sim._

import Util._



// TODO: eventually we'd like this to be far more general;
//       we'd like to support a more proper packetized protocol,
//       supporting different packets with different data payloads,
//       as well as responding to said packets.
//       furthermore, it should not be the uart handling code's
//       responsibility to handle writing data into RAM...
case class UartHandler(cfg: LedMatrixConfig) extends Component {
    import FedUp._

    val io = new Bundle {
        val uart        = UartBus()
        val ram_waddr   = out(cfg.atype())
        val ram_din     = out(UInt(8 bits))
        val ram_write   = out(Bool())
    }
    import io._


    val r_urd           = Reg(Bool()) init(False)

    uart.rd             := r_urd
    uart.wr             := False
    uart.wdata          := 0


    val r_waddr         = Reg(UInt(cfg.addr_width bits)) init(0)
    val r_wdata         = Reg(UInt(8 bits)) init(0)
    val r_write         = Reg(Bool()) init(False)

    ram_waddr           := r_waddr
    ram_din             := r_wdata
    ram_write           := r_write


    // TODO: probably change the `buffer situation`; i feel that
    //       generating switch statements like we are now is silly
    //       and we should just do this as some form of dynamic index.
    val buffer          = Vec.fill(5)(Reg(UInt(8 bits)))
    val count           = Reg(UInt(3 bits)) init(0)

    
    val stateBits       = 8 // NOTE: it'd be nice if we knew this a priori
    val state           = Reg(UInt(stateBits bits)) init(0)
    val stateNext       = UInt(stateBits bits)
    val advance         = False

    stateNext           := state + 1

    var stateID = 0
    def nextStateID(): Int = {
        var id = stateID
        stateID += 1
        return id
    }

    switch(state) {
        def nextState(body: => Unit) = {
            val id = nextStateID()
            is(id)(body)
            id
        }

        def goto(id: Any) = {
            id match {
                case i: Int => { stateNext := U(i, stateBits bits) }
                case ui: UInt => { stateNext := ui }
                case _ => throw new IllegalArgumentException()
            }
            advance := True
        }
        def next() = { advance := True }
        def delayState() = nextState { next() }


        val recv = nextState {
            when(!uart.rxf) {
                r_urd := True
                next()
            }
        }
        delayState()
        nextState {
            switch(count) {
                for(i <- 0 until buffer.length) {
                    is(i) { buffer(i) := uart.rdata.asUInt }
                }
            }
            next()
        }
        delayState()
        nextState {
            r_urd := False
            count := count + 1
            next()
        }
        nextState {
            when(uart.rxf) { next() }
        }
        nextState {
            when(count === buffer.length) {
                count := 0
                next()
            } otherwise {
                goto(recv)
            }
        }

        delayState()

        val write = nextState {
            val addr = ((buffer(0) + buffer(1) * cfg.width) * 3) + count
            r_waddr := addr.resized
            switch(count) {
                for(i <- 0 until 3) {
                    is(i) { r_wdata := buffer(i + 2) }
                }
            }
            r_write := True
            next()
        }
        nextState {
            count := count + 1
            next()
        }
        nextState {
            r_write := False
            r_waddr := 0 // NOTE: technically not needed (right?)
            r_wdata := 0 // NOTE: technically not needed (right?)
            when(count === 3) {
                count := 0
                next()
            } otherwise {
                goto(write)
            }
        }
    }

    when(advance) {
        when(stateNext === stateID) {
            state := 0
        } otherwise {
            state := stateNext
        }
    }
}