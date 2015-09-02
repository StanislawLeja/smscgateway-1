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

package org.mobicents.smsc.library;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javolution.util.FastMap;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmsSetCache {

    public static int SMSSET_MSG_PRO_SEGMENT_LIMIT = 50;
    public static int SMSSET_FREE_SEGMENT_CNT = 100;

    private int processingSmsSetTimeout;
    private int correlationIdLiveTime;

    private boolean isStarted = false;

	private FastMap<TargetAddress, TargetAddressContainer> lstSmsSetUnderAtomicOper = new FastMap<TargetAddress, TargetAddressContainer>();

	private AtomicInteger activityCount = new AtomicInteger(0);

    private FastMap<String, SmsSet> lstSmsSetInProcessing = new FastMap<String, SmsSet>();
    private FastMap<String, SmsSet> lstSmsSetWithBigMessageCount = new FastMap<String, SmsSet>();
    private UpdateMessagesInProcessListener smscStatAggregator;

    private FastMap<String, CorrelationIdValue> correlationIdCache1 = new FastMap<String, CorrelationIdValue>();
    private FastMap<String, CorrelationIdValue> correlationIdCache2 = new FastMap<String, CorrelationIdValue>();
    private Object correlationIdCacheSync = new Object();
    private ScheduledExecutorService executor;

	private static SmsSetCache singeltone;

	static {
		singeltone = new SmsSetCache();
	}

	private SmsSetCache() {
	}

	public static SmsSetCache getInstance() {
		return singeltone;
	}

    public static void start(int correlationIdLiveTime) {
        SmsSetCache ssc = SmsSetCache.getInstance();
        ssc.correlationIdLiveTime = correlationIdLiveTime;

        ssc.executor = Executors.newScheduledThreadPool(1);

        ssc.isStarted = true;

        CacheManTask t = ssc.new CacheManTask();
        ssc.executor.schedule(t, correlationIdLiveTime, TimeUnit.SECONDS);
    }

    public static void stop() {
        SmsSetCache ssc = SmsSetCache.getInstance();
        ssc.isStarted = false;

        ssc.executor.shutdown();
    }


    public void setUpdateMessagesInProcessListener(UpdateMessagesInProcessListener smscStatAggregator) {
        this.smscStatAggregator = smscStatAggregator;
    }

	public TargetAddress addSmsSet(TargetAddress ta) {
		synchronized (lstSmsSetUnderAtomicOper) {
			TargetAddressContainer cont = lstSmsSetUnderAtomicOper.get(ta);
			if (cont != null) {
				cont.count++;
				return cont.targetAddress;

			} else {
				cont = new TargetAddressContainer();
				lstSmsSetUnderAtomicOper.put(ta, cont);

				cont.count = 1;
				cont.targetAddress = ta;
				return ta;
			}
		}

	}

	public void removeSmsSet(TargetAddress ta) {
		synchronized (lstSmsSetUnderAtomicOper) {
			TargetAddressContainer cont = lstSmsSetUnderAtomicOper.get(ta);

            if (cont != null && --cont.count <= 0)
                lstSmsSetUnderAtomicOper.remove(ta);
		}
	}

	public void incrementActivityCount() {
		activityCount.incrementAndGet();
	}

	public void decrementActivityCount() {
		activityCount.decrementAndGet();
	}

	public int getActivityCount() {
		return activityCount.get();
	}

    public SmsSet getProcessingSmsSet(String targetId) {
        return lstSmsSetInProcessing.get(targetId);
    }

    public SmsSet addProcessingSmsSet(String targetId, SmsSet smsSet, int processingSmsSetTimeout) {
        this.processingSmsSetTimeout = processingSmsSetTimeout;

        synchronized (lstSmsSetInProcessing) {
            SmsSet res = lstSmsSetInProcessing.put(targetId, smsSet);
            if (smscStatAggregator != null) {
                smscStatAggregator.updateMaxMessagesInProcess(lstSmsSetInProcessing.size());
                smscStatAggregator.updateMinMessagesInProcess(lstSmsSetInProcessing.size());
            }
            return res;
        }
    }

    public void registerSmsSetWithBigMessageCount(String targetId, SmsSet smsSet) {
        synchronized (lstSmsSetInProcessing) {
            lstSmsSetWithBigMessageCount.put(targetId, smsSet);
        }
    }

    public SmsSet removeProcessingSmsSet(String targetId) {
        synchronized (lstSmsSetInProcessing) {
            SmsSet smsSet = lstSmsSetInProcessing.remove(targetId);
            lstSmsSetWithBigMessageCount.remove(targetId);

            if (smscStatAggregator != null) {
                smscStatAggregator.updateMaxMessagesInProcess(lstSmsSetInProcessing.size());
                smscStatAggregator.updateMinMessagesInProcess(lstSmsSetInProcessing.size());
            }
            return smsSet;
        }
    }

    public int getProcessingSmsSetSize() {
        int res = lstSmsSetInProcessing.size();
        for (FastMap.Entry<String, SmsSet> n = this.lstSmsSetWithBigMessageCount.head(), end = this.lstSmsSetWithBigMessageCount
                .tail(); (n = n.getNext()) != end && n != null;) {
            res += n.getValue().getSmsCountWithoutDelivered();
        }
        return res;
    }

    public String getLstSmsSetWithBigMessageCountState() {
        if (this.lstSmsSetWithBigMessageCount.size() == 0)
            return null;

        StringBuilder sb = new StringBuilder();
        for (FastMap.Entry<String, SmsSet> n = this.lstSmsSetWithBigMessageCount.head(), end = this.lstSmsSetWithBigMessageCount
                .tail(); (n = n.getNext()) != end && n != null;) {
            SmsSet smsSet = n.getValue();
            sb.append(smsSet.getTargetId());
            sb.append(" - ");
            sb.append(smsSet.getSmsCount());
            sb.append(" - ");
            sb.append(smsSet.getSmsCountWithoutDelivered());
            sb.append("\n");
        }
        return sb.toString();
    }

    public void garbadeCollectProcessingSmsSet() {
        synchronized (lstSmsSetInProcessing) {
            Date limit = new Date(new Date().getTime() - processingSmsSetTimeout * 1000);
            ArrayList<String> toDel = new ArrayList<String>();
            for (Map.Entry<String, SmsSet> entry : lstSmsSetInProcessing.entrySet()) {
                if (entry.getValue().getLastUpdateTime().before(limit)) {
                    toDel.add(entry.getKey());
                }
            }
            for (String key : toDel) {
                lstSmsSetInProcessing.remove(key);
            }

            toDel = new ArrayList<String>();
            for (Map.Entry<String, SmsSet> entry : lstSmsSetWithBigMessageCount.entrySet()) {
                if (entry.getValue().getLastUpdateTime().before(limit)) {
                    toDel.add(entry.getKey());
                }
            }
            for (String key : toDel) {
                lstSmsSetWithBigMessageCount.remove(key);
            }

            if (smscStatAggregator != null) {
                smscStatAggregator.updateMaxMessagesInProcess(lstSmsSetInProcessing.size());
                smscStatAggregator.updateMinMessagesInProcess(lstSmsSetInProcessing.size());
            }
        }
    }

    public void clearProcessingSmsSet() {
        synchronized (lstSmsSetInProcessing) {
            lstSmsSetInProcessing.clear();
            lstSmsSetWithBigMessageCount.clear();
            if (smscStatAggregator != null) {
                smscStatAggregator.updateMaxMessagesInProcess(lstSmsSetInProcessing.size());
                smscStatAggregator.updateMinMessagesInProcess(lstSmsSetInProcessing.size());
            }
        }
    }


    public void putCorrelationIdCacheElement(CorrelationIdValue elem, int correlationIdLiveTime) throws Exception {
        this.correlationIdLiveTime = correlationIdLiveTime;
        synchronized (this.correlationIdCacheSync) {
            this.correlationIdCache1.put(elem.getCorrelationID(), elem);
        }
    }

    public CorrelationIdValue getCorrelationIdCacheElement(String correlationID) throws Exception {
        synchronized (this.correlationIdCacheSync) {
            CorrelationIdValue res = this.correlationIdCache1.get(correlationID);
            if (res == null)
                res = this.correlationIdCache2.get(correlationID);
            return res;
        }
    }

    private class CacheManTask implements Runnable {
        public void run() {
            try {
                correlationIdCache2 = correlationIdCache1;
                correlationIdCache1 = new FastMap<String, CorrelationIdValue>();
            } finally {
                if (isStarted) {
                    CacheManTask t = new CacheManTask();
                    executor.schedule(t, correlationIdLiveTime, TimeUnit.SECONDS);
                }
            }
        }
    }

}
