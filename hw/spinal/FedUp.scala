import scala.collection.mutable.ArrayBuffer
import scala.math

import spinal.lib._
import spinal.core._
import spinal.core.sim._

import Util._

import java.io.File
import javax.imageio.ImageIO



object FedUp {
    val WIDTH               = 16
    val HEIGHT              = 16
    val PIXELS              = WIDTH * HEIGHT
    val BYTES_PER_PIXEL     = 3
    val ADDR_WIDTH          = log2Up(PIXELS * BYTES_PER_PIXEL)

    def atype()                     = UInt(ADDR_WIDTH bits)

    private def createRAM() = {
        val ramSize = WIDTH * HEIGHT * 3
        val img = ImageIO.read(new File("v.jpg"))
        val init = new Array[UInt](ramSize)
        for(y <- 0 until HEIGHT) {
            for(x <- 0 until WIDTH) {
                val p = img.getRGB(x, y)
                val i = (x + y * WIDTH) * 3
                val r = (p & 0xFF0000) >> 16
                val g = (p & 0x00FF00) >> 8
                val b = p & 0x0000FF
                init(i + 0) = U(r >> 5, 8 bits)
                init(i + 1) = U(g >> 5, 8 bits)
                init(i + 2) = U(b >> 5, 8 bits)
            }
        }
        Mem(UInt(8 bits), init)
    }
}


case class UartHandler() extends Component {
    import FedUp._

    val io = new Bundle {
        val uart        = UartBus()
        val ram_waddr   = out(atype())
        val ram_din     = out(UInt(8 bits))
        val ram_write   = out(Bool())
    }
    import io._


    val r_urd       = Reg(Bool()) init(False)
    val r_uwr       = Reg(Bool()) init(False)
    val r_uout      = Reg(UInt(8 bits)) init(0)

    uart.rd         := r_urd
    uart.wr         := r_uwr
    uart.wdata      := r_uout.asBits


    val r_waddr     = Reg(UInt(ADDR_WIDTH bits)) init(0)
    val r_wdata     = Reg(UInt(8 bits)) init(0)
    val r_write     = Reg(Bool()) init(False)

    ram_waddr       := r_waddr
    ram_din         := r_wdata
    ram_write       := r_write


    val buffer      = Vec.fill(5)(Reg(UInt(8 bits)))
    val count       = Reg(UInt(3 bits)) init(0)

    
    val stateBits   = 8
    val state       = Reg(UInt(stateBits bits)) init(0)
    val stateNext   = UInt(stateBits bits) //Reg(UInt(stateBits bits)) init(0) 
    val advance     = False

    stateNext := state + 1

    var stateID = 0
    def nextStateID(): Int = {
        var id = stateID
        stateID += 1
        return id
    }

    val ibuf = Reg(UInt(8 bits)) init(0)

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


        nextState {
            when(!uart.rxf) {
                r_urd := True
                next()
            }
        }
        delayState()
        delayState()
        nextState {
            ibuf := uart.rdata.asUInt
            next()
        }
        delayState()
        delayState()
        nextState {
            r_urd := False
            next()
        }
        nextState {
            when(!uart.txe) {
                r_uout := ibuf
                next()
            }
        }
        nextState {
            r_uwr := True
            next()
        }
        nextState {
            r_uwr := False
            //ibuf := 0
            next()
        }

        /*
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
                    is(i) {
                        buffer(i) := uart.rdata.asUInt
                        r_uout := uart.rdata.asUInt
                    }
                }
            }
            next()
        }
        nextState {
            r_urd   := False
            when(!uart.txe) {
                r_uwr   := True
                count   := count + 1
                next()
            }
        }
        delayState()
        nextState {
            r_uwr := False
            when(count === buffer.length) {
                count := 0
                next()
            } otherwise {
                goto(recv)
            }
        }
        val write = nextState {
            val addr = buffer(0) + buffer(1) * WIDTH + count
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
        */
    }

    when(advance) {
        when(stateNext === stateID) {
            state := 0
        } otherwise {
            state := stateNext
        }
    }
}


