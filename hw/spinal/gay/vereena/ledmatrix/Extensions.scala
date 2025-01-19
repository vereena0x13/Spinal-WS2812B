package gay.vereena.ledmatrix

import spinal.core._
import spinal.lib.fsm._

object FSMExtensions {
    // TODO: why didn't i use a StateFsm for this? :sob: try doing that :P
    implicit class StateExt(val s: State) {
        def counting(ctr: UInt, lim: UInt, next: State, refrain: State = s) = s.whenIsActive {
            when(ctr === lim) {
                ctr := 0
                s.goto(next)
            } otherwise {
                ctr := ctr + 1
                if(s != refrain) s.goto(refrain)
            }
        }
    }
}