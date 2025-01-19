package gay.vereena.ledmatrix

import spinal.core._


case class Pixel() extends Bundle {
    val r = UInt(8 bits)
    val g = UInt(8 bits)
    val b = UInt(8 bits)
}