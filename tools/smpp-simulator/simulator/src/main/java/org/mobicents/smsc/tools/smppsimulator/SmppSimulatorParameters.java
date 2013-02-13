/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * TeleStax and individual contributors
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

package org.mobicents.smsc.tools.smppsimulator;

import com.cloudhopper.smpp.SmppBindType;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmppSimulatorParameters {

	private int windowSize = 1;
	private SmppBindType bindType = SmppBindType.TRANSCEIVER;
	private String host = "127.0.0.1";
	private int port = 2776;

	public int getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(int val) {
		windowSize = val;
	}

	public SmppBindType getBindType() {
		return bindType;
	}

	public void setBindType(SmppBindType val) {
		bindType = val;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String val) {
		host = val;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int val) {
		port = val;
	}

}

