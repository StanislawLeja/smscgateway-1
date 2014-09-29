package org.mobicents.smsc.slee.services.mt;

import static org.testng.Assert.*;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.smsc.slee.resources.persistence.TT_PersistenceRAInterfaceProxy;
import org.testng.annotations.Test;

public class A1Test {

    @Test(groups = { "Base" })
    public void testA1() {
        String s1 = "^45[0-9a-zA-Z]*";
        String s21 = "45";
        String s22 = "45222";
        String s23 = "4";
        String s24 = "46888888";
        String s25 = "";
        Pattern pattern = Pattern.compile(s1);
        Matcher m = pattern.matcher(s21);
        boolean res = m.matches();
        m = pattern.matcher(s22);
        res = m.matches();
        m = pattern.matcher(s23);
        res = m.matches();
        m = pattern.matcher(s24);
        res = m.matches();
        m = pattern.matcher(s25);
        res = m.matches();


//        Map availcs = Charset.availableCharsets();
//        Set keys = availcs.keySet();
//        for (Iterator iter = keys.iterator(); iter.hasNext();) {
//            System.out.println(iter.next());
//        }

        Charset ascii = Charset.forName("ISO-8859-1");
        byte[] data1 = new byte[256];
        for (int i1 = 0; i1 < 256; i1++) {
            data1[i1] = (byte) i1;
        }
        String sxx = new String(data1, ascii);
        byte[] data2 = sxx.getBytes(ascii);
        assertEquals(data1, data2);
        assertEquals(sxx.length(), 256);



//        DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(8);
//        String s11 = "12345abcde";
//        String sx = "";
//        for (int i1 = 0; i1 < 15; i1++) {
//            sx += s11;
//        }
//        MtSbbProxy mt = new MtSbbProxy(new TT_PersistenceRAInterfaceProxy());
//        ArrayList<String> resx = mt.sliceMessage(sx, dataCodingScheme);
//        int i111 = 0;
    }

}
