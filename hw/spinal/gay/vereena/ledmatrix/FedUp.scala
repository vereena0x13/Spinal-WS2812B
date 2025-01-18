package gay.vereena.ledmatrix

import spinal.lib._
import spinal.core._
import spinal.core.sim._

import Util._



object FedUp {
    private def createRAM(size: Int, initialRamData: Option[Seq[Int]]) = {
        val init = initialRamData match {
            case Some(data) => {
                require(size == data.size, s"${size} != ${data.size}")
                data.map(U(_))
            }
            case None => Seq.fill(size)(U(0, 8 bits))
        }
        Mem(UInt(8 bits), init)
    }
}


case class FedUp(initialRamData: Option[Seq[Int]]) extends Component {
    import FedUp._

    val io = new Bundle {
        val uart        = UartBus()
        val gpio_a13    = out(Bool())
    }
    import io._


    val matrixCfg = LedMatrixConfig(
        width           = 16,
        height          = 16,
    )


    // TODO: double buffer?
    val ram             = createRAM(matrixCfg.memory_size, initialRamData)

    val ram_waddr       = matrixCfg.atype()
    val ram_din         = UInt(8 bits)
    val ram_write       = Bool()

    val ram_raddr       = matrixCfg.atype()
    val ram_read        = Bool()

    ram.write(
        enable          = ram_write,
        address         = ram_waddr,
        data            = ram_din
    )

    val ram_dout     = ram.readSync(
        address         = ram_raddr,
        enable          = ram_read
    )


    val uartHandler     = UartHandler(matrixCfg)
    uartHandler.io.uart <> uart
    ram_waddr           := uartHandler.io.ram_waddr
    ram_din             := uartHandler.io.ram_din
    ram_write           := uartHandler.io.ram_write



    val matrix          = LedMatrix(matrixCfg)
    gpio_a13            := matrix.io.dout
    ram_raddr           := matrix.io.ram_raddr
    ram_read            := matrix.io.ram_read
    matrix.io.ram_dout  := ram_dout
}