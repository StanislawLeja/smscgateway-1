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

package org.mobicents.smsc.cassandra;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javolution.util.FastMap;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmsSetCashe {

    private int processingSmsSetTimeout;
    
	private FastMap<TargetAddress, TargetAddressContainer> lstSmsSetUnderAtomicOper = new FastMap<TargetAddress, TargetAddressContainer>();

	private AtomicInteger activityCount = new AtomicInteger(0);

	private FastMap<String, SmsSet> lstSmsSetInProcessing = new FastMap<String, SmsSet>();

	private static SmsSetCashe singeltone;

	static {
		singeltone = new SmsSetCashe();
	}

	private SmsSetCashe() {
	}

	public static SmsSetCashe getInstance() {
		return singeltone;
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

			if (--cont.count <= 0)
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
            return lstSmsSetInProcessing.put(targetId, smsSet);
        }
    }

    public SmsSet removeProcessingSmsSet(String targetId) {
        synchronized (lstSmsSetInProcessing) {
            SmsSet smsSet = lstSmsSetInProcessing.remove(targetId);
            return smsSet;
        }
    }

    public int getProcessingSmsSetSize() {
        return lstSmsSetInProcessing.size();
    }

    public void garbadeCollectProcessingSmsSet() {
        synchronized (lstSmsSetInProcessing) {
            Date limit = new Date(new Date().getTime() - processingSmsSetTimeout * 1000);
            ArrayList<String> toDel = new ArrayList<String>();
            for (Map.Entry<String, SmsSet> entry : lstSmsSetInProcessing.entrySet()) {
                if (entry.getValue().getCreationTime().before(limit)) {
                    toDel.add(entry.getKey());
                }
            }
            for (String key : toDel) {
                lstSmsSetInProcessing.remove(key);
            }
        }
    }

    public void clearProcessingSmsSet() {
        synchronized (lstSmsSetInProcessing) {
            lstSmsSetInProcessing.clear();
        }
    }

}
