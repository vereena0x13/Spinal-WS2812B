import spinal.core._


case class UartBus() extends Bundle {
    val wdata = out(Bits(8 bits))
    val rdata = in(Bits(8 bits))
    val txe   = in(Bool())
    val rxf   = in(Bool())
    val wr    = out(Bool())
    val rd    = out(Bool())
}