package org.mobicents.smsc.slee.services.mt;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.LMSI;
import org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.protocols.ss7.map.primitives.IMSIImpl;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.protocols.ss7.map.primitives.LMSIImpl;
import org.mobicents.protocols.ss7.map.service.sms.LocationInfoWithLMSIImpl;
import org.mobicents.protocols.ss7.map.service.sms.SendRoutingInfoForSMResponseImpl;
import org.testng.annotations.Test;

public class XXXTest {


	@Test
	public void ATest() {

		try {
			ByteArrayOutputStream fos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(fos);

			X1 x1 = new X1();
			IMSI imsi = new IMSIImpl("11223344");
			ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "111222333");
			LMSI lmsi = new LMSIImpl(new byte[] { 1, 2, 3, 4 });
			LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, lmsi, null, null, null);
			x1.resp = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, true);

			oos.writeObject(x1);
			oos.flush();
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
			int i1 = 0;
			i1++;
		}
	}
	
}
