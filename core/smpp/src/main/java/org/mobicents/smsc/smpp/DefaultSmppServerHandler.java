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
package org.mobicents.smsc.smpp;

import org.apache.log4j.Logger;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.SmppProcessingException;

/**
 * 
 * @author Amit Bhayani
 * 
 */
public class DefaultSmppServerHandler implements SmppServerHandler {

	private static final Logger logger = Logger.getLogger(DefaultSmppServerHandler.class);

	private final SmppSessionHandlerInterface smppSessionHandlerInterface;

	private final EsmeManagement esmeManagement;

	public DefaultSmppServerHandler(EsmeManagement esmeManagement,
			SmppSessionHandlerInterface smppSessionHandlerInterface) {
		this.esmeManagement = esmeManagement;
		this.smppSessionHandlerInterface = smppSessionHandlerInterface;
	}

	@Override
	public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration,
			final BaseBind bindRequest) throws SmppProcessingException {

		if (this.smppSessionHandlerInterface == null) {
			logger.error("Received BIND request but no SmppSessionHandlerInterface registered yet! Will close SmppServerSession");
			throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL);
		}

		Esme esme = this.esmeManagement.getEsmeByPrimaryKey(bindRequest.getSystemId(), sessionConfiguration.getHost(),
				sessionConfiguration.getPort());

		if (esme == null) {
			logger.error(String.format("Received BIND request but no ESME configured for SystemId=%s Host=%s Port=%d",
					bindRequest.getSystemId(), sessionConfiguration.getHost(), sessionConfiguration.getPort()));
			throw new SmppProcessingException(SmppConstants.STATUS_INVSYSID);
		}

		if (!esme.isStarted()) {
			logger.error(String.format("Received BIND request but ESME is not yet started for name %s", esme.getName()));
			throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL);
		}

		if (!esme.getStateName().equals(com.cloudhopper.smpp.SmppSession.STATES[SmppSession.STATE_CLOSED])) {
			logger.error(String.format(
					"Received BIND request but ESME Already in Bound State Name=%s SystemId=%s Host=%s Port=%d",
					esme.getName(), bindRequest.getSystemId(), esme.getHost(), esme.getPort()));
			throw new SmppProcessingException(SmppConstants.STATUS_ALYBND);
		}

		if (!(esme.getPassword().equals(bindRequest.getPassword()))) {
			logger.error(String.format("Received BIND request but invalid password for SystemId=%s",
					bindRequest.getSystemId()));
			throw new SmppProcessingException(SmppConstants.STATUS_INVPASWD);
		}

		// Check of BIND is correct?
		if ((bindRequest.getCommandId() == SmppConstants.CMD_ID_BIND_RECEIVER)
				&& esme.getSmppBindType() != SmppBindType.RECEIVER) {
			logger.error(String.format("Received BIND_RECEIVER for SystemId=%s but configured=%s",
					bindRequest.getSystemId(), esme.getSmppBindType()));
			throw new SmppProcessingException(SmppConstants.STATUS_INVBNDSTS);
		} else if ((bindRequest.getCommandId() == SmppConstants.CMD_ID_BIND_TRANSMITTER)
				&& esme.getSmppBindType() != SmppBindType.TRANSMITTER) {
			logger.error(String.format("Received BIND_TRANSMITTER for SystemId=%s but configured=%s",
					bindRequest.getSystemId(), esme.getSmppBindType()));
			throw new SmppProcessingException(SmppConstants.STATUS_INVBNDSTS);
		} else if ((bindRequest.getCommandId() == SmppConstants.CMD_ID_BIND_TRANSCEIVER)
				&& esme.getSmppBindType() != SmppBindType.TRANSCEIVER) {
			logger.error(String.format("Received BIND_TRANSCEIVER for SystemId=%s but configured=%s",
					bindRequest.getSystemId(), esme.getSmppBindType()));
			throw new SmppProcessingException(SmppConstants.STATUS_INVBNDSTS);
		}

		// Check if TON, NPI and Address Range matches
		Address esmeAddressRange = esme.getAddress();
		Address bindRequestAddressRange = bindRequest.getAddressRange();

		if (esmeAddressRange.getTon() != bindRequestAddressRange.getTon()) {
			logger.error(String.format("Received BIND request with TON=%d but configured TON=%d",
					bindRequestAddressRange.getTon(), esmeAddressRange.getTon()));
			throw new SmppProcessingException(SmppConstants.STATUS_INVBNDSTS);
		}

		if (esmeAddressRange.getNpi() != bindRequestAddressRange.getNpi()) {
			logger.error(String.format("Received BIND request with NPI=%d but configured NPI=%d",
					bindRequestAddressRange.getNpi(), esmeAddressRange.getNpi()));
			throw new SmppProcessingException(SmppConstants.STATUS_INVBNDSTS);
		}

		// TODO : we are checking with empty String, is this correct?

		if (bindRequestAddressRange.getAddress() == null || bindRequestAddressRange.getAddress() == "") {
			// If ESME doesn't know we set it up from our config
			bindRequestAddressRange.setAddress(esmeAddressRange.getAddress());
		} else if (!bindRequestAddressRange.getAddress().equals(esmeAddressRange.getAddress())) {
			logger.error(String.format("Received BIND request with Address_Range=%s but configured Address_Range=%s",
					bindRequestAddressRange.getAddress(), esmeAddressRange.getAddress()));
			throw new SmppProcessingException(SmppConstants.STATUS_INVBNDSTS);
		}

		sessionConfiguration.setAddressRange(bindRequestAddressRange);

		sessionConfiguration.setCountersEnabled(esme.isCountersEnabled());

		// TODO More parameters to compare

		// test name change of sessions
		// this name actually shows up as thread context....
		sessionConfiguration.setName(esme.getName());

		// throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL,
		// null);
	}

	@Override
	public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse)
			throws SmppProcessingException {
		if (logger.isInfoEnabled()) {
			logger.info(String.format("Session created: %s", session.getConfiguration().getSystemId()));
		}

		if (this.smppSessionHandlerInterface == null) {
			logger.error("No SmppSessionHandlerInterface registered yet! Will close SmppServerSession");
			throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL);
		}

		SmppSessionConfiguration sessionConfiguration = session.getConfiguration();

		Esme esme = this.esmeManagement.getEsmeByPrimaryKey(sessionConfiguration.getSystemId(),
				sessionConfiguration.getHost(), sessionConfiguration.getPort());

		if (esme == null) {
			logger.error(String.format("No ESME for SystemId=% Host=%s Port=%d", sessionConfiguration.getSystemId(),
					sessionConfiguration.getHost(), sessionConfiguration.getPort()));
			throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL);
		}

		esme.setSmppSession((DefaultSmppSession) session);

		if (!logger.isDebugEnabled()) {
			session.getConfiguration().getLoggingOptions().setLogBytes(false);
			session.getConfiguration().getLoggingOptions().setLogPdu(false);
		}

		SmppSessionHandler smppSessionHandler = this.smppSessionHandlerInterface.createNewSmppSessionHandler(esme);
		// need to do something it now (flag we're ready)
		session.serverReady(smppSessionHandler);
	}

	@Override
	public void sessionDestroyed(Long sessionId, SmppServerSession session) {
		this.sessionDestroyed(session);
	}

	public void sessionDestroyed(SmppSession session) {
		if (logger.isInfoEnabled()) {
			logger.info(String.format("Session destroyed: %s", session.getConfiguration().getSystemId()));
		}

		// print out final stats
		if (session.hasCounters()) {
			logger.info(String.format("final session rx-submitSM: %s", session.getCounters().getRxSubmitSM()));
		}

		// make sure it's really shutdown
		session.destroy();
	}
}
