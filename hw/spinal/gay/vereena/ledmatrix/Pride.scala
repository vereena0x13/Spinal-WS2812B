package gay.vereena.ledmatrix

import spinal.core._


object Pride {
    def hexColor(c: Int) = Seq(
        U((c & 0xFF0000) >> 16, 8 bits),
        U((c & 0x00FF00) >> 8,  8 bits),
        U((c & 0x0000FF),       8 bits),
    )


    def flag(w: Option[Int], cs: Int*): Seq[UInt] = w match {
        case None       => cs.map(hexColor).flatten
        case Some(i)    => cs.flatMap(Seq.fill(i)(_))
    }

    def flag(cs: Int*): Seq[UInt] = flag(None, cs: _*)


    val transFlag = flag(
        0x5BCEFA,
        0xF5A9B8,
        0xFFFFFF,
        0xF5A9B8,
        0x5BCEFA,
    )

    val lesbianFlag = flag(
        0xD62900,
        0xFF9B55,
        0xFFFFFF,
        0xD461A6,
        0xA50062,
    )

    val genderfluidFlag = flag(
        0xFE76A2,
        0xFFFFFF,
        0xBF12D7,
        0x000000,
        0x303CBE
    )


    val prideSeq = Seq(
        genderfluidFlag,
        //transFlag,
        //lesbianFlag,
    )
}