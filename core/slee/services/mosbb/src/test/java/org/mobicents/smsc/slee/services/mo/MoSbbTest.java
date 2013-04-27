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

package org.mobicents.smsc.slee.services.mo;

import static org.testng.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;

import javax.slee.ActivityContextInterface;
import javax.slee.SLEEException;
import javax.slee.SbbLocalObject;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.TransactionRolledbackLocalException;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPDialogListener;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.MAPServiceBase;
import org.mobicents.protocols.ss7.map.api.MAPSmsTpduParameterFactory;
import org.mobicents.protocols.ss7.map.api.dialog.MAPDialogState;
import org.mobicents.protocols.ss7.map.api.dialog.MAPUserAbortChoice;
import org.mobicents.protocols.ss7.map.api.dialog.Reason;
import org.mobicents.protocols.ss7.map.api.dialog.ServingCheckData;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageFactory;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.service.callhandling.MAPServiceCallHandling;
import org.mobicents.protocols.ss7.map.api.service.lsm.MAPServiceLsm;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPServiceMobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberManagement.TeleserviceCode;
import org.mobicents.protocols.ss7.map.api.service.oam.MAPServiceOam;
import org.mobicents.protocols.ss7.map.api.service.pdpContextActivation.MAPServicePdpContextActivation;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPServiceSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MWStatus;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_MTI;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_SMEA;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.mobicents.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementary;
import org.mobicents.protocols.ss7.map.api.smstpdu.AbsoluteTimeStamp;
import org.mobicents.protocols.ss7.map.api.smstpdu.AddressField;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.mobicents.protocols.ss7.map.api.smstpdu.ProtocolIdentifier;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.TypeOfNumber;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserData;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeaderElement;
import org.mobicents.protocols.ss7.map.api.smstpdu.ValidityPeriod;
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageFactoryImpl;
import org.mobicents.protocols.ss7.map.primitives.AddressStringImpl;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.protocols.ss7.map.service.sms.ForwardShortMessageRequestImpl;
import org.mobicents.protocols.ss7.map.service.sms.MoForwardShortMessageRequestImpl;
import org.mobicents.protocols.ss7.map.service.sms.SM_RP_DAImpl;
import org.mobicents.protocols.ss7.map.service.sms.SM_RP_OAImpl;
import org.mobicents.protocols.ss7.map.service.sms.SmsSignalInfoImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AbsoluteTimeStampImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AddressFieldImpl;
import org.mobicents.protocols.ss7.map.smstpdu.ConcatenatedShortMessagesIdentifierImpl;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.protocols.ss7.map.smstpdu.ProtocolIdentifierImpl;
import org.mobicents.protocols.ss7.map.smstpdu.SmsSubmitTpduImpl;
import org.mobicents.protocols.ss7.map.smstpdu.UserDataHeaderImpl;
import org.mobicents.protocols.ss7.map.smstpdu.UserDataImpl;
import org.mobicents.protocols.ss7.map.smstpdu.ValidityPeriodImpl;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.api.MessageType;
import org.mobicents.protocols.ss7.tcap.asn.comp.Invoke;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.mobicents.protocols.ss7.tcap.asn.comp.ReturnResult;
import org.mobicents.protocols.ss7.tcap.asn.comp.ReturnResultLast;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.smsc.slee.resources.smpp.server.SmppSessions;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransaction;
import org.mobicents.smsc.slee.services.persistence.CassandraPersistenceSbbProxy;
import org.mobicents.smsc.slee.services.persistence.MessageUtil;
import org.mobicents.smsc.slee.services.persistence.Persistence;
import org.mobicents.smsc.slee.services.persistence.PersistenceException;
import org.mobicents.smsc.slee.services.persistence.Sms;
import org.mobicents.smsc.slee.services.persistence.SmsSet;
import org.mobicents.smsc.slee.services.persistence.TargetAddress;
import org.mobicents.smsc.slee.services.persistence.TraceProxy;
import org.mobicents.smsc.smpp.Esme;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.cloudhopper.smpp.SmppConstants;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class MoSbbTest {

	private MoSbbProxy sbb;
	private CassandraPersistenceSbbProxy pers;
	private boolean cassandraDbInited;

	private TargetAddress ta1 = new TargetAddress(1, 1, "5555");

//	private byte[] msg = { 11, 12, 13, 14, 15, 15 };

	@BeforeClass
	public void setUpClass() throws Exception {
		System.out.println("setUpClass");

		this.pers = new CassandraPersistenceSbbProxy();
		this.cassandraDbInited = this.pers.testCassandraAccess();
		if (!this.cassandraDbInited)
			return;

		this.sbb = new MoSbbProxy(this.pers);

		SmscPropertiesManagement.getInstance("Test");
	}

	@AfterClass
	public void tearDownClass() throws Exception {
		System.out.println("tearDownClass");
	}

	@Test(groups = { "Mo" })
	public void testMo1_Gsm7() throws Exception {

		if (!this.cassandraDbInited)
			return;

		this.clearDatabase();


		AddressString serviceCentreAddressDA = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "1111");
		SM_RP_DA sm_RP_DA = new SM_RP_DAImpl(serviceCentreAddressDA);
		ISDNAddressString msisdn = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "4444");
		SM_RP_OAImpl sm_RP_OA = new SM_RP_OAImpl();
		sm_RP_OA.setMsisdn(msisdn);
		AddressField destinationAddress = new AddressFieldImpl(TypeOfNumber.InternationalNumber, NumberingPlanIdentification.ISDNTelephoneNumberingPlan, "5555");
		ProtocolIdentifier protocolIdentifier = new ProtocolIdentifierImpl(12);
		ValidityPeriod validityPeriod = new ValidityPeriodImpl(11); // 11==60min
		DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(0);
		UserDataHeader decodedUserDataHeader = new UserDataHeaderImpl();
		UserDataHeaderElement informationElement = new ConcatenatedShortMessagesIdentifierImpl(false, 55, 3, 1);
