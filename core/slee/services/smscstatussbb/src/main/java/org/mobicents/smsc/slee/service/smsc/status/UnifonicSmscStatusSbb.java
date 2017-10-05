/*
 * Telestax, Open Source Cloud Communications Copyright 2011-2017,
 * Telestax Inc and individual contributors by the @authors tag.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
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
package org.mobicents.smsc.slee.service.smsc.status;

import net.java.slee.resource.http.events.HttpServletRequestEvent;

import javax.management.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.slee.*;
import javax.slee.facilities.FacilityException;
import javax.slee.facilities.Tracer;
import javax.slee.serviceactivity.ServiceStartedEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;


/**
 * The Class UnifonicSmscStatusSbb.
 */
public abstract class UnifonicSmscStatusSbb implements Sbb {

    private static final String TRACER_NAME_MAIN = "main";

    private static final String GET = "GET";
    private static final String STATUS_QUERY = "statusQuery";
    private static final String OBJECT_NAME_STATISTICS = "org.mobicents.smsc:layer=SmscStats,name=SmscManagement";
    private static final String OBJECT_NAME_SMSC_PROPERTIES = "org.mobicents.smsc:layer=SmscPropertiesManagement,name=SmscManagement";
    private static final String ATTRIBUTE_MESSAGE_IN_PROGRESS = "MessageInProcess";
    private static final String ATTRIBUTE_MAX_ACTIVITY_COUNT = "MaxActivityCount";

    private static final float THRESHOLD_YELLOW = 0.75f;
    private static final float THRESHOLD_RED = 0.95f;

    private static final String ERROR = "ERROR";
    private static final String GREEN = "GREEN";
    private static final String YELLOW = "YELLOW";
    private static final String RED = "RED";

    private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";

    private Tracer itsTracer;
    private SbbContext itsContext;

    @Override
    public void setSbbContext(final SbbContext aContext) {
        itsContext = aContext;
        itsTracer = aContext.getTracer(TRACER_NAME_MAIN);
        if (itsTracer.isFinestEnabled()) {
            itsTracer.finest("SBB context set.");
        }
    }

    /**
     * Checks if is initial HTTP request event.
     *
     * @param aSelector the selector
     * @return the initial event selector
     */
    public InitialEventSelector isInitialHttpRequestEvent(final InitialEventSelector aSelector) {
        if (itsTracer.isFinestEnabled()) {
            itsTracer.finest("Incomming HTTP event: " + aSelector.getEvent() + ".");
        }
        final Object event = aSelector.getEvent();
        if (!(event instanceof HttpServletRequestEvent)) {
            aSelector.setInitialEvent(false);
            if (itsTracer.isFinestEnabled()) {
                itsTracer.finest("Not an initial event for us (unsupported event type).");
            }
            return aSelector;
        }
        final HttpServletRequest request = ((HttpServletRequestEvent) event).getRequest();
        if (!request.getMethod().equals(GET)) {
            aSelector.setInitialEvent(false);
            if (itsTracer.isFinestEnabled()) {
                itsTracer.finest("Not an initial event for us (unsupported request method).");
            }
            return aSelector;
        }
        final String requestURL = request.getRequestURL().toString();
        final String[] tmp = requestURL.split("\\?");
        if (tmp[0].endsWith(STATUS_QUERY)) {
            aSelector.setInitialEvent(true);
            return aSelector;
        }
        aSelector.setInitialEvent(false);
        if (itsTracer.isFinestEnabled()) {
            itsTracer.finest("Not an initial event for us (request URL).");
        }
        return aSelector;
    }

    /**
     * On service started event.
     *
     * @param anEvent the event
     * @param anActivityContextInterface the activity context interface
     * @param anEventContext the event context
     */
    public void onServiceStarted(final ServiceStartedEvent anEvent,
            final ActivityContextInterface anActivityContextInterface, final EventContext anEventContext) {
        itsTracer.info("Service started.");
        anActivityContextInterface.detach(itsContext.getSbbLocalObject());
    }

