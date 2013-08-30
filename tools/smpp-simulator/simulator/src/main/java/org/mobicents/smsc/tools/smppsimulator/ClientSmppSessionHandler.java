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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;

import org.mobicents.protocols.ss7.map.api.smstpdu.CharacterSet;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;

import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseSm;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class ClientSmppSessionHandler extends DefaultSmppSessionHandler {

    private SmppTestingForm testingForm;

    public ClientSmppSessionHandler(SmppTestingForm testingForm) {
        this.testingForm = testingForm;
    }

    @Override
    public void fireChannelUnexpectedlyClosed() {
        testingForm.addMessage("ChannelUnexpectedlyClosed", "SMPP channel unexpectedly closed by a peer or by TCP connection dropped");

        testingForm.doStop();
    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {
        testingForm.addMessage("PduRequestReceived: " + pduRequest.getName(), pduRequest.toString());

        PduResponse resp = pduRequest.createResponse();

        // here we can insert responses
        if (pduRequest.getCommandId() == SmppConstants.CMD_ID_DELIVER_SM || pduRequest.getCommandId() == SmppConstants.CMD_ID_DATA_SM
                || pduRequest.getCommandId() == SmppConstants.CMD_ID_SUBMIT_SM) {

            if (pduRequest instanceof BaseSm) {
                BaseSm dev = (BaseSm) pduRequest;
                String s;
                byte[] msg = dev.getShortMessage();
                DataCodingScheme dcs = new DataCodingSchemeImpl(dev.getDataCoding());
                if (dcs.getCharacterSet() == CharacterSet.UCS2) {
                    boolean udhPresent = (dev.getEsmClass() & SmppConstants.ESM_CLASS_UDHI_MASK) != 0;
                    // if (udhPresent) {
                    // Charset ucs2Charset = Charset.forName("UTF-16BE");
                    // ByteBuffer bb = ByteBuffer.wrap(msg);
                    // CharBuffer cb = ucs2Charset.decode(bb);
                    // s = cb.toString();
                    // } else {

                    Charset utf8Charset;
                    if (this.testingForm.getSmppSimulatorParameters().getSmppEncodingForUCS2() == 0) {
                        utf8Charset = Charset.forName("UTF-8");
                    } else {
                        utf8Charset = Charset.forName("UTF-16BE");
                    }
                    ByteBuffer bb = ByteBuffer.wrap(msg);
                    CharBuffer cb = utf8Charset.decode(bb);
                    s = cb.toString();
                    // }
                } else {
                    s = new String(msg);
                }
                testingForm.addMessage("TextReceived: ", s);
            }

            if (this.testingForm.getSmppSimulatorParameters().isRejectIncomingDeliveryMessage()) {
                resp.setCommandStatus(1);
            }

            testingForm.addMessage("PduResponseSent: " + resp.getName(), resp.toString());
        }

        return resp;
    }

    @Override
    public void firePduRequestExpired(PduRequest pduRequest) {
        testingForm.addMessage("PduRequestExpired: " + pduRequest.getName(), pduRequest.toString());
    }

    @Override
    public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
        this.testingForm.responsesRcvd.incrementAndGet();
        if (this.testingForm.timer == null) {
            testingForm.addMessage("Response=" + pduAsyncResponse.getResponse().getName(), "Req: " + pduAsyncResponse.getRequest().toString() + "\nResp: "
                    + pduAsyncResponse.getResponse().toString());
        }
    }

    @Override
    public void fireUnexpectedPduResponseReceived(PduResponse pduResponse) {
        testingForm.addMessage("UnexpectedPduResponseReceived: " + pduResponse.getName(), pduResponse.toString());
    }

    @Override
    public void fireUnrecoverablePduException(UnrecoverablePduException e) {
        testingForm.addMessage("UnrecoverablePduException", e.toString());

        testingForm.doStop();
    }

    @Override
    public void fireRecoverablePduException(RecoverablePduException e) {
        testingForm.addMessage("RecoverablePduException", e.toString());
    }

    @Override
    public void fireUnknownThrowable(Throwable t) {
        if (t instanceof ClosedChannelException) {
            testingForm.addMessage("UnknownThrowable",
                    "Unknown throwable received, but it was a ClosedChannelException, calling fireChannelUnexpectedlyClosed instead");
            fireChannelUnexpectedlyClosed();
        } else {
            testingForm.addMessage("UnknownThrowable", t.toString());

            testingForm.doStop();
        }
    }

    // @Override
    // public String lookupResultMessage(int commandStatus) {
    // return null;
    // }
    //
    // @Override
    // public String lookupTlvTagName(short tag) {
    // return null;
    // }

}