//		boolean referenceIs16bit, int reference, int mesageSegmentCount, int mesageSegmentNumber
		decodedUserDataHeader.addInformationElement(informationElement);
		UserData userData = new UserDataImpl(new String("0123456789"), dataCodingScheme, decodedUserDataHeader, null);
//		userData.encode();
//		String decodedMessage, DataCodingScheme dataCodingScheme, UserDataHeader decodedUserDataHeader, Charset gsm8Charset
		SmsTpdu tpdu = new SmsSubmitTpduImpl(false, true, false, 150, destinationAddress, protocolIdentifier, validityPeriod, userData);
		//		boolean rejectDuplicates, boolean replyPathExists, boolean statusReportRequest, int messageReference,
		//		AddressField destinationAddress, ProtocolIdentifier protocolIdentifier, ValidityPeriod validityPeriod, UserData userData
//		tpdu.encodeData();
		SmsSignalInfo sm_RP_UI = new SmsSignalInfoImpl(tpdu, null);
		MoForwardShortMessageRequest event = new MoForwardShortMessageRequestImpl(sm_RP_DA, sm_RP_OA, sm_RP_UI, null, null);

//		ActivityContextInterface aci = new SmppTransactionProxy(esme);

//		Date curDate = new Date();
//		this.fillSm(event, curDate, true);
//		event.setShortMessage(msg);

		boolean b1 = this.pers.checkSmsSetExists(ta1);
		assertFalse(b1);

		MAPDialogSmsProxy dialog = new MAPDialogSmsProxy();
		event.setMAPDialog(dialog);
		Date curDate = new Date();
		this.sbb.onMoForwardShortMessageRequest(event, null);

		b1 = this.pers.checkSmsSetExists(ta1);
		assertTrue(b1);
		SmsSet smsSet = this.pers.obtainSmsSet(ta1);

		assertEquals(dialog.getResponseCount(), 1);
		assertEquals(dialog.getErrorList().size(), 0);

		this.pers.fetchSchedulableSms(smsSet);

		assertEquals(smsSet.getDestAddr(), "5555");
		assertEquals(smsSet.getDestAddrTon(), SmppConstants.TON_INTERNATIONAL);
		assertEquals(smsSet.getDestAddrNpi(), SmppConstants.NPI_E164);

		assertEquals(smsSet.getInSystem(), 1);
		assertEquals(smsSet.getDueDelay(), 0);
		assertNull(smsSet.getStatus());
		assertFalse(smsSet.isAlertingSupported());

		Sms sms = smsSet.getFirstSms();
		assertNotNull(sms);
		assertEquals(sms.getSourceAddr(), "4444");
		assertEquals(sms.getSourceAddrTon(), SmppConstants.TON_INTERNATIONAL);
		assertEquals(sms.getSourceAddrNpi(), SmppConstants.NPI_E164);
		assertEquals(sms.getMessageId(), 150);

		assertEquals(sms.getDataCoding(), 0);
		assertNull(sms.getOrigEsmeName());
		assertNull(sms.getOrigSystemId());

		assertNull(sms.getServiceType());
		assertEquals(sms.getEsmClass() & 0xFF, 195);
		assertEquals(sms.getRegisteredDelivery(), 0);

		assertEquals(sms.getProtocolId(), 12);
		assertEquals(sms.getPriority(), 0);
		assertEquals(sms.getReplaceIfPresent(), 0);
		assertEquals(sms.getDefaultMsgId(), 0);

		assertEquals(sms.getTlvSet().getOptionalParameterCount(), 0);

		assertNull(sms.getScheduleDeliveryTime());
		assertDateEq(sms.getValidityPeriod(), MessageUtil.addHours(curDate, 1));

		assertEquals(sms.getDeliveryCount(), 0);

		assertDateEq(smsSet.getDueDate(), new Date(curDate.getTime() + 1 * 60 * 1000));

		byte[] buf = new byte[10];
		System.arraycopy(sms.getShortMessage(), sms.getShortMessage().length - 10, buf, 0, 10);
		assertEquals(new String(buf), "0123456789");
	}

	@Test(groups = { "Mo" })
	public void testMo2_Gsm7() throws Exception {

		if (!this.cassandraDbInited)
			return;

		this.clearDatabase();


		AddressString serviceCentreAddressDA = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "1111");
		SM_RP_DA sm_RP_DA = new SM_RP_DAImpl(serviceCentreAddressDA);
		ISDNAddressString msisdn = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "4444");
		SM_RP_OAImpl sm_RP_OA = new SM_RP_OAImpl();
		sm_RP_OA.setMsisdn(msisdn);
		AddressField destinationAddress = new AddressFieldImpl(TypeOfNumber.InternationalNumber, NumberingPlanIdentification.ISDNTelephoneNumberingPlan, "5555");
		ProtocolIdentifier protocolIdentifier = new ProtocolIdentifierImpl(12);
		Date valDate = MessageUtil.addHours(new Date(), 3); // 3 hours: 1 for delay + 2 different timezone 
		int tzo = -valDate.getTimezoneOffset();
		AbsoluteTimeStamp absoluteFormatValue = new AbsoluteTimeStampImpl(valDate.getYear(), valDate.getMonth(), valDate.getDate(), valDate.getHours(),
				valDate.getMinutes(), valDate.getSeconds(), tzo / 15 + 4 * 2);
		// int year, int month, int day, int hour, int minute, int second, int timeZone
		ValidityPeriod validityPeriod = new ValidityPeriodImpl(absoluteFormatValue);
		DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(0);
		UserData userData = new UserDataImpl(new String("0123456789"), dataCodingScheme, null, null);
