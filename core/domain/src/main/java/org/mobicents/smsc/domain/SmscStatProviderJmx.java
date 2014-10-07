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

package org.mobicents.smsc.domain;

import java.util.Date;

import org.apache.log4j.Logger;

import javolution.util.FastMap;

import com.telscale.protocols.ss7.oam.common.jmx.MBeanHost;
import com.telscale.protocols.ss7.oam.common.jmx.MBeanType;
import com.telscale.protocols.ss7.oam.common.jmxss7.Ss7Layer;
import com.telscale.protocols.ss7.oam.common.statistics.CounterDefImpl;
import com.telscale.protocols.ss7.oam.common.statistics.CounterDefSetImpl;
import com.telscale.protocols.ss7.oam.common.statistics.SourceValueCounterImpl;
import com.telscale.protocols.ss7.oam.common.statistics.SourceValueObjectImpl;
import com.telscale.protocols.ss7.oam.common.statistics.SourceValueSetImpl;
import com.telscale.protocols.ss7.oam.common.statistics.api.CounterDef;
import com.telscale.protocols.ss7.oam.common.statistics.api.CounterDefSet;
import com.telscale.protocols.ss7.oam.common.statistics.api.CounterMediator;
import com.telscale.protocols.ss7.oam.common.statistics.api.CounterType;
import com.telscale.protocols.ss7.oam.common.statistics.api.SourceValueSet;

/**
*
* @author sergey vetyutnev
*
*/
public class SmscStatProviderJmx implements SmscStatProviderJmxMBean, CounterMediator {

    protected final Logger logger;

    private final MBeanHost ss7Management;
    private final SmscStatAggregator smscStatAggregator = SmscStatAggregator.getInstance();

    private FastMap<String, CounterDefSet> lstCounters = new FastMap<String, CounterDefSet>();

    public SmscStatProviderJmx(MBeanHost ss7Management) {
        this.ss7Management = ss7Management;

        this.logger = Logger.getLogger(SmscStatProviderJmx.class.getCanonicalName() + "-" + getName());
    }

    /**
     * methods - bean life-cycle
     */

    public void start() throws Exception {
        logger.info("Starting ...");

        setupCounterList();

        this.ss7Management.registerMBean(Ss7Layer.SMSC_GW, SmscManagementType.MANAGEMENT, this.getName(), this);

        logger.info("Started ...");
    }

    public void stop() {
        logger.info("Stopping ...");
        logger.info("Stopped ...");
    }

    public String getName() {
        return "SMSC";
    }

    private void setupCounterList() {
        FastMap<String, CounterDefSet> lst = new FastMap<String, CounterDefSet>();

        CounterDefSetImpl cds = new CounterDefSetImpl(this.getCounterMediatorName() + "-Main");
        lst.put(cds.getName(), cds);

        CounterDef cd = new CounterDefImpl(CounterType.Minimal, "MinMessagesInProcess", "A min count of messages that are in progress during a period");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Maximal, "MaxMessagesInProcess", "A max count of messages that are in progress during a period");
        cds.addCounterDef(cd);