case class FedUp() extends Component {
    import FedUp._

    val io = new Bundle {
        val uart = UartBus()
        val gpio_a13 = out(Bool())
    }
    import io._



    // TODO: double buffer

    val ram         = createRAM() //Mem(UInt(8 bits), WIDTH * HEIGHT & BYTES_PER_PIXEL)

    val ram_waddr   = atype()
    val ram_din     = UInt(8 bits)
    val ram_write   = Bool()

    val ram_raddr   = atype()
    val ram_read    = Bool()

    ram.write(
        enable      = ram_write,
        address     = ram_waddr,
        data        = ram_din
    )

    val ram_dout = ram.readSync(
        address     = ram_raddr,
        enable      = ram_read
    )


    val uartHandler = UartHandler()
    uartHandler.io.uart <> uart
    ram_waddr := uartHandler.io.ram_waddr
    ram_din := uartHandler.io.ram_din
    ram_write := uartHandler.io.ram_write


    val dout        = Reg(Bool()) init(True)
    gpio_a13        := dout

    val timer       = Reg(UInt(32 bits)) init(0)
    val prst        = Reg(Bool()) init(False)
    val px          = Reg(UInt(8 bits)) init(0)
    val py          = Reg(UInt(8 bits)) init(0)
    val pbit        = Reg(UInt(5 bits)) init(0)

    val pg          = pbit < 8
    val pr          = pbit >= 8 && pbit < 16
    val pb          = pbit >= 16

    val apx         = UInt(8 bits)
    when(py(0)) {
        apx         := WIDTH - 1 - px
    } otherwise {
        apx         := px
    }
    val apy         = HEIGHT - 1 - py


    ram_raddr       := 0
    ram_read        := !prst && timer < 10 /*100*/

    val want        = False
    val bit         = 7 - pbit(2 downto 0)
    when(pr) {
        val idx     = (apx + apy * WIDTH) * 3
        ram_raddr   := idx(9 downto 0)
        want        := ram_dout(bit)
    } elsewhen(pg) {
        val idx     = ((apx + apy * WIDTH) * 3) + 1
        ram_raddr   := idx(9 downto 0)
        want        := ram_dout(bit)
    } elsewhen(pb) {
        val idx     = ((apx + apy * WIDTH) * 3) + 2
        ram_raddr   := idx(9 downto 0)
        want        := ram_dout(bit)
    }
    

    when(prst) {
        when(timer === 3999/*39999*/) {
            timer := 0
            prst := False
            dout := True
        } otherwise {
            timer := timer + 1
        }
    } otherwise {
        when(timer ===  13 /*124*/) {
            timer := 0
            
            when(pbit === 3 /*23*/) {
                pbit := 0

                when(px === WIDTH - 1) {
                    px := 0
                    when(py === HEIGHT - 1) {
                        py := 0
                        prst := True
                    } otherwise {
                        py := py + 1
                        dout := True
                    }
                } otherwise {
                    px := px + 1
                    dout := True
                }
            } otherwise {
                pbit := pbit + 1
                dout := True
            }
        } otherwise {
            timer := timer + 1

            when(want && timer === 8 /*84*/) {
                dout := False
            } elsewhen(!want && timer === 4 /*44*/) {
                dout := False
            }
        }
    }
}



object GenerateSOC extends App {
    spinalConfig().generateVerilog(FedUp())
}


object SimulateSOC extends App {
    SimConfig
        .withWave
        .withConfig(spinalConfig())
        .compile {
            val soc = UartHandler()
            
            soc.io.uart.rdata.simPublic()
            soc.io.uart.rxf.simPublic()
            soc.io.uart.rd.simPublic()
            soc.io.ram_din.simPublic()
            soc.io.ram_waddr.simPublic()
            soc.io.ram_write.simPublic()
            
            soc
        }
        .doSim { soc =>
            import soc.io._

            uart.rdata #= 0
            uart.rxf   #= true

            
            val clk = soc.clockDomain

            clk.fallingEdge()
            sleep(0)

            clk.assertReset()
            for(_ <- 0 until 10) {
                clk.clockToggle()
                sleep(1)
            }
            clk.deassertReset()
            sleep(1)


            def tick() = {
                clk.clockToggle()
                sleep(1)
                clk.clockToggle()
                sleep(1)
            }
            
            def ticks(n: Int) = {
                for(i <- 0 until n) { tick() }
            }


            for(i <- 0 until 20) { tick() }


            val xs = Array(12, 7, 37, 14, 9)
            for(x <- xs) {
                uart.rxf #= false

                while(!uart.rd.toBoolean) { tick() }
                uart.rxf #= true
                uart.rdata #= x
                while(uart.rd.toBoolean) { tick() }
                uart.rdata #= 0

                ticks(10)
            }
            

            for(i <- 0 until 20) { tick() }
        }
}


/*
object SimulateSOC extends App {
    SimConfig
        .withWave
        .withConfig(spinalConfig())
        .compile {
            val soc = FedUp()
            
            soc.io.uart.rdata.simPublic()
            soc.io.uart.txe.simPublic()
            soc.io.uart.rxf.simPublic()
            soc.io.gpio_a13.simPublic()
            
            soc
        }
        .doSim { soc =>
            soc.io.uart.rdata #= 0
            soc.io.uart.txe   #= false
            soc.io.uart.rxf   #= true

            
            val clk = soc.clockDomain

            clk.fallingEdge()
            sleep(0)

            clk.assertReset()
            for(_ <- 0 until 10) {
                clk.clockToggle()
                sleep(1)
            }
            clk.deassertReset()
            sleep(1)


            for(i <- 0 until 850000) {
                clk.clockToggle()
                sleep(1)
                clk.clockToggle()
                sleep(1)
            }
        }
}
*/