//		userData.encode();
//		String decodedMessage, DataCodingScheme dataCodingScheme, UserDataHeader decodedUserDataHeader, Charset gsm8Charset
		SmsTpdu tpdu = new SmsSubmitTpduImpl(true, true, false, 150, destinationAddress, protocolIdentifier, validityPeriod, userData);
		//		boolean rejectDuplicates, boolean replyPathExists, boolean statusReportRequest, int messageReference,
		//		AddressField destinationAddress, ProtocolIdentifier protocolIdentifier, ValidityPeriod validityPeriod, UserData userData
//		tpdu.encodeData();
		SmsSignalInfo sm_RP_UI = new SmsSignalInfoImpl(tpdu, null);
		ForwardShortMessageRequest event = new ForwardShortMessageRequestImpl(sm_RP_DA, sm_RP_OA, sm_RP_UI, false);

//		ActivityContextInterface aci = new SmppTransactionProxy(esme);

//		Date curDate = new Date();
//		this.fillSm(event, curDate, true);
//		event.setShortMessage(msg);

		boolean b1 = this.pers.checkSmsSetExists(ta1);
		assertFalse(b1);

		MAPDialogSmsProxy dialog = new MAPDialogSmsProxy();
		event.setMAPDialog(dialog);
		Date curDate = new Date();
		this.sbb.onForwardShortMessageRequest(event, null);

		b1 = this.pers.checkSmsSetExists(ta1);
		assertTrue(b1);
		SmsSet smsSet = this.pers.obtainSmsSet(ta1);

		assertEquals(dialog.getResponseCount(), 1);
		assertEquals(dialog.getErrorList().size(), 0);

		this.pers.fetchSchedulableSms(smsSet);

		assertEquals(smsSet.getDestAddr(), "5555");
		assertEquals(smsSet.getDestAddrTon(), SmppConstants.TON_INTERNATIONAL);
		assertEquals(smsSet.getDestAddrNpi(), SmppConstants.NPI_E164);

		assertEquals(smsSet.getInSystem(), 1);
		assertEquals(smsSet.getDueDelay(), 0);
		assertNull(smsSet.getStatus());
		assertFalse(smsSet.isAlertingSupported());

		Sms sms = smsSet.getFirstSms();
		assertNotNull(sms);
		assertEquals(sms.getSourceAddr(), "4444");
		assertEquals(sms.getSourceAddrTon(), SmppConstants.TON_INTERNATIONAL);
		assertEquals(sms.getSourceAddrNpi(), SmppConstants.NPI_E164);
		assertEquals(sms.getMessageId(), 150);

		assertEquals(sms.getDataCoding(), 0);
		assertNull(sms.getOrigEsmeName());
		assertNull(sms.getOrigSystemId());

		assertNull(sms.getServiceType());
		assertEquals(sms.getEsmClass() & 0xFF, 131);
		assertEquals(sms.getRegisteredDelivery(), 0);

		assertEquals(sms.getProtocolId(), 12);
		assertEquals(sms.getPriority(), 0);
		assertEquals(sms.getReplaceIfPresent(), 2);
		assertEquals(sms.getDefaultMsgId(), 0);

		assertEquals(sms.getTlvSet().getOptionalParameterCount(), 0);

		assertNull(sms.getScheduleDeliveryTime());
		assertDateEq(sms.getValidityPeriod(), MessageUtil.addHours(curDate, 1));

		assertEquals(sms.getDeliveryCount(), 0);

		assertDateEq(smsSet.getDueDate(), new Date(curDate.getTime() + 1 * 60 * 1000));

		assertEquals(new String(sms.getShortMessage()), "0123456789");
	}

	@Test(groups = { "Mo" })
	public void testMo3_Usc2() throws Exception {

		if (!this.cassandraDbInited)
			return;

		this.clearDatabase();


		AddressString serviceCentreAddressDA = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "1111");
		SM_RP_DA sm_RP_DA = new SM_RP_DAImpl(serviceCentreAddressDA);
		ISDNAddressString msisdn = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "4444");
		SM_RP_OAImpl sm_RP_OA = new SM_RP_OAImpl();
		sm_RP_OA.setMsisdn(msisdn);
		AddressField destinationAddress = new AddressFieldImpl(TypeOfNumber.InternationalNumber, NumberingPlanIdentification.ISDNTelephoneNumberingPlan, "5555");
		ProtocolIdentifier protocolIdentifier = new ProtocolIdentifierImpl(12);
		DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(8);
		UserData userData = new UserDataImpl(new String("Привет"), dataCodingScheme, null, null);
