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

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.slee.facilities.Tracer;

import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

/**
 * 
 * @author baranowb
 * @author sergey vetyutnev
 * 
 */
public class DBOperations {
	private static final Logger logger = Logger.getLogger(DBOperations.class);

	public static final String TLV_SET = "tlvSet";

	private Cluster cluster;
	private Session session;

	private PreparedStatement smsSetExist;

	private static final DBOperations instance = new DBOperations();

	private volatile boolean started = false;

	private DBOperations() {
		super();
	}

	public static DBOperations getInstance() {
		return instance;
	}

	public void start(String ip, String keyspace) throws Exception {
		if (this.started) {
			throw new Exception("DBOperations already started");
		}

		this.cluster = Cluster.builder().addContactPoint(ip).build();
		Metadata metadata = cluster.getMetadata();

		logger.info(String.format("Connected to cluster: %s\n", metadata.getClusterName()));
		for (Host host : metadata.getAllHosts()) {
			logger.info(String.format("Datacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(), host.getAddress(),
					host.getRack()));
		}

		session = cluster.connect();

		session.execute("USE \"" + keyspace + "\"");

		// TODO : Prepare all PreparedStatements here

		smsSetExist = session.prepare("SELECT count(*) FROM \"LIVE\" WHERE \"TARGET_ID\"=?;");

		this.started = true;
	}

	public void stop() throws Exception {
		cluster.shutdown();
		Metadata metadata = cluster.getMetadata();
		logger.info(String.format("Disconnected from cluster: %s\n", metadata.getClusterName()));

		this.started = false;
	}

	public boolean checkSmsSetExists(final TargetAddress ta) throws PersistenceException {

		try {
			BoundStatement boundStatement = new BoundStatement(smsSetExist);
			boundStatement.bind(ta.getTargetId());
			ResultSet results = session.execute(boundStatement);

			Row row = results.one();

			long count = row.getLong(0);

			return (count > 0);
		} catch (Exception e) {
			String msg = "Failed to checkSmsSetExists SMS for '" + ta.getAddr() + ",Ton=" + ta.getAddrTon() + ",Npi="
					+ ta.getAddrNpi() + "'!";
			throw new PersistenceException(msg, e);
		}
	}

	public static SmsSet obtainSmsSet(final TargetAddress ta) throws PersistenceException {
		// TODO
		return null;
	}

	public void setNewMessageScheduled(final SmsSet smsSet, final Date newDueDate) throws PersistenceException {
		// TODO
	}

	public void setDeliveringProcessScheduled(final SmsSet smsSet, Date newDueDate, final int newDueDelay)
			throws PersistenceException {
		// TODO
	}

	public void setDestination(SmsSet smsSet, String destClusterName, String destSystemId, String destEsmeId,
			SmType type) {
		// TODO
	}

	public void setRoutingInfo(SmsSet smsSet, IMSI imsi, LocationInfoWithLMSI locationInfoWithLMSI) {
		// TODO
	}

	public void setDeliveryStart(final SmsSet smsSet, final Date newInSystemDate) throws PersistenceException {
		// TODO
	}

	public void setDeliveryStart(final Sms sms) throws PersistenceException {
		// TODO
	}

	public void setDeliverySuccess(final SmsSet smsSet, final Date lastDelivery) throws PersistenceException {
		// TODO
	}

	public void setDeliveryFailure(final SmsSet smsSet, final ErrorCode smStatus, final Date lastDelivery)
			throws PersistenceException {
		// TODO
	}

	public void setAlertingSupported(final String targetId, final boolean alertingSupported)
			throws PersistenceException {
		// TODO
	}

	public boolean deleteSmsSet(final SmsSet smsSet) throws PersistenceException {
		// TODO

		return false;
	}

	public void createLiveSms(final Sms sms) throws PersistenceException {
		// TODO
	}

	public Sms obtainLiveSms(final UUID dbId) throws PersistenceException {
		// TODO

		return null;
	}

	public Sms obtainLiveSms(final long messageId) throws PersistenceException {
		// TODO

		return null;
	}

	public void updateLiveSms(Sms sms) throws PersistenceException {
		// TODO
	}

	public void archiveDeliveredSms(final Sms sms, Date deliveryDate) throws PersistenceException {
		// TODO
	}

	public void archiveFailuredSms(final Sms sms) throws PersistenceException {
		// TODO
	}

	public List<SmsSet> fetchSchedulableSmsSets(final int maxRecordCount, Tracer tracer) throws PersistenceException {
		// TODO

		return null;
	}

	private void doFetchSchedulableSmsSets(final int maxRecordCount, List<SmsSet> lst, int opt)
			throws PersistenceException {
		// TODO

	}

	public void fetchSchedulableSms(final SmsSet smsSet, boolean excludeNonScheduleDeliveryTime)
			throws PersistenceException {
		// TODO
	}

	public DbSmsRoutingRule getSmsRoutingRule(final String address) throws PersistenceException {
		// TODO
		return null;
	}

	public void updateDbSmsRoutingRule(DbSmsRoutingRule dbSmsRoutingRule) throws PersistenceException {
		// TODO
	}

	public void deleteDbSmsRoutingRule(final String address) throws PersistenceException {
		// TODO
	}

	public List<DbSmsRoutingRule> getSmsRoutingRulesRange() throws PersistenceException {
		return getSmsRoutingRulesRange(null);
	}
	
	public List<DbSmsRoutingRule> getSmsRoutingRulesRange(String lastAdress) throws PersistenceException {
		//TODO
		return null;
	}

}
