package gay.vereena.ledmatrix.util

import scala.collection.mutable.ArrayBuffer
import scala.collection.Iterator
import scala.util.Using

import java.io.DataInputStream
import java.io.FileInputStream

import spinal.core._


object Util {
    def spinalConfig(): SpinalConfig = SpinalConfig(
        targetDirectory = "hw/gen",
        onlyStdLogicVectorAtTopLevelIo = true,
        mergeAsyncProcess = true,
        defaultConfigForClockDomains = ClockDomainConfig(
            resetKind = SYNC  
        ),
        defaultClockDomainFrequency = FixedFrequency(100 MHz),
        device = Device(
            vendor = "xilinx",
            family = "Artix 7"
        )
    )
}