//		String decodedMessage, DataCodingScheme dataCodingScheme, UserDataHeader decodedUserDataHeader, Charset gsm8Charset
		SmsTpdu tpdu = new SmsSubmitTpduImpl(false, false, false, 150, destinationAddress, protocolIdentifier, null, userData);
		//		boolean rejectDuplicates, boolean replyPathExists, boolean statusReportRequest, int messageReference,
		//		AddressField destinationAddress, ProtocolIdentifier protocolIdentifier, ValidityPeriod validityPeriod, UserData userData
//		tpdu.encodeData();
		SmsSignalInfo sm_RP_UI = new SmsSignalInfoImpl(tpdu, null);
		ForwardShortMessageRequest event = new ForwardShortMessageRequestImpl(sm_RP_DA, sm_RP_OA, sm_RP_UI, false);

//		ActivityContextInterface aci = new SmppTransactionProxy(esme);

//		Date curDate = new Date();
//		this.fillSm(event, curDate, true);
//		event.setShortMessage(msg);

		boolean b1 = this.pers.checkSmsSetExists(ta1);
		assertFalse(b1);

		MAPDialogSmsProxy dialog = new MAPDialogSmsProxy();
		event.setMAPDialog(dialog);
		Date curDate = new Date();
		this.sbb.onForwardShortMessageRequest(event, null);

		b1 = this.pers.checkSmsSetExists(ta1);
		assertTrue(b1);
		SmsSet smsSet = this.pers.obtainSmsSet(ta1);

		assertEquals(dialog.getResponseCount(), 1);
		assertEquals(dialog.getErrorList().size(), 0);

		this.pers.fetchSchedulableSms(smsSet);

		assertEquals(smsSet.getDestAddr(), "5555");
		assertEquals(smsSet.getDestAddrTon(), SmppConstants.TON_INTERNATIONAL);
		assertEquals(smsSet.getDestAddrNpi(), SmppConstants.NPI_E164);

		assertEquals(smsSet.getInSystem(), 1);
		assertEquals(smsSet.getDueDelay(), 0);
		assertNull(smsSet.getStatus());
		assertFalse(smsSet.isAlertingSupported());

		Sms sms = smsSet.getFirstSms();
		assertNotNull(sms);
		assertEquals(sms.getSourceAddr(), "4444");
		assertEquals(sms.getSourceAddrTon(), SmppConstants.TON_INTERNATIONAL);
		assertEquals(sms.getSourceAddrNpi(), SmppConstants.NPI_E164);
		assertEquals(sms.getMessageId(), 150);

		assertEquals(sms.getDataCoding(), 8);
		assertNull(sms.getOrigEsmeName());
		assertNull(sms.getOrigSystemId());

		assertNull(sms.getServiceType());
		assertEquals(sms.getEsmClass() & 0xFF, 3);
		assertEquals(sms.getRegisteredDelivery(), 0);

		assertEquals(sms.getProtocolId(), 12);
		assertEquals(sms.getPriority(), 0);
		assertEquals(sms.getReplaceIfPresent(), 0);
		assertEquals(sms.getDefaultMsgId(), 0);

		assertEquals(sms.getTlvSet().getOptionalParameterCount(), 0);

		assertNull(sms.getScheduleDeliveryTime());
		assertDateEq(sms.getValidityPeriod(), MessageUtil.addHours(curDate, 24 * 3));

		assertEquals(sms.getDeliveryCount(), 0);

		assertDateEq(smsSet.getDueDate(), new Date(curDate.getTime() + 1 * 60 * 1000));

		Charset ucs2Charset = Charset.forName("UTF-16BE");
		ByteBuffer bb = ByteBuffer.wrap(sms.getShortMessage());
		CharBuffer bf = ucs2Charset.decode(bb);
		String s = bf.toString();
		assertEquals(s, "Привет");
	}

	@Test(groups = { "Mo" })
	public void testMo4_BadEncScheme() throws Exception {

		if (!this.cassandraDbInited)
			return;

		this.clearDatabase();


		AddressString serviceCentreAddressDA = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "1111");
		SM_RP_DA sm_RP_DA = new SM_RP_DAImpl(serviceCentreAddressDA);
		ISDNAddressString msisdn = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "4444");
		SM_RP_OAImpl sm_RP_OA = new SM_RP_OAImpl();
		sm_RP_OA.setMsisdn(msisdn);
		AddressField destinationAddress = new AddressFieldImpl(TypeOfNumber.InternationalNumber, NumberingPlanIdentification.ISDNTelephoneNumberingPlan, "5555");
		ProtocolIdentifier protocolIdentifier = new ProtocolIdentifierImpl(12);
		DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(200);
		UserData userData = new UserDataImpl(new String("Привет"), dataCodingScheme, null, null);
