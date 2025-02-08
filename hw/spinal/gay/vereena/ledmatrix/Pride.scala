package gay.vereena.ledmatrix

import spinal.core._


object Pride {
    private val GAMMA = Array(
        0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
        0,   0,   0,   0,   0,   0,   0,   0,   0,   1,   1,   1,   1,   1,   1,
        1,   1,   1,   1,   1,   1,   2,   2,   2,   2,   2,   2,   2,   2,   3,
        3,   3,   3,   3,   3,   4,   4,   4,   4,   5,   5,   5,   5,   5,   6,
        6,   6,   6,   7,   7,   7,   8,   8,   8,   9,   9,   9,   10,  10,  10,
        11,  11,  11,  12,  12,  13,  13,  13,  14,  14,  15,  15,  16,  16,  17,
        17,  18,  18,  19,  19,  20,  20,  21,  21,  22,  22,  23,  24,  24,  25,
        25,  26,  27,  27,  28,  29,  29,  30,  31,  31,  32,  33,  34,  34,  35,
        36,  37,  38,  38,  39,  40,  41,  42,  42,  43,  44,  45,  46,  47,  48,
        49,  50,  51,  52,  53,  54,  55,  56,  57,  58,  59,  60,  61,  62,  63,
        64,  65,  66,  68,  69,  70,  71,  72,  73,  75,  76,  77,  78,  80,  81,
        82,  84,  85,  86,  88,  89,  90,  92,  93,  94,  96,  97,  99,  100, 102,
        103, 105, 106, 108, 109, 111, 112, 114, 115, 117, 119, 120, 122, 124, 125,
        127, 129, 130, 132, 134, 136, 137, 139, 141, 143, 145, 146, 148, 150, 152,
        154, 156, 158, 160, 162, 164, 166, 168, 170, 172, 174, 176, 178, 180, 182,
        184, 186, 188, 191, 193, 195, 197, 199, 202, 204, 206, 209, 211, 213, 215,
        218, 220, 223, 225, 227, 230, 232, 235, 237, 240, 242, 245, 247, 250, 252,
        255
    )

    private val BRIGHTNESS          = 140
    private val FLAG_COLOR_WIDTH    = 16

    
    private def fixupColor(x: Int): Int = {
        val xd = x.toDouble / 255.0d
        val y = (xd * BRIGHTNESS).toInt
        GAMMA(y)
    }

    private def hexColor(c: Int) = Seq(
        fixupColor((c & 0xFF0000) >> 16),
        fixupColor((c & 0x00FF00) >> 8 ),
        fixupColor((c & 0x0000FF)      ),
    ).map(U(_, 8 bits))


    def flag(w: Option[Int], cs: Int*): Seq[UInt] = w match {
        case None       => cs.reverse.map(hexColor).flatten
        case Some(i)    => cs.reverse.flatMap(c => Seq.fill(i)(hexColor(c)).flatten)
    }

    def flag(cs: Int*): Seq[UInt] = flag(Some(FLAG_COLOR_WIDTH), cs: _*)


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
    )
}