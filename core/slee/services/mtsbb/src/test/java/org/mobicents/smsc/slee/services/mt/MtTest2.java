package org.mobicents.smsc.slee.services.mt;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Timestamp;

import org.mobicents.protocols.ss7.map.api.smstpdu.AbsoluteTimeStamp;
import org.mobicents.protocols.ss7.map.api.smstpdu.AddressField;
import org.mobicents.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.mobicents.protocols.ss7.map.api.smstpdu.ProtocolIdentifier;
import org.mobicents.protocols.ss7.map.api.smstpdu.TypeOfNumber;
import org.mobicents.protocols.ss7.map.service.sms.SmsSignalInfoImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AbsoluteTimeStampImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AddressFieldImpl;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.protocols.ss7.map.smstpdu.ProtocolIdentifierImpl;
import org.mobicents.protocols.ss7.map.smstpdu.SmsDeliverTpduImpl;
import org.mobicents.protocols.ss7.map.smstpdu.UserDataImpl;
import org.testng.annotations.Test;

public class MtTest2 {

    @Test(groups = { "Mt" })
    public void Ucs2Test() throws Exception {
        
        String s11 = "زمانیکه بررسی";
        Charset ucs2Charset = Charset.forName("UTF-16BE");
        Charset utf8 = Charset.forName("UTF-8");
//        ByteBuffer bb = ByteBuffer.wrap(textPart);
//        CharBuffer bf = ucs2Charset.decode(bb);
//        msg = bf.toString();
        ByteBuffer bb = utf8.encode(s11);
        byte[] buf = new byte[bb.limit()];
        bb.get(buf, 0, bb.limit());
        String s2 = new String(buf);

        UserDataImpl ud = new UserDataImpl(s2, new DataCodingSchemeImpl(8), null, null);
        AddressField originatingAddress = new AddressFieldImpl(TypeOfNumber.InternationalNumber, NumberingPlanIdentification.ISDNTelephoneNumberingPlan,
                "123456");
        ProtocolIdentifier pi = new ProtocolIdentifierImpl(0);
        AbsoluteTimeStamp serviceCentreTimeStamp = new AbsoluteTimeStampImpl(05, 3, 4, 5, 6, 7, 0);
        SmsDeliverTpduImpl smsDeliverTpduImpl = new SmsDeliverTpduImpl(false, false, false, true, originatingAddress, pi, serviceCentreTimeStamp, ud);

        SmsSignalInfoImpl SmsSignalInfoImpl = new SmsSignalInfoImpl(smsDeliverTpduImpl, null);

        int gg=0;
        gg++;
    }
}