//		String decodedMessage, DataCodingScheme dataCodingScheme, UserDataHeader decodedUserDataHeader, Charset gsm8Charset
		SmsTpdu tpdu = new SmsSubmitTpduImpl(false, false, false, 150, destinationAddress, protocolIdentifier, null, userData);
		//		boolean rejectDuplicates, boolean replyPathExists, boolean statusReportRequest, int messageReference,
		//		AddressField destinationAddress, ProtocolIdentifier protocolIdentifier, ValidityPeriod validityPeriod, UserData userData
//		tpdu.encodeData();
		SmsSignalInfo sm_RP_UI = new SmsSignalInfoImpl(tpdu, null);
		ForwardShortMessageRequest event = new ForwardShortMessageRequestImpl(sm_RP_DA, sm_RP_OA, sm_RP_UI, false);

//		ActivityContextInterface aci = new SmppTransactionProxy(esme);

//		Date curDate = new Date();
//		this.fillSm(event, curDate, true);
//		event.setShortMessage(msg);

		boolean b1 = this.pers.checkSmsSetExists(ta1);
		assertFalse(b1);

		MAPDialogSmsProxy dialog = new MAPDialogSmsProxy();
		event.setMAPDialog(dialog);
		Date curDate = new Date();
		this.sbb.onForwardShortMessageRequest(event, null);

		b1 = this.pers.checkSmsSetExists(ta1);
		assertFalse(b1);

		assertEquals(dialog.getResponseCount(), 0);
		assertEquals(dialog.getErrorList().size(), 1);
		assertEquals((long) dialog.getErrorList().get(0), MAPErrorCode.unexpectedDataValue);

	}

	private void clearDatabase() throws PersistenceException, IOException {

		// SmsSet smsSet_x1 = new SmsSet();
		// smsSet_x1.setDestAddr(ta1.getAddr());
		// smsSet_x1.setDestAddrTon(ta1.getAddrTon());
		// smsSet_x1.setDestAddrNpi(ta1.getAddrNpi());

		SmsSet smsSet_x1 = this.pers.obtainSmsSet(ta1);
		this.pers.fetchSchedulableSms(smsSet_x1);

		this.pers.deleteSmsSet(smsSet_x1);
		Sms sms = smsSet_x1.getFirstSms();
		while (sms != null) {
			this.pers.deleteLiveSms(sms.getDbId());
			sms = smsSet_x1.getNextSms();
		}
		this.pers.deleteSmsSet(smsSet_x1);
	}

	private void assertDateEq(Date d1, Date d2) {
		// creating d3 = d1 + 2 min

		long tm = d2.getTime();
		tm -= 15 * 1000;
		Date d3 = new Date(tm);

		tm = d2.getTime();
		tm += 15 * 1000;
		Date d4 = new Date(tm);

		assertTrue(d1.after(d3));
		assertTrue(d1.before(d4));
	}

	private class MoSbbProxy extends MoSbb {

		private CassandraPersistenceSbbProxy cassandraSbb;

		public MoSbbProxy(CassandraPersistenceSbbProxy cassandraSbb) {
			this.cassandraSbb = cassandraSbb;
			this.logger = new TraceProxy();
		}

		@Override
		public ChildRelationExt getStoreSbb() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Persistence getStore() {
			return cassandraSbb;
		}

		public void setSmppServerSessions(SmppSessions smppServerSessions) {
			this.smppServerSessions = smppServerSessions;
		}

		@Override
		public void setProcessingState(MoProcessingState processingState) {
			// TODO Auto-generated method stub

		}

		@Override
		public MoProcessingState getProcessingState() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MoActivityContextInterface asSbbActivityContextInterface(ActivityContextInterface aci) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	private class SmppTransactionProxy implements SmppTransaction, ActivityContextInterface {

		private Esme esme;

		public SmppTransactionProxy(Esme esme) {
			this.esme = esme;
		}

		@Override
		public Esme getEsme() {
			return this.esme;
		}

		@Override
		public void attach(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException, TransactionRolledbackLocalException,
				SLEEException {
			// TODO Auto-generated method stub

		}

		@Override
		public void detach(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException, TransactionRolledbackLocalException,
				SLEEException {
			// TODO Auto-generated method stub

		}

		@Override
		public Object getActivity() throws TransactionRequiredLocalException, SLEEException {
			// TODO Auto-generated method stub
			return this;
		}

		@Override
		public boolean isAttached(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException, TransactionRolledbackLocalException,
				SLEEException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isEnding() throws TransactionRequiredLocalException, SLEEException {
			// TODO Auto-generated method stub
			return false;
		}

	}

	private class MAPDialogSmsProxy implements MAPDialogSms {

		private int responseCount = 0;
		private ArrayList<Long> errorList = new ArrayList<Long>();
		private MAPServiceBaseProxy mapServiceBase = new MAPServiceBaseProxy();

		public int getResponseCount() {
			return responseCount;
		}

		public ArrayList<Long> getErrorList() {
			return errorList;
		}

		@Override
		public MAPDialogState getState() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SccpAddress getLocalAddress() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setLocalAddress(SccpAddress localAddress) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public SccpAddress getRemoteAddress() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setRemoteAddress(SccpAddress remoteAddress) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setReturnMessageOnError(boolean val) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean getReturnMessageOnError() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public MessageType getTCAPMessageType() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public AddressString getReceivedOrigReference() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public AddressString getReceivedDestReference() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MAPExtensionContainer getReceivedExtensionContainer() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void release() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void keepAlive() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Long getLocalDialogId() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Long getRemoteDialogId() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MAPServiceBase getService() {
			return mapServiceBase;
		}

		@Override
		public void setExtentionContainer(MAPExtensionContainer extContainer) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void send() throws MAPException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void close(boolean prearrangedEnd) throws MAPException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sendDelayed() throws MAPException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void closeDelayed(boolean prearrangedEnd) throws MAPException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void abort(MAPUserAbortChoice mapUserAbortChoice) throws MAPException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void refuse(Reason reason) throws MAPException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void processInvokeWithoutAnswer(Long invokeId) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sendInvokeComponent(Invoke invoke) throws MAPException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sendReturnResultComponent(ReturnResult returnResult) throws MAPException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sendReturnResultLastComponent(ReturnResultLast returnResultLast) throws MAPException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sendErrorComponent(Long invokeId, MAPErrorMessage mapErrorMessage) throws MAPException {
			this.errorList.add(mapErrorMessage.getErrorCode());
		}

		@Override
		public void sendRejectComponent(Long invokeId, Problem problem) throws MAPException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void resetInvokeTimer(Long invokeId) throws MAPException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean cancelInvocation(Long invokeId) throws MAPException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Object getUserObject() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setUserObject(Object userObject) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public MAPApplicationContext getApplicationContext() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getMaxUserDataLength() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getMessageUserDataLengthOnSend() throws MAPException {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getMessageUserDataLengthOnClose(boolean prearrangedEnd) throws MAPException {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void addEricssonData(IMSI imsi, AddressString vlrNo) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Long addForwardShortMessageRequest(SM_RP_DA sm_RP_DA, SM_RP_OA sm_RP_OA, SmsSignalInfo sm_RP_UI, boolean moreMessagesToSend) throws MAPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Long addForwardShortMessageRequest(int customInvokeTimeout, SM_RP_DA sm_RP_DA, SM_RP_OA sm_RP_OA, SmsSignalInfo sm_RP_UI,
				boolean moreMessagesToSend) throws MAPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addForwardShortMessageResponse(long invokeId) throws MAPException {
			responseCount++;
		}

		@Override
		public Long addMoForwardShortMessageRequest(SM_RP_DA sm_RP_DA, SM_RP_OA sm_RP_OA, SmsSignalInfo sm_RP_UI, MAPExtensionContainer extensionContainer,
				IMSI imsi) throws MAPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Long addMoForwardShortMessageRequest(int customInvokeTimeout, SM_RP_DA sm_RP_DA, SM_RP_OA sm_RP_OA, SmsSignalInfo sm_RP_UI,
				MAPExtensionContainer extensionContainer, IMSI imsi) throws MAPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addMoForwardShortMessageResponse(long invokeId, SmsSignalInfo sm_RP_UI, MAPExtensionContainer extensionContainer) throws MAPException {
			responseCount++;
		}

		@Override
		public Long addMtForwardShortMessageRequest(SM_RP_DA sm_RP_DA, SM_RP_OA sm_RP_OA, SmsSignalInfo sm_RP_UI, boolean moreMessagesToSend,
				MAPExtensionContainer extensionContainer) throws MAPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Long addMtForwardShortMessageRequest(int customInvokeTimeout, SM_RP_DA sm_RP_DA, SM_RP_OA sm_RP_OA, SmsSignalInfo sm_RP_UI,
				boolean moreMessagesToSend, MAPExtensionContainer extensionContainer) throws MAPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addMtForwardShortMessageResponse(long invokeId, SmsSignalInfo sm_RP_UI, MAPExtensionContainer extensionContainer) throws MAPException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Long addSendRoutingInfoForSMRequest(ISDNAddressString msisdn, boolean sm_RP_PRI, AddressString serviceCentreAddress,
				MAPExtensionContainer extensionContainer, boolean gprsSupportIndicator, SM_RP_MTI sM_RP_MTI, SM_RP_SMEA sM_RP_SMEA, TeleserviceCode teleservice)
				throws MAPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Long addSendRoutingInfoForSMRequest(int customInvokeTimeout, ISDNAddressString msisdn, boolean sm_RP_PRI, AddressString serviceCentreAddress,
				MAPExtensionContainer extensionContainer, boolean gprsSupportIndicator, SM_RP_MTI sM_RP_MTI, SM_RP_SMEA sM_RP_SMEA, TeleserviceCode teleservice)
				throws MAPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addSendRoutingInfoForSMResponse(long invokeId, IMSI imsi, LocationInfoWithLMSI locationInfoWithLMSI,
				MAPExtensionContainer extensionContainer, Boolean mwdSet) throws MAPException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Long addReportSMDeliveryStatusRequest(ISDNAddressString msisdn, AddressString serviceCentreAddress, SMDeliveryOutcome sMDeliveryOutcome,
				Integer absentSubscriberDiagnosticSM, MAPExtensionContainer extensionContainer, boolean gprsSupportIndicator, boolean deliveryOutcomeIndicator,
				SMDeliveryOutcome additionalSMDeliveryOutcome, Integer additionalAbsentSubscriberDiagnosticSM) throws MAPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Long addReportSMDeliveryStatusRequest(int customInvokeTimeout, ISDNAddressString msisdn, AddressString serviceCentreAddress,
				SMDeliveryOutcome sMDeliveryOutcome, Integer absentSubscriberDiagnosticSM, MAPExtensionContainer extensionContainer,
				boolean gprsSupportIndicator, boolean deliveryOutcomeIndicator, SMDeliveryOutcome additionalSMDeliveryOutcome,
				Integer additionalAbsentSubscriberDiagnosticSM) throws MAPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addReportSMDeliveryStatusResponse(long invokeId, ISDNAddressString storedMSISDN, MAPExtensionContainer extensionContainer)
				throws MAPException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Long addInformServiceCentreRequest(ISDNAddressString storedMSISDN, MWStatus mwStatus, MAPExtensionContainer extensionContainer,
				Integer absentSubscriberDiagnosticSM, Integer additionalAbsentSubscriberDiagnosticSM) throws MAPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Long addInformServiceCentreRequest(int customInvokeTimeout, ISDNAddressString storedMSISDN, MWStatus mwStatus,
				MAPExtensionContainer extensionContainer, Integer absentSubscriberDiagnosticSM, Integer additionalAbsentSubscriberDiagnosticSM)
				throws MAPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Long addAlertServiceCentreRequest(ISDNAddressString msisdn, AddressString serviceCentreAddress) throws MAPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Long addAlertServiceCentreRequest(int customInvokeTimeout, ISDNAddressString msisdn, AddressString serviceCentreAddress) throws MAPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addAlertServiceCentreResponse(long invokeId) throws MAPException {
			// TODO Auto-generated method stub
			
		}
	}

	private class MAPServiceBaseProxy implements MAPServiceBase {

		private MAPProviderProxy mapProvider = new MAPProviderProxy();

		@Override
		public MAPProvider getMAPProvider() {
			return mapProvider;
		}

		@Override
		public MAPDialog createNewDialog(MAPApplicationContext appCntx, SccpAddress origAddress, AddressString origReference, SccpAddress destAddress,
				AddressString destReference) throws MAPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ServingCheckData isServingService(MAPApplicationContext dialogApplicationContext) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isActivated() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void acivate() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void deactivate() {
			// TODO Auto-generated method stub
			
		}
	}

	private class MAPProviderProxy implements MAPProvider {

		@Override
		public void addMAPDialogListener(MAPDialogListener mapDialogListener) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void removeMAPDialogListener(MAPDialogListener mapDialogListener) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public MAPParameterFactory getMAPParameterFactory() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MAPErrorMessageFactory getMAPErrorMessageFactory() {
			return new MAPErrorMessageFactoryImpl();
		}

		@Override
		public MAPDialog getMAPDialog(Long dialogId) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MAPSmsTpduParameterFactory getMAPSmsTpduParameterFactory() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MAPServiceMobility getMAPServiceMobility() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MAPServiceCallHandling getMAPServiceCallHandling() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MAPServiceOam getMAPServiceOam() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MAPServicePdpContextActivation getMAPServicePdpContextActivation() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MAPServiceSupplementary getMAPServiceSupplementary() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MAPServiceSms getMAPServiceSms() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MAPServiceLsm getMAPServiceLsm() {
			// TODO Auto-generated method stub
			return null;
		}

	}
}
