package org.mobicents.smsc.library;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

/**
*
* @author sergey vetyutnev
* 
*/
public class RETest {
    @Test(groups = { "RegularExpr" })
    public void testRegularExpr() {
//        String expr = "^([1-9][1-9]|[0-9][1-9]|[1-9][0-9]).*";
        String expr = "^00[0-9].*";
        Pattern p = Pattern.compile(expr);
        Matcher m = p.matcher("0011");
        boolean b1 = m.matches();

        m = p.matcher("01");
        boolean b2 = m.matches();

        m = p.matcher("1");
        boolean b3 = m.matches();

        m = p.matcher("0");
        boolean b4 = m.matches();

        m = p.matcher("012222");
        boolean b5 = m.matches();

        m = p.matcher("1022222");
        boolean b6 = m.matches();

        m = p.matcher("11223232");
        boolean b7 = m.matches();

        m = p.matcher("100212");
        boolean b8 = m.matches();

        m = p.matcher("0000000");
        boolean b9 = m.matches();

        m = p.matcher("");
        boolean b0 = m.matches();
    }
}
