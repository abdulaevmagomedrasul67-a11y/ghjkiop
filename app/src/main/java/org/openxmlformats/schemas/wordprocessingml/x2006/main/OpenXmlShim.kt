package org.openxmlformats.schemas.wordprocessingml.x2006.main

import java.math.BigInteger

enum class STTabJc {
    LEFT, CENTER, RIGHT
}

enum class STTabTlc {
    NONE, DOT, HYPHEN, UNDERSCORE
}

class CTTabStop {
    var jc: STTabJc = STTabJc.LEFT
    var leader: STTabTlc = STTabTlc.NONE
    var pos: BigInteger = BigInteger.ZERO

    fun setVal(value: STTabJc) {
        this.jc = value
    }
}

class CTTabs {
    val tabList = mutableListOf<CTTabStop>()

    fun addNewTab(): CTTabStop {
        val tab = CTTabStop()
        tabList.add(tab)
        return tab
    }
}

class CTPPr {
    var tabs: CTTabs? = null

    fun addNewTabs(): CTTabs {
        val t = CTTabs()
        tabs = t
        return t
    }
}

class CTFonts {
    @JvmField var ascii: String? = null
    @JvmField var hAnsi: String? = null

    fun setAscii(font: String) {
        this.ascii = font
    }

    fun setHAnsi(font: String) {
        this.hAnsi = font
    }
}

class CTHpsMeasure {
    var value: BigInteger = BigInteger.ZERO

    fun setVal(v: BigInteger) {
        this.value = v
    }
}

class CTOnOff

class CTRPr {
    var rFonts: CTFonts? = null
    var sz: CTHpsMeasure? = null
    var b: CTOnOff? = null

    fun addNewRFonts(): CTFonts {
        val f = CTFonts()
        rFonts = f
        return f
    }

    fun addNewSz(): CTHpsMeasure {
        val s = CTHpsMeasure()
        sz = s
        return s
    }

    fun addNewB(): CTOnOff {
        val bold = CTOnOff()
        b = bold
        return bold
    }
}

class CTText {
    var stringValue: String = ""
}

class CTR {
    var rPr: CTRPr? = null
    var t: CTText? = null

    fun addNewRPr(): CTRPr {
        val pr = CTRPr()
        rPr = pr
        return pr
    }

    fun addNewT(): CTText {
        val text = CTText()
        t = text
        return text
    }
}

class CTHyperlink {
    var anchor: String = ""
    val runs = mutableListOf<CTR>()

    fun addNewR(): CTR {
        val r = CTR()
        runs.add(r)
        return r
    }
}

class CTP {
    var pPr: CTPPr? = null
    val hyperlinks = mutableListOf<CTHyperlink>()

    fun addNewPPr(): CTPPr {
        val pr = CTPPr()
        pPr = pr
        return pr
    }

    fun addNewHyperlink(): CTHyperlink {
        val h = CTHyperlink()
        hyperlinks.add(h)
        return h
    }
}
