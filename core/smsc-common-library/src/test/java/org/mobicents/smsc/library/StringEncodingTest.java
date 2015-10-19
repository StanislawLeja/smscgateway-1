package org.mobicents.smsc.library;

import java.nio.charset.Charset;

import org.testng.annotations.Test;

public class StringEncodingTest {

    @Test(groups = { "StringEncoding" })
    public void testRegularExpr() {
        byte[] buf = new byte[] { (byte) 0xc3, (byte) 0x84, 0x20, 0x65, 0x20, 0x69, 0x20, (byte) 0xc3, (byte) 0xb6, 0x20,
                (byte) 0xc3, (byte) 0xbc };
        Charset utf8Charset = Charset.forName("UTF-8");
        String s = new String(buf, utf8Charset);
        System.out.println(s);
    }
}
