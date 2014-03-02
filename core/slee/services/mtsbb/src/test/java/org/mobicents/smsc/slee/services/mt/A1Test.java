package org.mobicents.smsc.slee.services.mt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

public class A1Test {

    @Test(groups = { "Base" })
    public void testA1() {
        String s1 = "";
        String s2 = "1111";
        Pattern pattern = Pattern.compile(s1);
        Matcher m = pattern.matcher(s2);
        boolean res = m.matches();
    }

}
