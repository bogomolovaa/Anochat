package bogomolov.aa.anochat

import org.junit.Test

class TempTest {

    fun insertLinks(text: String) =
        text.replace("(https|http)://[^ ]+".toRegex(), """<a href="$0">$0</a>""")

    @Test
    fun testHtml(){
       val string = "lashdfsdklfj https://mail.ru vjsodfcxvxcv"
       //println(insertLinks(string))
        assert(string.contains("(https|http)".toRegex()))
    }
}