    /**
     * On HTTP GET.
     *
     * @param anEvent the event
     * @param anActivityContextInterface the activity context interface
     */
    public void onHttpGet(final HttpServletRequestEvent anEvent, ActivityContextInterface anActivityContextInterface) {
        if (itsTracer.isFineEnabled()) {
            itsTracer.fine("Received HTTP GET event: " + anEvent + ".");
        }
        try {
            final String s = getStatus();
            itsTracer.info("Determined status: " + s + ".");
            sendOkResponseWithContent(anEvent.getResponse(), s);
        } catch (FacilityException | MalformedObjectNameException | AttributeNotFoundException
                | InstanceNotFoundException | NullPointerException | MBeanException | ReflectionException e) {
            itsTracer.warning("Unable to determine SMSC load status. Message: " + e.getMessage() + ".", e);
            try {
                sendOkResponseWithContent(anEvent.getResponse(), ERROR + ":" + e.getMessage());
            } catch (IOException ioe) {
                itsTracer.warning("Unable to send SMSC error status. Message: " + ioe.getMessage() + ".", ioe);
            }
        } catch (IOException e) {
            itsTracer.warning("Unable to send SMSC load status. Message: " + e.getMessage() + ".", e);
        }
        anActivityContextInterface.detach(itsContext.getSbbLocalObject());
    }

    /**
     * On activity end.
     *
     * @param anEvent the event
     * @param anActivityContextInterface the activity context interface
     * @param anEventContext the event context
     */
    public void onActivityEnd(final ActivityEndEvent anEvent, final ActivityContextInterface anActivityContextInterface,
            final EventContext anEventContext) {
        itsTracer.info("Activity End event: " + anEvent + ".");
    }

    @Override
    public void unsetSbbContext() {
        if (itsTracer.isFinestEnabled()) {
            itsTracer.finest("SBB context unset.");
        }
    }

    @Override
    public void sbbCreate() {
        if (itsTracer.isFinestEnabled()) {
            itsTracer.finest("Sbb create.");
        }
    }

    @Override
    public void sbbPostCreate() {
        if (itsTracer.isFinestEnabled()) {
            itsTracer.finest("SBB post create.");
        }
    }

    @Override
    public void sbbActivate() {
        if (itsTracer.isFinestEnabled()) {
            itsTracer.finest("SBB activate.");
        }
    }

    @Override
    public void sbbPassivate() {
        if (itsTracer.isFinestEnabled()) {
            itsTracer.finest("SBB passivate.");
        }
    }

    @Override
    public void sbbLoad() {
        if (itsTracer.isFinestEnabled()) {
            itsTracer.finest("SBB load.");
        }
    }

    @Override
    public void sbbStore() {
        if (itsTracer.isFinestEnabled()) {
            itsTracer.finest("SBB store.");
        }
    }

    @Override
    public void sbbRemove() {
        if (itsTracer.isFinestEnabled()) {
            itsTracer.finest("SBB remove.");
        }
    }

    @Override
    public void sbbExceptionThrown(final Exception anException, final Object anEvent,
            final ActivityContextInterface anActivityContextInterface) {
        itsTracer.warning("SBB exception thrown. Message: " + anException.getMessage() + ". Event: " + anEvent
                + ". ACI: " + anActivityContextInterface + ".", anException);
    }

    @Override
    public void sbbRolledBack(final RolledBackContext aContext) {
        itsTracer.warning("SBB rolled back. Event: " + aContext.getEvent() + ". ACI: "
                + aContext.getActivityContextInterface() + ".");
    }

    private String getStatus() throws MalformedObjectNameException, AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException {
        final MBeanServer mbeans = ManagementFactory.getPlatformMBeanServer();
        final int mip = (Integer) mbeans.getAttribute(new ObjectName(OBJECT_NAME_STATISTICS),
                ATTRIBUTE_MESSAGE_IN_PROGRESS);
        final int mac = (Integer) mbeans.getAttribute(new ObjectName(OBJECT_NAME_SMSC_PROPERTIES),
                ATTRIBUTE_MAX_ACTIVITY_COUNT);
        final double level = ((double) mip) / ((double) mac);
        if (itsTracer.isFineEnabled()) {
            itsTracer.fine("MIP: " + mip + ". MAC: " + mac + ". Level: " + level + ".");
        }
        if (level > THRESHOLD_RED) {
            return RED;
        }
        if (level > THRESHOLD_YELLOW) {
            return YELLOW;
        }
        return GREEN;
    }

    private static void sendOkResponseWithContent(final HttpServletResponse aResponse, final String aContent)
            throws IOException {
        aResponse.setStatus(HttpServletResponse.SC_OK);
        aResponse.setContentType(CONTENT_TYPE_TEXT_PLAIN);
        final PrintWriter writer = aResponse.getWriter();
        writer.write(aContent + "\n");
        writer.flush();
        aResponse.flushBuffer();
    }

}
