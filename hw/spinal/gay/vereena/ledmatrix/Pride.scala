package gay.vereena.ledmatrix


object Pride {
    private val MAX_BRIGHTNESS      = 120
    private val FLAG_COLOR_WIDTH    = 16

    
    private def fixupColor(x: Int): Int = {
        val xd = x.toDouble / 255.0d
        (xd * MAX_BRIGHTNESS).toInt
    }

    private def hexColor(c: Int) = Seq(
        fixupColor((c & 0xFF0000) >> 16),
        fixupColor((c & 0x00FF00) >> 8 ),
        fixupColor((c & 0x0000FF)      ),
    )


    def flag(w: Option[Int], cs: Int*): Seq[Int] = w match {
        case None       => cs.reverse.map(hexColor).flatten
        case Some(i)    => cs.reverse.flatMap(c => Seq.fill(i)(hexColor(c)).flatten)
    }

    def flag(cs: Int*): Seq[Int] = flag(Some(FLAG_COLOR_WIDTH), cs: _*)


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
        //genderfluidFlag,
        transFlag,
        lesbianFlag,
    ).flatten
}