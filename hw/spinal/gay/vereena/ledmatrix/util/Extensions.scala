package gay.vereena.ledmatrix.util

import spinal.core._
import spinal.lib.fsm._


object FSMExtensions {
    implicit class StateExt(val s: State) {
        def counting(ctr: UInt, lim: UInt, next: State, refrain: State = s, cond: Option[Bool] = None) = s.whenIsActive {
            cond match {
                case None       => _counting(ctr, lim, next, refrain)
                case Some(c)    => when(c) { _counting(ctr, lim, next, refrain) }
            }
        }

        private def _counting(ctr: UInt, lim: UInt, next: State, refrain: State) = {
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