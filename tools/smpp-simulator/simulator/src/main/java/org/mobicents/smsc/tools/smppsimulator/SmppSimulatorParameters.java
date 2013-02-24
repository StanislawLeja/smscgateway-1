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
	private long connectTimeout = 10000;
	private String systemId = "test";
	private String password = "test";
	private long requestExpiryTimeout = 30000;
	private long windowMonitorInterval = 15000;

	private String messageText = "Hello!";
	private EncodingType encodingType = EncodingType.GSM7;
	private SplittingType splittingType = SplittingType.DoNotSplit;

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

    public void setConnectTimeout(long value) {
        this.connectTimeout = value;
    }

    public long getConnectTimeout() {
        return this.connectTimeout;
    }

    public void setSystemId(String value) {
        this.systemId = value;
    }

    public String getSystemId() {
        return this.systemId;
    }

    public void setPassword(String value) {
        this.password = value;
    }

    public String getPassword() {
        return this.password;
    }

    public long getRequestExpiryTimeout() {
        return requestExpiryTimeout;
    }

    /**
     * Set the amount of time to wait for an endpoint to respond to
     * a request before it expires. Defaults to disabled (-1).
     * @param requestExpiryTimeout  The amount of time to wait (in ms) before
     *      an unacknowledged request expires.  -1 disables.
     */
    public void setRequestExpiryTimeout(long requestExpiryTimeout) {
        this.requestExpiryTimeout = requestExpiryTimeout;
    }

    public long getWindowMonitorInterval() {
        return windowMonitorInterval;
    }

    /**
     * Sets the amount of time between executions of monitoring the window
     * for requests that expire.  It's recommended that this generally either
     * matches or is half the value of requestExpiryTimeout.  Therefore, at worst
     * a request would could take up 1.5X the requestExpiryTimeout to clear out.
     * @param windowMonitorInterval The amount of time to wait (in ms) between
     *      executions of monitoring the window.
     */
    public void setWindowMonitorInterval(long windowMonitorInterval) {
        this.windowMonitorInterval = windowMonitorInterval;
    }

    
    

    public String getMessageText() {
        return this.messageText;
    }

    public void setMessageText(String value) {
        this.messageText = value;
    }

	public EncodingType getEncodingType() {
		return encodingType;
	}

	public void setEncodingType(EncodingType val) {
		encodingType = val;
	}

	public SplittingType getSplittingType() {
		return splittingType;
	}

	public void setSplittingType(SplittingType val) {
		splittingType = val;
	}


    public enum EncodingType {
    	GSM7, UCS2,
    }

    public enum SplittingType {
    	DoNotSplit, SplitWithParameters, SplitWithUdh,
    }
}

