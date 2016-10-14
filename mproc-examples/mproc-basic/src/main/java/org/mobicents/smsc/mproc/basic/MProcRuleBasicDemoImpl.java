/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
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

package org.mobicents.smsc.mproc.basic;
import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.mobicents.smsc.mproc.DeliveryReceiptData;
import org.mobicents.smsc.mproc.MProcMessage;
import org.mobicents.smsc.mproc.MProcNewMessage;
import org.mobicents.smsc.mproc.MProcRuleBaseImpl;
import org.mobicents.smsc.mproc.PostArrivalProcessor;


public class MProcRuleBasicDemoImpl extends MProcRuleBaseImpl {

	private static final String VAL1 = "value1";
	private static final String VAL2 = "value2";

    private static final Logger logger = Logger.getLogger(MProcRuleBasicDemoImpl.class);
    private String value1 = "1";
    private String value2 = "2";

	@Override
	public String getRuleClassName() {
		return MProcRuleBasicDemoFactImpl.CLASS_NAME;
	}

	@Override
	public void setInitialRuleParameters(String parametersString) throws Exception {
		logger.info("setInitialRuleParameter is called");
	}

	@Override
	public void updateRuleParameters(String parametersString) throws Exception {
		logger.info("updateRuleParameters is called");
	}

	@Override
	public String getRuleParameters() {
		return "getRuleParameters is called";
	}


	@Override
	public boolean isForPostArrivalState() {
		return true;
	}


	@Override
	public boolean matchesPostArrival(MProcMessage message) {
			return false;  // do nothing
	}

	@Override
	public boolean matchesPostImsiRequest(MProcMessage message) {
		return false;
	}

	@Override
	public boolean matchesPostDelivery(MProcMessage message) {
		return false;
	}

	@Override
	public void onPostArrival(PostArrivalProcessor factory, MProcMessage message) throws Exception {
		factory.updateMessageDestAddr(message, "1234");
		logger.info("onPostArrival is called");
	}

	/**
	 * XML Serialization/Deserialization
	 */
	protected static final XMLFormat<MProcRuleBasicDemoImpl> M_PROC_RULE_TEST_XML = new XMLFormat<MProcRuleBasicDemoImpl>(
			MProcRuleBasicDemoImpl.class) {

		@Override
		public void read(javolution.xml.XMLFormat.InputElement xml, MProcRuleBasicDemoImpl mProcRule) throws XMLStreamException {
			M_PROC_RULE_BASE_XML.read(xml, mProcRule);

			mProcRule.value1 = xml.getAttribute(VAL1, "");
			mProcRule.value2 = xml.getAttribute(VAL2, "");
		}

		@Override
		public void write(MProcRuleBasicDemoImpl mProcRule, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
			M_PROC_RULE_BASE_XML.write(mProcRule, xml);

			xml.setAttribute(VAL1, mProcRule.value1);
			xml.setAttribute(VAL2, mProcRule.value2);
		}
	};

}
