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

/**
 * 
 * @author Amit Bhayani
 * @author sergey vetyutnev
 *
 */
public interface SmscPropertiesManagementMBean {

	public String getServiceCenterGt();

	public void setServiceCenterGt(String serviceCenterGt);

	public int getServiceCenterSsn();

	public void setServiceCenterSsn(int serviceCenterSsn);

	public int getHlrSsn();

	public void setHlrSsn(int hlrSsn);

	public int getMscSsn();

	public void setMscSsn(int mscSsn);

	public int getMaxMapVersion();

	public void setMaxMapVersion(int maxMapVersion);

	public int getDefaultValidityPeriodHours();

	public void setDefaultValidityPeriodHours(int defaultValidityPeriodHours);

	public int getMaxValidityPeriodHours();

	public void setMaxValidityPeriodHours(int maxValidityPeriodHours);

	public int getDefaultTon();

	public void setDefaultTon(int defaultTon);

	public int getDefaultNpi();

	public void setDefaultNpi(int defaultNpi);

	public int getSubscriberBusyDueDelay();

	public void setSubscriberBusyDueDelay(int subscriberBusyDueDelay);

	public int getFirstDueDelay();

	public void setFirstDueDelay(int firstDueDelay);

	public int getSecondDueDelay();

	public void setSecondDueDelay(int secondDueDelay);

	public int getMaxDueDelay();

	public void setMaxDueDelay(int maxDueDelay);

	public int getDueDelayMultiplicator();

	public void setDueDelayMultiplicator(int dueDelayMultiplicator);

	public int getMaxMessageLengthReducer();

	public void setMaxMessageLengthReducer(int maxMessageLengReducer);


    public String getHosts();

    public void setHosts(String hosts);

    public String getKeyspaceName();

    public void setKeyspaceName(String keyspaceName);

    public String getClusterName();

    public void setClusterName(String clusterName);

    public long getFetchPeriod();

    public void setFetchPeriod(long fetchPeriod);

    public int getFetchMaxRows();

    public void setFetchMaxRows(int fetchMaxRows);

    public int getMaxActivityCount();

    public void setMaxActivityCount(int maxActivityCount);


    public int getCdrDatabaseExportDuration();

    public void setCdrDatabaseExportDuration(int cdrDatabaseExportDuration);

}