        cd = new CounterDefImpl(CounterType.Summary, "MsgInReceivedAll", "Messages received and accepted via all interfaces");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgInRejectedAll", "Messages received and rejected because of charging reject via all interfaces");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgInFailedAll", "Messages received and failed to process via all interfaces");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgInReceivedSs7", "Messages received and accepted via SS7 interface");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgInReceivedSmpp", "Messages received and accepted via SMPP interface");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgInReceivedSip", "Messages received and accepted via SIP interface");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary_Cumulative, "MsgInReceivedAllCumulative", "Messages received and accepted via all interfaces cumulative");
        cds.addCounterDef(cd);

        cd = new CounterDefImpl(CounterType.Summary, "MsgOutTryAll", "Messages sending tries via all interfaces");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgOutSentAll", "Messages sent via all interfaces");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary_Cumulative, "MsgOutTryAllCumulative", "Messages sending tries via all interfaces cumulative");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary_Cumulative, "MsgOutSentAllCumulative", "Messages sent via all interfaces cumulative");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgOutFailedAll", "Messages failed to send via all interfaces");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Average, "MsgOutTryAllPerSec", "Messages sending tries via all interfaces per second");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Average, "MsgOutSentAllPerSec", "Messages sent via all interfaces per second");
        cds.addCounterDef(cd);

        cd = new CounterDefImpl(CounterType.Summary, "MsgOutTrySs7", "Messages sending tries via SS7 interface");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgOutSentSs7", "Messages sent via SS7 interface");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgOutTrySmpp", "Messages sending tries via SMPP interface");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgOutSentSmpp", "Messages sent via SMPP interface");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgOutTrySip", "Messages sending tries via SIP interface");
        cds.addCounterDef(cd);
        cd = new CounterDefImpl(CounterType.Summary, "MsgOutSentSip", "Messages sent via SIP interface");
        cds.addCounterDef(cd);

        cd = new CounterDefImpl(CounterType.Summary_Cumulative, "SmscDeliveringLag", "Lag of delivering messages by Smsc (in seconds)");
        cds.addCounterDef(cd);

        lstCounters = lst;
    }

    @Override
    public CounterDefSet getCounterDefSet(String counterDefSetName) {
        return lstCounters.get(counterDefSetName);
    }

    @Override
    public String[] getCounterDefSetList() {
        String[] res = new String[lstCounters.size()];
        lstCounters.keySet().toArray(res);
        return res;
    }

    @Override
    public String getCounterMediatorName() {
        return "SMSC GW-" + this.getName();
    }

    @Override
    public SourceValueSet getSourceValueSet(String counterDefSetName, String campaignName, int durationInSeconds) {

        if (durationInSeconds >= 60)
            logger.info("getSourceValueSet() - starting - campaignName=" + campaignName);
        else
            logger.debug("getSourceValueSet() - starting - campaignName=" + campaignName);

        long curTimeSeconds = new Date().getTime() / 1000;
        
        SourceValueSetImpl svs;
        try {
            String[] csl = this.getCounterDefSetList();
            if (!csl[0].equals(counterDefSetName))
                return null;

            svs = new SourceValueSetImpl(smscStatAggregator.getSessionId());

            CounterDefSet cds = getCounterDefSet(counterDefSetName);
            for (CounterDef cd : cds.getCounterDefs()) {
                SourceValueCounterImpl scs = new SourceValueCounterImpl(cd);

                SourceValueObjectImpl svo = null;
                if (cd.getCounterName().equals("MsgInReceivedAll")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInReceivedAll());
                } else if (cd.getCounterName().equals("MsgInRejectedAll")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInRejectedAll());
                } else if (cd.getCounterName().equals("MsgInFailedAll")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInFailedAll());
                } else if (cd.getCounterName().equals("MsgInReceivedSs7")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInReceivedSs7());
                } else if (cd.getCounterName().equals("MsgInReceivedSmpp")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInReceivedSmpp());
                } else if (cd.getCounterName().equals("MsgInReceivedSip")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInReceivedSip());
                } else if (cd.getCounterName().equals("MsgInReceivedAllCumulative")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgInReceivedAllCumulative());

                } else if (cd.getCounterName().equals("MsgOutTryAll")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutTryAll());
                } else if (cd.getCounterName().equals("MsgOutSentAll")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutSentAll());
                } else if (cd.getCounterName().equals("MsgOutTryAllCumulative")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutTryAllCumulative());
                } else if (cd.getCounterName().equals("MsgOutSentAllCumulative")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutSentAllCumulative());
                } else if (cd.getCounterName().equals("MsgOutFailedAll")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutFailedAll());

                } else if (cd.getCounterName().equals("MsgOutTryAllPerSec")) {
                    long cnt = smscStatAggregator.getMsgOutTryAll();
                    svo = new SourceValueObjectImpl(this.getName(), 0);
                    svo.setValueA(cnt);
                    svo.setValueB(curTimeSeconds);
                } else if (cd.getCounterName().equals("MsgOutSentAllPerSec")) {
                    long cnt = smscStatAggregator.getMsgOutSentAll();
                    svo = new SourceValueObjectImpl(this.getName(), 0);
                    svo.setValueA(cnt);
                    svo.setValueB(curTimeSeconds);

                } else if (cd.getCounterName().equals("MsgOutTrySs7")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutTrySs7());
                } else if (cd.getCounterName().equals("MsgOutSentSs7")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutSentSs7());
                } else if (cd.getCounterName().equals("MsgOutTrySmpp")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutTrySmpp());
                } else if (cd.getCounterName().equals("MsgOutSentSmpp")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutSentSmpp());
                } else if (cd.getCounterName().equals("MsgOutTrySip")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutTrySip());
                } else if (cd.getCounterName().equals("MsgOutSentSip")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getMsgOutSentSip());

                } else if (cd.getCounterName().equals("MinMessagesInProcess")) {
                    Long res = smscStatAggregator.getMinMessagesInProcess(campaignName);
                    if (res != null)
                        svo = new SourceValueObjectImpl(this.getName(), res);
                } else if (cd.getCounterName().equals("MaxMessagesInProcess")) {
                    Long res = smscStatAggregator.getMaxMessagesInProcess(campaignName);
                    if (res != null)
                        svo = new SourceValueObjectImpl(this.getName(), res);
                } else if (cd.getCounterName().equals("SmscDeliveringLag")) {
                    svo = new SourceValueObjectImpl(this.getName(), smscStatAggregator.getSmscDeliveringLag());
                }
                if (svo != null)
                    scs.addObject(svo);

                svs.addCounter(scs);
            }
        } catch (Throwable e) {
            logger.info("Exception when getSourceValueSet() - campaignName=" + campaignName + " - " + e.getMessage(), e);
            return null;
        }

        if (durationInSeconds >= 60)
            logger.info("getSourceValueSet() - return value - campaignName=" + campaignName);
        else
            logger.debug("getSourceValueSet() - return value - campaignName=" + campaignName);

        return svs;
    }



    public enum SmscManagementType implements MBeanType {
        MANAGEMENT("Management");

        private final String name;

        public static final String NAME_MANAGEMENT = "Management";

        private SmscManagementType(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static SmscManagementType getInstance(String name) {
            if (NAME_MANAGEMENT.equals(name)) {
                return MANAGEMENT;
            }

            return null;
        }
    }

}
