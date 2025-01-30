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
        tile_width      = 16,
        tile_height     = 16,
        tiles_x         = 2,
        tiles_y         = 1
    )
    println(matrixCfg)


    // TODO: double buffer?
    val ram             = createRAM(matrixCfg.memory_size, initialRamData)

    val ram_waddr       = matrixCfg.atype()
    val mem_wdata       = UInt(8 bits)
    val ram_write       = Bool()

    val ram_raddr       = matrixCfg.atype()
    val ram_read        = Bool()

    ram.write(
        enable          = ram_write,
        address         = ram_waddr,
        data            = mem_wdata
    )

    val ram_rdata       = ram.readSync(
        address         = ram_raddr,
        enable          = ram_read
    )


    val uartHandler     = UartHandler(matrixCfg)
    uartHandler.io.uart <> uart
    ram_waddr           := uartHandler.io.mem_waddr
    mem_wdata           := uartHandler.io.mem_wdata
    ram_write           := uartHandler.io.mem_write


    val matrix          = LedMatrix(matrixCfg)
    gpio_a13            := matrix.io.dout
    ram_raddr           := matrix.io.mem_raddr
    ram_read            := matrix.io.mem_read
    matrix.io.mem_rdata := ram_rdata
}