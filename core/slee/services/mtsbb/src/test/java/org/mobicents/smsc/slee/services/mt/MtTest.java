/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.smsc.slee.services.mt;

import static org.testng.Assert.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.mobicents.protocols.ss7.map.api.smstpdu.ConcatenatedShortMessagesIdentifier;
import org.mobicents.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsDeliverTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsTpduType;
import org.mobicents.protocols.ss7.map.api.smstpdu.TypeOfNumber;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserData;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.mobicents.protocols.ss7.map.primitives.IMSIImpl;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.protocols.ss7.map.service.sms.ForwardShortMessageRequestImpl;
import org.mobicents.protocols.ss7.map.service.sms.ForwardShortMessageResponseImpl;
import org.mobicents.protocols.ss7.map.service.sms.InformServiceCentreRequestImpl;
import org.mobicents.protocols.ss7.map.service.sms.LocationInfoWithLMSIImpl;
import org.mobicents.protocols.ss7.map.service.sms.MWStatusImpl;
import org.mobicents.protocols.ss7.map.service.sms.MtForwardShortMessageRequestImpl;
import org.mobicents.protocols.ss7.map.service.sms.MtForwardShortMessageResponseImpl;
import org.mobicents.protocols.ss7.map.service.sms.ReportSMDeliveryStatusRequestImpl;
import org.mobicents.protocols.ss7.map.service.sms.ReportSMDeliveryStatusResponseImpl;
import org.mobicents.protocols.ss7.map.service.sms.SendRoutingInfoForSMResponseImpl;
import org.mobicents.protocols.ss7.sccp.parameter.GT0100;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextNameImpl;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.smsc.slee.services.persistence.CassandraPersistenceSbbProxy;
import org.mobicents.smsc.slee.services.persistence.MAPDialogSmsProxy;
import org.mobicents.smsc.slee.services.persistence.MAPDialogSmsProxy.MAPTestEvent;
import org.mobicents.smsc.slee.services.persistence.MAPDialogSmsProxy.MAPTestEventType;
import org.mobicents.smsc.slee.services.persistence.MAPServiceSmsProxy;
import org.mobicents.smsc.slee.services.persistence.PersistenceException;
import org.mobicents.smsc.slee.services.persistence.Sms;
import org.mobicents.smsc.slee.services.persistence.SmsProxy;
import org.mobicents.smsc.slee.services.persistence.SmsSet;
import org.mobicents.smsc.slee.services.persistence.TargetAddress;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsSetEvent;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class MtTest {

	private RsdsSbbProxy rsdsSbb;
	private MtSbbProxy mtSbb;
	private SriSbbProxy sriSbb;
	private CassandraPersistenceSbbProxy pers;
	private boolean cassandraDbInited;
	private Date curDate;

	private String msdnDig = "5555";
	private String origDig = "4444";
	private String imsiDig = "11111222225555";
	private String nnnDig = "2222";
	private TargetAddress ta1 = new TargetAddress(1, 1, msdnDig);
	private byte[] udhTemp = new byte[] { 5, 0, 3, -116, 2, 1 };

	private String msgShort = "01230123";

	@BeforeMethod
	public void setUpMethod() throws Exception {
		System.out.println("setUpMethod");

		SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance("Test");
		smscPropertiesManagement.setServiceCenterGt("1111");
		smscPropertiesManagement.setServiceCenterSsn(8);
		smscPropertiesManagement.setHlrSsn(6);
		smscPropertiesManagement.setMscSsn(8);

		this.pers = new CassandraPersistenceSbbProxy();
		this.cassandraDbInited = this.pers.testCassandraAccess();
		if (!this.cassandraDbInited)
			return;

		this.mtSbb = new MtSbbProxy(this.pers);
		this.rsdsSbb = new RsdsSbbProxy(this.pers);
		this.sriSbb = new SriSbbProxy(this.pers, this.mtSbb, this.rsdsSbb);
		this.mtSbb.setSriSbbProxy(this.sriSbb);

		SmscPropertiesManagement.getInstance("Test");
	}

	@AfterMethod
	public void tearDownMethod() throws Exception {
		System.out.println("tearDownMethod");
	}


	/**
	 * MAP V3, 1 message, 1 segment, GSM7
	 */
	@Test(groups = { "Mt" })
	public void SuccessDelivery1Test() throws Exception {

		if (!this.cassandraDbInited)
			return;

		MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
		MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
		SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

		this.clearDatabase();

		ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
		SmsDef sd1 = new SmsDef();
		lst.add(sd1);
		SmsSet smsSet = prepareDatabase(lst);

		this.pers.setDeliveryStart(smsSet, curDate);

		// initial onSms message
		SmsSetEvent event = new SmsSetEvent();
		event.setSmsSet(smsSet);
		this.sriSbb.onSms(event, null, null);

		MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
		MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
		assertEquals(acv, MAPApplicationContextVersion.version3);

		assertNull(serviceMt.getLastMAPDialogSms());
		ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
		assertEquals(lstEvt.size(), 2);
		assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
		assertEquals(((GT0100) dlg.getLocalAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
		assertEquals(((GT0100) dlg.getLocalAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);
		assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);
		assertEquals(((GT0100) dlg.getRemoteAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
		assertEquals(((GT0100) dlg.getRemoteAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);

		MAPTestEvent evt = lstEvt.get(0);
		assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
		SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
		assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
		assertEquals(sriReq.getMsisdn().getAddressNature(), AddressNature.international_number);
		assertEquals(sriReq.getMsisdn().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
		assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());
		assertEquals(sriReq.getServiceCentreAddress().getAddressNature(), AddressNature.international_number);
		assertEquals(sriReq.getServiceCentreAddress().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
		assertTrue(sriReq.getSm_RP_PRI());

		evt = lstEvt.get(1);
		assertEquals(evt.testEventType, MAPTestEventType.send);

		// SRI response
		IMSI imsi = new IMSIImpl(imsiDig);
		ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
				org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, nnnDig);
		LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, null, null);
		SendRoutingInfoForSMResponse evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, null);
		evt1.setMAPDialog(dlg);
		this.sriSbb.onSendRoutingInfoForSMResponse(evt1, null);
		this.sriSbb.onDialogDelimiter(null, null);

		dlg = serviceMt.getLastMAPDialogSms();
		acv =  dlg.getApplicationContext().getApplicationContextVersion();
		assertEquals(acv, MAPApplicationContextVersion.version3);

		lstEvt = dlg.getEventList();
		assertEquals(lstEvt.size(), 2);

		dlg = serviceMt.getLastMAPDialogSms();
		lstEvt = dlg.getEventList();
		assertEquals(lstEvt.size(), 2);
		evt = lstEvt.get(0);
		assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
		MtForwardShortMessageRequestImpl mtFsmReq = (MtForwardShortMessageRequestImpl) evt.event;
		assertFalse(mtFsmReq.getMoreMessagesToSend());
		SM_RP_DA sm_RP_DA = mtFsmReq.getSM_RP_DA();
		IMSI daImsi = sm_RP_DA.getIMSI();
		assertEquals(daImsi.getData(), imsiDig);
		SM_RP_OA sm_RP_OA = mtFsmReq.getSM_RP_OA();
		AddressString scas = sm_RP_OA.getServiceCentreAddressOA();
		assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
		assertEquals(scas.getAddressNature(), AddressNature.international_number);
		assertEquals(scas.getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
		SmsSignalInfo ssi = mtFsmReq.getSM_RP_UI();
		SmsDeliverTpdu tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
		assertEquals(tpdu.getDataCodingScheme().getCode(), 0);
		assertFalse(tpdu.getForwardedOrSpawned());
		assertFalse(tpdu.getMoreMessagesToSend());
		assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
		assertEquals(tpdu.getOriginatingAddress().getNumberingPlanIdentification(), NumberingPlanIdentification.ISDNTelephoneNumberingPlan);
		assertEquals(tpdu.getOriginatingAddress().getTypeOfNumber(), TypeOfNumber.InternationalNumber);
		assertEquals(tpdu.getProtocolIdentifier().getCode(), 7);
		assertFalse(tpdu.getReplyPathExists());
		assertEquals(tpdu.getServiceCentreTimeStamp().getDay(), curDate.getDate());
		assertEquals(tpdu.getServiceCentreTimeStamp().getMinute(), curDate.getMinutes());
		int mon1 = tpdu.getServiceCentreTimeStamp().getMonth();
		int mon2 = curDate.getMonth() + 1;
		assertEquals(mon1, mon2);
		assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
		assertFalse(tpdu.getStatusReportIndication());
		assertFalse(tpdu.getUserDataHeaderIndicator());
		UserData ud = tpdu.getUserData();
		assertNull(ud.getDecodedUserDataHeader());
		ud.decode();
		String msg1 = ud.getDecodedMessage();
		assertEquals(msg1, msgShort);

		evt = lstEvt.get(1);
		assertEquals(evt.testEventType, MAPTestEventType.send);

		boolean b1 = this.pers.checkSmsSetExists(ta1);
		assertTrue(b1);
		UUID smsId = smsSet.getSms(0).getDbId();
		Sms smsx1 = this.pers.obtainLiveSms(smsId);
		assertNotNull(smsx1);
		SmsProxy smsx2 = this.pers.obtainArchiveSms(smsId);
		assertNull(smsx2);

		// Mt response
		MtForwardShortMessageResponseImpl evt2 = new MtForwardShortMessageResponseImpl(null, null);
		evt2.setMAPDialog(dlg);
		this.mtSbb.onMtForwardShortMessageResponse(evt2, null);
		this.mtSbb.onDialogClose(null, null);

		dlg = serviceSri.getLastMAPDialogSms();
		lstEvt = dlg.getEventList();
		assertEquals(lstEvt.size(), 2);

		dlg = serviceMt.getLastMAPDialogSms();
		lstEvt = dlg.getEventList();
		assertEquals(lstEvt.size(), 2);

		b1 = this.pers.checkSmsSetExists(ta1);
		assertFalse(b1);
		smsx1 = this.pers.obtainLiveSms(smsId);
		assertNull(smsx1);
		smsx2 = this.pers.obtainArchiveSms(smsId);
		assertNotNull(smsx2);
	}

	/**
	 * MAP V2, 2 message:
	 *  - USC2, 1 segment, +UDH
	 *  - GSM7, 1 segment, +ReplyPathExists, Empty TC-BEGIN
	 * InforServiceCenter -> SRDS(Success)
	 */
	@Test(groups = { "Mt" })
	public void SuccessDelivery2Test() throws Exception {

		if (!this.cassandraDbInited)
			return;

		MAPServiceSmsProxy serviceSri = (MAPServiceSmsProxy)this.sriSbb.mapProvider.getMAPServiceSms();
		MAPServiceSmsProxy serviceMt = (MAPServiceSmsProxy)this.mtSbb.mapProvider.getMAPServiceSms();
		MAPServiceSmsProxy serviceRsds = (MAPServiceSmsProxy)this.rsdsSbb.mapProvider.getMAPServiceSms();
		SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

		this.clearDatabase();

		ArrayList<SmsDef> lst = new ArrayList<SmsDef>();
		SmsDef sd1 = new SmsDef();
		lst.add(sd1);
		sd1.dataCodingScheme = 8;
		sd1.esmClass = 3 + 0x40;
		String msga1 = this.msgShort + "ß";
		Charset ucs2Charset = Charset.forName("UTF-16BE");
		ByteBuffer bb = ucs2Charset.encode(msga1);
		byte[] buf = new byte[udhTemp.length + bb.limit()];
		System.arraycopy(udhTemp, 0, buf, 0, udhTemp.length);
		bb.get(buf, udhTemp.length, bb.limit());
		sd1.msg = buf;

		SmsDef sd2 = new SmsDef();
		lst.add(sd2);
		sd2.esmClass = 3 + 0x80;
		StringBuilder sb = new StringBuilder();
		for (int i2 = 0; i2 < 16; i2++) {
			sb.append("1234567890");
		}
		String msga2 = sb.toString();
		sd2.msg = msga2.getBytes();

		SmsSet smsSet = prepareDatabase(lst);
		Sms sms1 = smsSet.getSms(0);
		Sms sms2 = smsSet.getSms(1);

		this.pers.setDeliveryStart(smsSet, curDate);

		// initial onSms message
		SmsSetEvent event = new SmsSetEvent();
		event.setSmsSet(smsSet);
		this.sriSbb.onSms(event, null, null);

		assertNull(serviceMt.getLastMAPDialogSms());
		MAPDialogSmsProxy dlg = serviceSri.getLastMAPDialogSms();
		MAPApplicationContextVersion acv =  dlg.getApplicationContext().getApplicationContextVersion();
		assertEquals(acv, MAPApplicationContextVersion.version3);
		ArrayList<MAPTestEvent> lstEvt = dlg.getEventList();
		assertEquals(lstEvt.size(), 2);
		assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
		assertEquals(((GT0100) dlg.getLocalAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
		assertEquals(((GT0100) dlg.getLocalAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);
		assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);
		assertEquals(((GT0100) dlg.getRemoteAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
		assertEquals(((GT0100) dlg.getRemoteAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);

		MAPTestEvent evt = lstEvt.get(0);
		assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
		SendRoutingInfoForSMRequest sriReq = (SendRoutingInfoForSMRequest) evt.event;
		assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
		assertEquals(sriReq.getMsisdn().getAddressNature(), AddressNature.international_number);
		assertEquals(sriReq.getMsisdn().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
		assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());
		assertEquals(sriReq.getServiceCentreAddress().getAddressNature(), AddressNature.international_number);
		assertEquals(sriReq.getServiceCentreAddress().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
		assertTrue(sriReq.getSm_RP_PRI());

		evt = lstEvt.get(1);
		assertEquals(evt.testEventType, MAPTestEventType.send);

		// SRI "MAP only V2 supported" response
		ApplicationContextNameImpl acn = new ApplicationContextNameImpl();
		acn.setOid(new long[] { 0, 4, 0, 0, 1, 0, 20, 2 });
		DialogReject evt3 = new DialogReject(dlg, MAPRefuseReason.ApplicationContextNotSupported, acn, null);
		this.sriSbb.onDialogReject(evt3, null);

		dlg = serviceSri.getLastMAPDialogSms();
		acv =  dlg.getApplicationContext().getApplicationContextVersion();
		assertEquals(acv, MAPApplicationContextVersion.version2);
		lstEvt = dlg.getEventList();
		assertEquals(lstEvt.size(), 2);
		assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
		assertEquals(((GT0100) dlg.getLocalAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
		assertEquals(((GT0100) dlg.getLocalAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);
		assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);
		assertEquals(((GT0100) dlg.getRemoteAddress().getGlobalTitle()).getNumberingPlan(), org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY);
		assertEquals(((GT0100) dlg.getRemoteAddress().getGlobalTitle()).getNatureOfAddress(), NatureOfAddress.INTERNATIONAL);

		evt = lstEvt.get(0);
		assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
		sriReq = (SendRoutingInfoForSMRequest) evt.event;
		assertEquals(sriReq.getMsisdn().getAddress(), msdnDig);
		assertEquals(sriReq.getMsisdn().getAddressNature(), AddressNature.international_number);
		assertEquals(sriReq.getMsisdn().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
		assertEquals(sriReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());
		assertEquals(sriReq.getServiceCentreAddress().getAddressNature(), AddressNature.international_number);
		assertEquals(sriReq.getServiceCentreAddress().getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
		assertTrue(sriReq.getSm_RP_PRI());

		evt = lstEvt.get(1);
		assertEquals(evt.testEventType, MAPTestEventType.send);
		
		// SRI response + ISC request
		IMSI imsi = new IMSIImpl(imsiDig);
		ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
				org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, nnnDig);
		LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, null, null);
		SendRoutingInfoForSMResponse evt1 = new SendRoutingInfoForSMResponseImpl(imsi, locationInfoWithLMSI, null, null);
		evt1.setMAPDialog(dlg);
		this.sriSbb.onSendRoutingInfoForSMResponse(evt1, null);

		ISDNAddressStringImpl storedMSISDN = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, msdnDig);
		MWStatusImpl mwStatus = new MWStatusImpl(false, true, false, true);
		InformServiceCentreRequestImpl evt4 = new InformServiceCentreRequestImpl(storedMSISDN, mwStatus, null, null, null);
		evt4.setMAPDialog(dlg);
		this.sriSbb.onInformServiceCentreRequest(evt4, null);
		
		this.sriSbb.onDialogDelimiter(null, null);

		dlg = serviceMt.getLastMAPDialogSms();
		acv =  dlg.getApplicationContext().getApplicationContextVersion();
		assertEquals(acv, MAPApplicationContextVersion.version3);

		// MT "MAP only V2 supported" response
		acn = new ApplicationContextNameImpl();
		acn.setOid(new long[] { 0, 4, 0, 0, 1, 0, 25, 2 });
		DialogReject evt5 = new DialogReject(dlg, MAPRefuseReason.ApplicationContextNotSupported, acn, null);
		this.mtSbb.onDialogReject(evt5, null);

		// Analyzing MtMessage 1
		dlg = serviceMt.getLastMAPDialogSms();
		acv =  dlg.getApplicationContext().getApplicationContextVersion();
		assertEquals(acv, MAPApplicationContextVersion.version2);

		dlg = serviceSri.getLastMAPDialogSms();
		lstEvt = dlg.getEventList();
		assertEquals(lstEvt.size(), 2);

		dlg = serviceMt.getLastMAPDialogSms();
		lstEvt = dlg.getEventList();
		assertEquals(lstEvt.size(), 2);
		evt = lstEvt.get(0);
		assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
		ForwardShortMessageRequestImpl mtFsmReq = (ForwardShortMessageRequestImpl) evt.event;
		assertTrue(mtFsmReq.getMoreMessagesToSend());
		SM_RP_DA sm_RP_DA = mtFsmReq.getSM_RP_DA();
		IMSI daImsi = sm_RP_DA.getIMSI();
		assertEquals(daImsi.getData(), imsiDig);
		SM_RP_OA sm_RP_OA = mtFsmReq.getSM_RP_OA();
		AddressString scas = sm_RP_OA.getServiceCentreAddressOA();
		assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
		assertEquals(scas.getAddressNature(), AddressNature.international_number);
		assertEquals(scas.getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
		SmsSignalInfo ssi = mtFsmReq.getSM_RP_UI();
		SmsDeliverTpdu tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
		assertEquals(tpdu.getDataCodingScheme().getCode(), 8);
		assertFalse(tpdu.getForwardedOrSpawned());
		assertTrue(tpdu.getMoreMessagesToSend());
		assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
		assertEquals(tpdu.getOriginatingAddress().getNumberingPlanIdentification(), NumberingPlanIdentification.ISDNTelephoneNumberingPlan);
		assertEquals(tpdu.getOriginatingAddress().getTypeOfNumber(), TypeOfNumber.InternationalNumber);
		assertEquals(tpdu.getProtocolIdentifier().getCode(), 7);
		assertFalse(tpdu.getReplyPathExists());
		assertEquals(tpdu.getServiceCentreTimeStamp().getDay(), curDate.getDate());
		assertEquals(tpdu.getServiceCentreTimeStamp().getMinute(), curDate.getMinutes());
		int mon1 = tpdu.getServiceCentreTimeStamp().getMonth();
		int mon2 = curDate.getMonth() + 1;
		assertEquals(mon1, mon2);
		assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
		assertFalse(tpdu.getStatusReportIndication());
		assertTrue(tpdu.getUserDataHeaderIndicator());
		UserData ud = tpdu.getUserData();
		ud.decode();
		UserDataHeader udh = ud.getDecodedUserDataHeader();
		ConcatenatedShortMessagesIdentifier udhc = udh.getConcatenatedShortMessagesIdentifier();
		assertNotNull(udhc);
		assertEquals(udhc.getReference(), 140);
		String msg1 = ud.getDecodedMessage();
		assertEquals(msg1, msga1);

		evt = lstEvt.get(1);
		assertEquals(evt.testEventType, MAPTestEventType.send);

		boolean b1 = this.pers.checkSmsSetExists(ta1);
		assertTrue(b1);
		UUID smsId = sms1.getDbId();
		Sms smsx1 = this.pers.obtainLiveSms(smsId);
		assertNotNull(smsx1);
		SmsProxy smsx2 = this.pers.obtainArchiveSms(smsId);
		assertNull(smsx2);

		// Mt response
		ForwardShortMessageResponseImpl evt2 = new ForwardShortMessageResponseImpl();
		evt2.setMAPDialog(dlg);
		this.mtSbb.onForwardShortMessageResponse(evt2, null);
		this.mtSbb.onDialogClose(null, null);

		dlg = serviceMt.getLastMAPDialogSms();
		lstEvt = dlg.getEventList();
		assertEquals(lstEvt.size(), 3);
		evt = lstEvt.get(0);
		assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
		evt = lstEvt.get(1);
		assertEquals(evt.testEventType, MAPTestEventType.cancelInvoke);
		evt = lstEvt.get(2);
		assertEquals(evt.testEventType, MAPTestEventType.send);

		b1 = this.pers.checkSmsSetExists(ta1);
		assertTrue(b1);
		smsId = sms2.getDbId();
		smsx1 = this.pers.obtainLiveSms(smsId);
		assertNotNull(smsx1);
		smsx2 = this.pers.obtainArchiveSms(smsId);
		assertNull(smsx2);

		// TC-CONTINUE after empty TC-BEGIN
		DialogDelimiter evt6 = new DialogDelimiter(dlg);
		this.mtSbb.onDialogDelimiter(evt6, null);

		// Analyzing MtMessage 2
		dlg = serviceMt.getLastMAPDialogSms();
		acv =  dlg.getApplicationContext().getApplicationContextVersion();
		assertEquals(acv, MAPApplicationContextVersion.version2);

		dlg = serviceMt.getLastMAPDialogSms();
		lstEvt = dlg.getEventList();
		assertEquals(lstEvt.size(), 5);
		evt = lstEvt.get(3);
		assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
		mtFsmReq = (ForwardShortMessageRequestImpl) evt.event;
		assertFalse(mtFsmReq.getMoreMessagesToSend());
		sm_RP_DA = mtFsmReq.getSM_RP_DA();
		daImsi = sm_RP_DA.getIMSI();
		assertEquals(daImsi.getData(), imsiDig);
		sm_RP_OA = mtFsmReq.getSM_RP_OA();
		scas = sm_RP_OA.getServiceCentreAddressOA();
		assertEquals(scas.getAddress(), smscPropertiesManagement.getServiceCenterGt());
		assertEquals(scas.getAddressNature(), AddressNature.international_number);
		assertEquals(scas.getNumberingPlan(), org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN);
		ssi = mtFsmReq.getSM_RP_UI();
		tpdu = (SmsDeliverTpdu) ssi.decodeTpdu(false);
		assertEquals(tpdu.getDataCodingScheme().getCode(), 0);
		assertFalse(tpdu.getForwardedOrSpawned());
		assertFalse(tpdu.getMoreMessagesToSend());
		assertEquals(tpdu.getOriginatingAddress().getAddressValue(), origDig);
		assertEquals(tpdu.getOriginatingAddress().getNumberingPlanIdentification(), NumberingPlanIdentification.ISDNTelephoneNumberingPlan);
		assertEquals(tpdu.getOriginatingAddress().getTypeOfNumber(), TypeOfNumber.InternationalNumber);
		assertEquals(tpdu.getProtocolIdentifier().getCode(), 7);
		assertTrue(tpdu.getReplyPathExists());
		assertEquals(tpdu.getServiceCentreTimeStamp().getDay(), curDate.getDate());
		assertEquals(tpdu.getServiceCentreTimeStamp().getMinute(), curDate.getMinutes());
		mon1 = tpdu.getServiceCentreTimeStamp().getMonth();
		mon2 = curDate.getMonth() + 1;
		assertEquals(mon1, mon2);
		assertEquals(tpdu.getSmsTpduType(), SmsTpduType.SMS_DELIVER);
		assertFalse(tpdu.getStatusReportIndication());
		assertFalse(tpdu.getUserDataHeaderIndicator());
		ud = tpdu.getUserData();
		ud.decode();
		udh = ud.getDecodedUserDataHeader();
		assertNull(udh);
		msg1 = ud.getDecodedMessage();
		assertEquals(msg1, msga2);

		evt = lstEvt.get(4);
		assertEquals(evt.testEventType, MAPTestEventType.send);

		b1 = this.pers.checkSmsSetExists(ta1);
		assertTrue(b1);
		smsId = sms2.getDbId();
		smsx1 = this.pers.obtainLiveSms(smsId);
		assertNotNull(smsx1);
		smsx2 = this.pers.obtainArchiveSms(smsId);
		assertNull(smsx2);

		// Mt response 2
		evt2 = new ForwardShortMessageResponseImpl();
		evt2.setMAPDialog(dlg);
		this.mtSbb.onForwardShortMessageResponse(evt2, null);
		this.mtSbb.onDialogClose(null, null);

		// .............................

		dlg = serviceRsds.getLastMAPDialogSms();
		acv =  dlg.getApplicationContext().getApplicationContextVersion();
		assertEquals(acv, MAPApplicationContextVersion.version2);
		assertEquals(dlg.getLocalAddress().getGlobalTitle().getDigits(), smscPropertiesManagement.getServiceCenterGt());
		assertEquals(dlg.getRemoteAddress().getGlobalTitle().getDigits(), msdnDig);

		lstEvt = dlg.getEventList();
		assertEquals(lstEvt.size(), 2);
		evt = lstEvt.get(0);

		assertEquals(evt.testEventType, MAPTestEventType.componentAdded);
		ReportSMDeliveryStatusRequestImpl rsdsReq = (ReportSMDeliveryStatusRequestImpl) evt.event;
		assertEquals(rsdsReq.getServiceCentreAddress().getAddress(), smscPropertiesManagement.getServiceCenterGt());
		assertEquals(rsdsReq.getMsisdn().getAddress(), msdnDig);
		assertEquals(rsdsReq.getSMDeliveryOutcome(), SMDeliveryOutcome.successfulTransfer);

		evt = lstEvt.get(1);
		assertEquals(evt.testEventType, MAPTestEventType.send);

		b1 = this.pers.checkSmsSetExists(ta1);
		assertFalse(b1);
		smsId = sms2.getDbId();
		smsx1 = this.pers.obtainLiveSms(smsId);
		assertNull(smsx1);
		smsx2 = this.pers.obtainArchiveSms(smsId);
		assertNotNull(smsx2);

		// rsds response 2
		ReportSMDeliveryStatusResponseImpl evt7 = new ReportSMDeliveryStatusResponseImpl(2, null, null);
		evt7.setMAPDialog(dlg);
		this.rsdsSbb.onReportSMDeliveryStatusResponse(evt7, null);
		this.rsdsSbb.onDialogClose(null, null);

	}

	private void clearDatabase() throws PersistenceException, IOException {

		SmsSet smsSet_x1 = this.pers.obtainSmsSet(ta1);
		this.pers.fetchSchedulableSms(smsSet_x1);

		this.pers.deleteSmsSet(smsSet_x1);
		int cnt = smsSet_x1.getSmsCount();
		for (int i1 = 0; i1 < cnt; i1++) {
			Sms sms = smsSet_x1.getSms(i1);
			this.pers.deleteLiveSms(sms.getDbId());
		}
		this.pers.deleteSmsSet(smsSet_x1);
	}

	private SmsSet prepareDatabase(ArrayList<SmsDef> lst) throws PersistenceException {
		SmsSet smsSet = this.pers.obtainSmsSet(ta1);

		int i1 = 1;
		for (SmsDef smsDef : lst) {
			Sms sms = this.prepareSms(smsSet, i1, smsDef);
			this.pers.createLiveSms(sms);
			i1++;
		}

		SmsSet res = this.pers.obtainSmsSet(ta1);
		this.pers.fetchSchedulableSms(res);
		curDate = new Date();
		this.pers.setDeliveryStart(smsSet, curDate);
		return res;
	}

	private Sms prepareSms(SmsSet smsSet, int num, SmsDef smsDef) {

		Sms sms = new Sms();
		sms.setSmsSet(smsSet);

		sms.setDbId(UUID.randomUUID());
		// sms.setDbId(id);
		sms.setSourceAddr(origDig);
		sms.setSourceAddrTon(1);
		sms.setSourceAddrNpi(1);
		sms.setMessageId(8888888 + num);
		sms.setMoMessageRef(102 + num);
		
		sms.setMessageId(num);

		sms.setOrigEsmeName("esme_1");
		sms.setOrigSystemId("sys_1");

		sms.setSubmitDate(new Date());
		// sms.setDeliveryDate(new GregorianCalendar(2013, 1, 15, 12, 15 +
		// num).getTime());

		// sms.setServiceType("serv_type__" + num);
		sms.setEsmClass(smsDef.esmClass);
		sms.setProtocolId(7);
		sms.setPriority(0);
		sms.setRegisteredDelivery(0);
		sms.setReplaceIfPresent(0);
		sms.setDataCoding(smsDef.dataCodingScheme);
		sms.setDefaultMsgId(0);

		sms.setShortMessage(smsDef.msg);

		// sms.setScheduleDeliveryTime(new GregorianCalendar(2013, 1, 20, 10, 00
		// + num).getTime());
		// sms.setValidityPeriod(new GregorianCalendar(2013, 1, 23, 13, 33 +
		// num).getTime());

		// short tag, byte[] value, String tagName
		// Tlv tlv = new Tlv((short) 5, new byte[] { (byte) (1 + num), 2, 3, 4,
		// 5 });
		// sms.getTlvSet().addOptionalParameter(tlv);
		// tlv = new Tlv((short) 6, new byte[] { (byte) (6 + num), 7, 8 });
		// sms.getTlvSet().addOptionalParameter(tlv);

		return sms;
	}

	private class SmsDef {
		public int dataCodingScheme = 0; // 0-GSM7, 8-UCS2
		public int esmClass = 3; // 3 + 0x40 (UDH) + 0x80 (ReplyPath)
		public byte[] msg = msgShort.getBytes();
	}
	
}
