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

package org.mobicents.smsc.slee.services.persistence.cassandra;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;

import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.DateSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.slee.resources.hector.client.HectorClientRAInterface;
import org.mobicents.smsc.slee.services.persistence.ErrorCode;
import org.mobicents.smsc.slee.services.persistence.Persistence;
import org.mobicents.smsc.slee.services.persistence.PersistenceException;
import org.mobicents.smsc.slee.services.persistence.Sms;
import org.mobicents.smsc.slee.services.persistence.SmsSet;
import org.mobicents.smsc.slee.services.persistence.SmsSetCashe;
import org.mobicents.smsc.slee.services.persistence.TargetAddress;
import org.mobicents.smsc.slee.services.persistence.TlvSet;

/**
 * @author baranowb
 * @author sergey vetyutnev
 * 
 */
public abstract class CassandraPersistenceSbb implements Sbb, Persistence {
    
    private static final ResourceAdaptorTypeID HECTOR_ID = new ResourceAdaptorTypeID("HectorClientResourceAdaptorType",
            "org.mobicents", "1.0");
    private static final String LINK = "HectorClientResourceAdaptorType";
	private static final String TLV_SET = "tlvSet";
    
    private final static CompositeSerializer SERIALIZER_COMPOSITE = CompositeSerializer.get();
    private final static BooleanSerializer SERIALIZER_BOOLEAN = BooleanSerializer.get();
    private final static DateSerializer SERIALIZER_DATE = DateSerializer.get();
    private final static StringSerializer SERIALIZER_STRING = StringSerializer.get();
    private final static IntegerSerializer SERIALIZER_INTEGER = IntegerSerializer.get();
    private final static BytesArraySerializer SERIALIZER_BYTE_ARRAY = BytesArraySerializer.get();
    
    private SbbContextExt sbbContextExt;
    private HectorClientRAInterface raSbbInterface;
    protected Keyspace keyspace;
    protected SmsSetCashe smsSetCashe = new SmsSetCashe();

    private Tracer logger;
    // ----------------------------------------
    // SBB LO
    // ----------------------------------------
 
	@Override
	public SmsSet obtainSmsSet(TargetAddress ta) throws PersistenceException {

		TargetAddress lock = this.smsSetCashe.addSmsSet(ta);
		try {
			synchronized (lock) {
				try {
					SliceQuery<String, Composite, ByteBuffer> query = HFactory.createSliceQuery(keyspace, SERIALIZER_STRING, SERIALIZER_COMPOSITE,
							ByteBufferSerializer.get());
					query.setColumnFamily(Schema.FAMILY_LIVE);
					query.setKey(ta.getTargetId());

					query.setRange(null, null, false, 100);

					QueryResult<ColumnSlice<Composite, ByteBuffer>> result = query.execute();
					ColumnSlice<Composite, ByteBuffer> cSlice = result.get();
//					List<HColumn<Composite, ByteBuffer>> lst = cSlice.getColumns();

					SmsSet smsSet = this.createSmsSet(cSlice);

					if (smsSet.getDestAddr() == null) {
						smsSet = new SmsSet();

						Mutator<String> mutator = HFactory.createMutator(keyspace, SERIALIZER_STRING);

						smsSet.setDestAddr(ta.getAddr());
						smsSet.setDestAddrTon(ta.getAddrTon());
						smsSet.setDestAddrNpi(ta.getAddrNpi());
						smsSet.setInSystem(0);

						Composite cc = createLiveColumnComposite(ta, Schema.COLUMN_ADDR_DST_DIGITS);
						mutator.addInsertion(ta.getTargetId(), Schema.FAMILY_LIVE, HFactory.createColumn(cc, ta.getAddr(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
						cc = createLiveColumnComposite(ta, Schema.COLUMN_ADDR_DST_TON);
						mutator.addInsertion(ta.getTargetId(), Schema.FAMILY_LIVE,
								HFactory.createColumn(cc, ta.getAddrTon(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
						cc = createLiveColumnComposite(ta, Schema.COLUMN_ADDR_DST_NPI);
						mutator.addInsertion(ta.getTargetId(), Schema.FAMILY_LIVE,
								HFactory.createColumn(cc, ta.getAddrNpi(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));

						cc = new Composite();
						cc = createLiveColumnComposite(ta, Schema.COLUMN_IN_SYSTEM);
						mutator.addInsertion(ta.getAddr(), Schema.FAMILY_LIVE, HFactory.createColumn(cc, 0, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));

						mutator.execute();
					}

					return smsSet;
				} catch (Exception e) {
					String msg = "Failed to obtainSmsSet SMS for '" + ta.getAddr() + ",Ton=" + ta.getAddrTon() + ",Npi=" + ta.getAddrNpi() + "'!";
					logger.severe(msg, e);
					throw new PersistenceException(msg, e);
				}
			}
		} finally {
			this.smsSetCashe.removeSmsSet(lock);
		}
	}

	@Override
	public void setScheduled(SmsSet smsSet, Date newDueDate, boolean fromMessageInsertion) throws PersistenceException {

		if (fromMessageInsertion && smsSet.getInSystem() == 2)
			// we do not update Scheduled if it is a new message insertion and target is under delivering process
			return;

		if (smsSet.getInSystem() == 1 && smsSet.getDueDate() != null && newDueDate.compareTo(smsSet.getDueDate()) >= 0)
			// we do not update Scheduled if it is already schedulered for a earlier time
			return;

		try {
			Mutator<String> mutator = HFactory.createMutator(keyspace, SERIALIZER_STRING);

			Composite cc = createLiveColumnComposite(smsSet, Schema.COLUMN_DUE_DATE);
			mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE, HFactory.createColumn(cc, newDueDate, SERIALIZER_COMPOSITE, SERIALIZER_DATE));
			cc = createLiveColumnComposite(smsSet, Schema.COLUMN_IN_SYSTEM);
			mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE, HFactory.createColumn(cc, 1, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));

			mutator.execute();
			
			smsSet.setInSystem(1);
			smsSet.setDueDate(newDueDate);
		} catch (Exception e) {
			String msg = "Failed to setScheduled for '" + smsSet.getDestAddr() + ",Ton=" + smsSet.getDestAddrTon() + ",Npi=" + smsSet.getDestAddrNpi() + "'!";
			logger.severe(msg, e);
			throw new PersistenceException(msg, e);
		}
	}

	@Override
	public void setDestination(SmsSet smsSet, String destClusterName, String destSystemId, String destEsmeId) {

		smsSet.setDestClusterName(destClusterName);
		smsSet.setDestSystemId(destSystemId);
		smsSet.setDestEsmeId(destEsmeId);
	}

	@Override
	public void setRoutingInfo(SmsSet smsSet, IMSI imsi, LocationInfoWithLMSI locationInfoWithLMSI) {

		smsSet.setImsi(imsi);
		smsSet.setLocationInfoWithLMSI(locationInfoWithLMSI);
	}

	@Override
	public void setDeliveryStart(SmsSet smsSet) throws PersistenceException {

		try {
			Mutator<String> mutator = HFactory.createMutator(keyspace, SERIALIZER_STRING);

			Composite cc = createLiveColumnComposite(smsSet, Schema.COLUMN_IN_SYSTEM);
			mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE, HFactory.createColumn(cc, 2, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
			cc = createLiveColumnComposite(smsSet, Schema.COLUMN_DELIVERY_COUNT);
			mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
					HFactory.createColumn(cc, smsSet.getDeliveryCount() + 1, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));

			mutator.execute();

			smsSet.setInSystem(2);
			smsSet.setDeliveryCount(smsSet.getDeliveryCount() + 1);
		} catch (Exception e) {
			String msg = "Failed to setDeliveryStart for '" + smsSet.getDestAddr() + ",Ton=" + smsSet.getDestAddrTon() + ",Npi=" + smsSet.getDestAddrNpi() + "'!";
			logger.severe(msg, e);
			throw new PersistenceException(msg, e);
		}
	}

	@Override
	public void setDeliverySuccess(SmsSet smsSet, Date lastDelivery) throws PersistenceException {

		try {
			Mutator<String> mutator = HFactory.createMutator(keyspace, SERIALIZER_STRING);

			Composite cc = createLiveColumnComposite(smsSet, Schema.COLUMN_IN_SYSTEM);
			mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE, HFactory.createColumn(cc, 0, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
			cc = createLiveColumnComposite(smsSet, Schema.COLUMN_SM_STATUS);
			mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE, HFactory.createColumn(cc, 0, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
			cc = createLiveColumnComposite(smsSet, Schema.COLUMN_LAST_DELIVERY);
			mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE, HFactory.createColumn(cc, lastDelivery, SERIALIZER_COMPOSITE, SERIALIZER_DATE));

			mutator.execute();

			smsSet.setInSystem(0);
			smsSet.setStatus(ErrorCode.SUCCESS);
		} catch (Exception e) {
			String msg = "Failed to setDeliverySuccess for '" + smsSet.getDestAddr() + ",Ton=" + smsSet.getDestAddrTon() + ",Npi=" + smsSet.getDestAddrNpi() + "'!";
			logger.severe(msg, e);
			throw new PersistenceException(msg, e);
		}
	}

	@Override
	public void setDeliveryFailure(SmsSet smsSet, ErrorCode smStatus, Date lastDelivery, boolean alertingSupported) throws PersistenceException {

		try {
			Mutator<String> mutator = HFactory.createMutator(keyspace, SERIALIZER_STRING);

			Composite cc = createLiveColumnComposite(smsSet, Schema.COLUMN_IN_SYSTEM);
			mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE, HFactory.createColumn(cc, 0, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
			cc = createLiveColumnComposite(smsSet, Schema.COLUMN_SM_STATUS);
			mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
					HFactory.createColumn(cc, smStatus.getCode(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
			cc = createLiveColumnComposite(smsSet, Schema.COLUMN_LAST_DELIVERY);
			mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE, HFactory.createColumn(cc, lastDelivery, SERIALIZER_COMPOSITE, SERIALIZER_DATE));
			cc = createLiveColumnComposite(smsSet, Schema.COLUMN_ALERTING_SUPPORTED);
			mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
					HFactory.createColumn(cc, alertingSupported, SERIALIZER_COMPOSITE, SERIALIZER_BOOLEAN));

			mutator.execute();

			smsSet.setInSystem(0);
			smsSet.setStatus(smStatus);
			smsSet.setLastDelivery(lastDelivery);
			smsSet.setAlertingSupported(alertingSupported);
		} catch (Exception e) {
			String msg = "Failed to setDeliverySuccess for '" + smsSet.getDestAddr() + ",Ton=" + smsSet.getDestAddrTon() + ",Npi=" + smsSet.getDestAddrNpi() + "'!";
			logger.severe(msg, e);
			throw new PersistenceException(msg, e);
		}
	}

	@Override
	public boolean deleteSmsSet(SmsSet smsSet) throws PersistenceException {

		TargetAddress lock = this.smsSetCashe.addSmsSet(new TargetAddress(smsSet));
		try {
			synchronized (lock) {

				// firstly we are looking for corresponded records in LIVE_SMS table
				this.fetchSchedulableSms(smsSet);
				if (smsSet.getFirstSms() != null) {
					// there are corresponded records in LIVE_SMS table - we will not delete LIVE record
					return false;
				}

				try {
					Mutator<String> mutator = HFactory.createMutator(keyspace, SERIALIZER_STRING);

					mutator.addDeletion(smsSet.getTargetId(), Schema.FAMILY_LIVE);
					mutator.execute();
				} catch (Exception e) {
					String msg = "Failed to deleteSmsSet for '" + smsSet.getDestAddr() + ",Ton=" + smsSet.getDestAddrTon() + ",Npi=" + smsSet.getDestAddrNpi()
							+ "'!";
					logger.severe(msg, e);
					throw new PersistenceException(msg, e);
				}

				return true;
			}
		} finally {
			this.smsSetCashe.removeSmsSet(lock);
		}
	}

	@Override
	public void createLiveSms(Sms sms) throws PersistenceException {

		try {
			Mutator<UUID> mutator = HFactory.createMutator(keyspace, UUIDSerializer.get());

			Composite cc = new Composite();
			cc.addComponent(Schema.COLUMN_TARGET_ID, SERIALIZER_STRING);
			mutator.addInsertion(sms.getDbId(), Schema.FAMILY_LIVE_SMS,
					HFactory.createColumn(cc, sms.getSmsSet().getTargetId(), SERIALIZER_COMPOSITE, StringSerializer.get()));

			this.FillUpdateFields(sms, mutator, Schema.FAMILY_LIVE_SMS);

	        mutator.execute();
		} catch (Exception e) {
			String msg = "Failed to createLiveSms SMS for '" + sms.getDbId() + "'!";
			logger.severe(msg, e);
			throw new PersistenceException(msg, e);
		}
	}

	@Override
	public Sms obtainLiveSms(UUID dbId) throws PersistenceException {
		try {
			SliceQuery<UUID, Composite, ByteBuffer> query = HFactory.createSliceQuery(keyspace, UUIDSerializer.get(), SERIALIZER_COMPOSITE,
					ByteBufferSerializer.get());
			query.setColumnFamily(Schema.FAMILY_LIVE_SMS);
			query.setRange(null, null, false, 100);
			Composite cc = new Composite();
			cc.addComponent(Schema.COLUMN_ID, SERIALIZER_STRING);
			query.setKey(dbId);

			QueryResult<ColumnSlice<Composite, ByteBuffer>> result = query.execute();
			ColumnSlice<Composite, ByteBuffer> cSlice = result.get();
			try {
				return this.createSms(cSlice, dbId);
			} catch (Exception e) {
				if (logger.isSevereEnabled()) {
					logger.severe("Failed to deserialize SMS at key '" + dbId + "'!", e);
				}
			}
		} catch (Exception e) {
			String msg = "Failed to obtainLiveSms SMS for '" + dbId + "'!";
			logger.severe(msg, e);
			throw new PersistenceException(msg, e);
		}
		
		return null;
	}

	@Override
	public Sms obtainLiveSms(long messageId) throws PersistenceException {
		try {
			IndexedSlicesQuery<UUID, Composite, ByteBuffer> query = HFactory.createIndexedSlicesQuery(keyspace, UUIDSerializer.get(),
					SERIALIZER_COMPOSITE, ByteBufferSerializer.get());
			query.setColumnFamily(Schema.FAMILY_LIVE_SMS);
			query.setRange(null, null, false, 100);
			Composite cc = new Composite();
			cc.addComponent(Schema.COLUMN_MESSAGE_ID, SERIALIZER_STRING);
			query.addEqualsExpression(cc, LongSerializer.get().toByteBuffer(messageId));

			final QueryResult<OrderedRows<UUID, Composite, ByteBuffer>> result = query.execute();
			final OrderedRows<UUID, Composite, ByteBuffer> rows = result.get();
			final List<Row<UUID, Composite, ByteBuffer>> rowsList = rows.getList();
			for (Row<UUID, Composite, ByteBuffer> row : rowsList) {
				try {
					return this.createSms(row.getColumnSlice(), row.getKey());
				} catch (Exception e) {
					if (logger.isSevereEnabled()) {
						logger.severe("Failed to deserialize SMS at key '" + row.getKey() + "'!", e);
					}
				}
			}
		} catch (Exception e) {
			String msg = "Failed to obtainLiveSms SMS for '" + messageId + "'!";
			logger.severe(msg, e);
			throw new PersistenceException(msg, e);
		}

		return null;
	}

	@Override
	public void updateLiveSms(Sms sms) throws PersistenceException {
		// TODO: implement it
		// .....................................
	}

	@Override
	public void archiveDeliveredSms(Sms sms, Date deliveryDate) throws PersistenceException {
		this.deleteLiveSms(sms);
		sms.setDeliveryDate(deliveryDate);
		this.doArchiveDeliveredSms(sms);
	}

	@Override
	public void archiveFailuredSms(Sms sms, Date deliveryDate) throws PersistenceException {
		this.deleteLiveSms(sms);
		sms.setDeliveryDate(deliveryDate);
		this.doArchiveDeliveredSms(sms);
	}

	@Override
	public List<SmsSet> fetchSchedulableSmsSets(int maxRecordCount) throws PersistenceException {
		try {
			List<SmsSet> lst = new ArrayList<SmsSet>();

			IndexedSlicesQuery<String, Composite, ByteBuffer> query = HFactory.createIndexedSlicesQuery(keyspace, StringSerializer.get(),
					SERIALIZER_COMPOSITE, ByteBufferSerializer.get());
			query.setColumnFamily(Schema.FAMILY_LIVE);
			query.setRange(null, null, false, 100);
			Composite cc = new Composite();
			cc.addComponent(Schema.COLUMN_IN_SYSTEM, SERIALIZER_STRING);
			query.addEqualsExpression(cc, IntegerSerializer.get().toByteBuffer(1));
			cc = new Composite();
			cc.addComponent(Schema.COLUMN_DUE_DATE, SERIALIZER_STRING);
			query.addLteExpression(cc, DateSerializer.get().toByteBuffer(new Date()));
			query.setRowCount(maxRecordCount);

			final QueryResult<OrderedRows<String, Composite, ByteBuffer>> result = query.execute();
			final OrderedRows<String, Composite, ByteBuffer> rows = result.get();
			final List<Row<String, Composite, ByteBuffer>> rowsList = rows.getList();
			for (Row<String, Composite, ByteBuffer> row : rowsList) {
				try {
					SmsSet smsSet = this.createSmsSet(row.getColumnSlice());
					lst.add(smsSet);
				} catch (Exception e) {
					if (logger.isSevereEnabled()) {
						logger.severe("Failed to deserialize SMS at key '" + row.getKey() + "'!", e);
					}
				}
			}

			return lst;
		} catch (Exception e) {
			String msg = "Failed to fetchSchedulableSmsSets!";
			logger.severe(msg, e);
			throw new PersistenceException(msg, e);
		}
	}

	@Override
	public void fetchSchedulableSms(SmsSet smsSet) throws PersistenceException {
		try {
			IndexedSlicesQuery<UUID, Composite, ByteBuffer> query = HFactory.createIndexedSlicesQuery(keyspace, UUIDSerializer.get(), SERIALIZER_COMPOSITE,
					ByteBufferSerializer.get());
			query.setColumnFamily(Schema.FAMILY_LIVE_SMS);
			query.setRange(null, null, false, 100);
			Composite cc = new Composite();
			cc.addComponent(Schema.COLUMN_TARGET_ID, SERIALIZER_STRING);
			query.addEqualsExpression(cc, StringSerializer.get().toByteBuffer(smsSet.getTargetId()));

			final QueryResult<OrderedRows<UUID, Composite, ByteBuffer>> result = query.execute();
			final OrderedRows<UUID, Composite, ByteBuffer> rows = result.get();
			final List<Row<UUID, Composite, ByteBuffer>> rowsList = rows.getList();
			smsSet.clearSmsList();
			for (Row<UUID, Composite, ByteBuffer> row : rowsList) {
				try {
					Sms sms = this.createSms(row.getColumnSlice(), row.getKey());
					smsSet.addSms(sms);
				} catch (Exception e) {
					if (logger.isSevereEnabled()) {
						String msg = "Failed to deserialize SMS at key '" + row.getKey() + "'!";
						logger.severe(msg, e);
						throw new PersistenceException(msg, e);
					}
				}
			}
		} catch (Exception e) {
			String msg = "Failed to fetchSchedulableSms SMS for '" + smsSet.getTargetId() + "'!";
			logger.severe(msg, e);
			throw new PersistenceException(msg, e);
		}
	}


	private Composite createLiveColumnComposite(TargetAddress ta, String colName) {
		Composite cc;
		cc = new Composite();
//		cc.addComponent(ta.getAddrTon(), SERIALIZER_INTEGER);
//		cc.addComponent(ta.getAddrNpi(), SERIALIZER_INTEGER);
		cc.addComponent(colName, SERIALIZER_STRING);
		return cc;
	}

	private Composite createLiveColumnComposite(SmsSet smsSet, String colName) {
		Composite cc;
		cc = new Composite();
//		cc.addComponent(smsSet.getDestAddrTon(), SERIALIZER_INTEGER);
//		cc.addComponent(smsSet.getDestAddrNpi(), SERIALIZER_INTEGER);
		cc.addComponent(colName, SERIALIZER_STRING);
		return cc;
	}

	private void FillUpdateFields(Sms sms, Mutator<UUID> mutator, String columnFamilyName) throws PersistenceException {
		Composite cc = new Composite();
		cc.addComponent(Schema.COLUMN_ID, SERIALIZER_STRING);
		mutator.addInsertion(sms.getDbId(), columnFamilyName, HFactory.createColumn(cc, sms.getDbId(), SERIALIZER_COMPOSITE, UUIDSerializer.get()));
		cc = new Composite();
		cc.addComponent(Schema.COLUMN_ADDR_DST_DIGITS, SERIALIZER_STRING);
		mutator.addInsertion(sms.getDbId(), columnFamilyName,
				HFactory.createColumn(cc, sms.getSmsSet().getDestAddr(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
		cc = new Composite();
		cc.addComponent(Schema.COLUMN_ADDR_DST_TON, SERIALIZER_STRING);
		mutator.addInsertion(sms.getDbId(), columnFamilyName,
				HFactory.createColumn(cc, sms.getSmsSet().getDestAddrTon(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
		cc = new Composite();
		cc.addComponent(Schema.COLUMN_ADDR_DST_NPI, SERIALIZER_STRING);
		mutator.addInsertion(sms.getDbId(), columnFamilyName,
				HFactory.createColumn(cc, sms.getSmsSet().getDestAddrNpi(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));

		if (sms.getSourceAddr() != null) {
			cc = new Composite();
			cc.addComponent(Schema.COLUMN_ADDR_SRC_DIGITS, SERIALIZER_STRING);
			mutator.addInsertion(sms.getDbId(), columnFamilyName,
					HFactory.createColumn(cc, sms.getSourceAddr(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
		}
		cc = new Composite();
		cc.addComponent(Schema.COLUMN_ADDR_SRC_TON, SERIALIZER_STRING);
		mutator.addInsertion(sms.getDbId(), columnFamilyName,
				HFactory.createColumn(cc, sms.getSourceAddrTon(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
		cc = new Composite();
		cc.addComponent(Schema.COLUMN_ADDR_SRC_NPI, SERIALIZER_STRING);
		mutator.addInsertion(sms.getDbId(), columnFamilyName,
				HFactory.createColumn(cc, sms.getSourceAddrNpi(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));

		cc = new Composite();
		cc.addComponent(Schema.COLUMN_MESSAGE_ID, SERIALIZER_STRING);
		mutator.addInsertion(sms.getDbId(), columnFamilyName,
				HFactory.createColumn(cc, sms.getMessageId(), SERIALIZER_COMPOSITE, LongSerializer.get()));
		if (sms.getOrigEsmeId() != null) {
			cc = new Composite();
			cc.addComponent(Schema.COLUMN_ORIG_ESME_ID, SERIALIZER_STRING);
			mutator.addInsertion(sms.getDbId(), columnFamilyName,
					HFactory.createColumn(cc, sms.getOrigEsmeId(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
		}
		if (sms.getOrigSystemId() != null) {
			cc = new Composite();
			cc.addComponent(Schema.COLUMN_ORIG_SYSTEM_ID, SERIALIZER_STRING);
			mutator.addInsertion(sms.getDbId(), columnFamilyName,
					HFactory.createColumn(cc, sms.getOrigSystemId(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
		}
		if (sms.getSubmitDate() != null) {
			cc = new Composite();
			cc.addComponent(Schema.COLUMN_SUBMIT_DATE, SERIALIZER_STRING);
			mutator.addInsertion(sms.getDbId(), columnFamilyName,
					HFactory.createColumn(cc, sms.getSubmitDate(), SERIALIZER_COMPOSITE, SERIALIZER_DATE));
		}

		if (sms.getServiceType() != null) {
			cc = new Composite();
			cc.addComponent(Schema.COLUMN_SERVICE_TYPE, SERIALIZER_STRING);
			mutator.addInsertion(sms.getDbId(), columnFamilyName,
					HFactory.createColumn(cc, sms.getServiceType(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
		}
		cc = new Composite();
		cc.addComponent(Schema.COLUMN_ESM_CLASS, SERIALIZER_STRING);
		mutator.addInsertion(sms.getDbId(), columnFamilyName,
				HFactory.createColumn(cc, sms.getEsmClass(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
		cc = new Composite();
		cc.addComponent(Schema.COLUMN_PROTOCOL_ID, SERIALIZER_STRING);
		mutator.addInsertion(sms.getDbId(), columnFamilyName,
				HFactory.createColumn(cc, sms.getProtocolId(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
		cc = new Composite();
		cc.addComponent(Schema.COLUMN_PRIORITY, SERIALIZER_STRING);
		mutator.addInsertion(sms.getDbId(), columnFamilyName,
				HFactory.createColumn(cc, sms.getPriority(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
		cc = new Composite();
		cc.addComponent(Schema.COLUMN_REGISTERED_DELIVERY, SERIALIZER_STRING);
		mutator.addInsertion(sms.getDbId(), columnFamilyName,
				HFactory.createColumn(cc, sms.getRegisteredDelivery(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
		cc = new Composite();
		cc.addComponent(Schema.COLUMN_REPLACE, SERIALIZER_STRING);
		mutator.addInsertion(sms.getDbId(), columnFamilyName,
				HFactory.createColumn(cc, sms.getReplaceIfPresent(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
		cc = new Composite();
		cc.addComponent(Schema.COLUMN_DATA_CODING, SERIALIZER_STRING);
		mutator.addInsertion(sms.getDbId(), columnFamilyName,
				HFactory.createColumn(cc, sms.getDataCoding(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
		cc = new Composite();
		cc.addComponent(Schema.COLUMN_DEFAULT_MSG_ID, SERIALIZER_STRING);
		mutator.addInsertion(sms.getDbId(), columnFamilyName,
				HFactory.createColumn(cc, sms.getDefaultMsgId(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));

		if (sms.getShortMessage() != null) {
			cc = new Composite();
			cc.addComponent(Schema.COLUMN_MESSAGE, SERIALIZER_STRING);
			mutator.addInsertion(sms.getDbId(), columnFamilyName,
					HFactory.createColumn(cc, sms.getShortMessage(), SERIALIZER_COMPOSITE, SERIALIZER_BYTE_ARRAY));
		}
		if (sms.getScheduleDeliveryTime() != null) {
			cc = new Composite();
			cc.addComponent(Schema.COLUMN_SCHEDULE_DELIVERY, SERIALIZER_STRING);
			mutator.addInsertion(sms.getDbId(), columnFamilyName,
					HFactory.createColumn(cc, sms.getScheduleDeliveryTime(), SERIALIZER_COMPOSITE, SERIALIZER_DATE));
		}
		if (sms.getValidityPeriod() != null) {
			cc = new Composite();
			cc.addComponent(Schema.COLUMN_VALIDITY_PERIOD, SERIALIZER_STRING);
			mutator.addInsertion(sms.getDbId(), columnFamilyName,
					HFactory.createColumn(cc, sms.getValidityPeriod(), SERIALIZER_COMPOSITE, SERIALIZER_DATE));
		}

		if (sms.getTlvSet().getOptionalParameterCount() > 0) {
			cc = new Composite();
			cc.addComponent(Schema.COLUMN_OPTIONAL_PARAMETERS, SERIALIZER_STRING);

			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				XMLObjectWriter writer = XMLObjectWriter.newInstance(baos);
				writer.setIndentation("\t");
				writer.write(sms.getTlvSet(), TLV_SET, TlvSet.class);
				writer.close();
				byte[] rawData = baos.toByteArray();
				String serializedEvent = new String(rawData);

				mutator.addInsertion(sms.getDbId(), columnFamilyName, HFactory.createColumn(cc, serializedEvent, SERIALIZER_COMPOSITE, SERIALIZER_STRING));
			} catch (XMLStreamException e) {
				String msg = "XMLStreamException when serializing optional parameters for '" + sms.getDbId() + "'!";
				logger.severe(msg, e);
				throw new PersistenceException(msg, e);
			}
		}
	
	}

	private Sms createSms(ColumnSlice<Composite, ByteBuffer> cSlice, UUID dbId) throws IOException, PersistenceException {

		Sms sms = new Sms();
		sms.setDbId(dbId);
		String destAddr = null;
		int destAddrTon = -1;
		int destAddrNpi = -1;

		for (HColumn<Composite, ByteBuffer> col : cSlice.getColumns()) {
			Composite nm = col.getName();
			String name = nm.get(0, SERIALIZER_STRING);

			if (name.equals(Schema.COLUMN_ADDR_DST_DIGITS)) {
				destAddr = SERIALIZER_STRING.fromByteBuffer(col.getValue());
			} else if (name.equals(Schema.COLUMN_ADDR_DST_TON)) {
				destAddrTon = SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
			} else if (name.equals(Schema.COLUMN_ADDR_DST_NPI)) {
				destAddrNpi = SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
			} else if (name.equals(Schema.COLUMN_MESSAGE_ID)) {
				long val = LongSerializer.get().fromByteBuffer(col.getValue());
				sms.setMessageId(val);
			} else if (name.equals(Schema.COLUMN_ORIG_ESME_ID)) {
				String val = SERIALIZER_STRING.fromByteBuffer(col.getValue());
				sms.setOrigEsmeId(val);
			} else if (name.equals(Schema.COLUMN_ORIG_SYSTEM_ID)) {
				String val = SERIALIZER_STRING.fromByteBuffer(col.getValue());
				sms.setOrigSystemId(val);
			} else if (name.equals(Schema.COLUMN_SUBMIT_DATE)) {
				Date val = SERIALIZER_DATE.fromByteBuffer(col.getValue());
				sms.setSubmitDate(val);

			} else if (name.equals(Schema.COLUMN_ADDR_SRC_DIGITS)) {
				sms.setSourceAddr(SERIALIZER_STRING.fromByteBuffer(col.getValue()));
			} else if (name.equals(Schema.COLUMN_ADDR_SRC_TON)) {
				sms.setSourceAddrTon(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
			} else if (name.equals(Schema.COLUMN_ADDR_SRC_NPI)) {
				sms.setSourceAddrNpi(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));

			} else if (name.equals(Schema.COLUMN_SERVICE_TYPE)) {
				String val = SERIALIZER_STRING.fromByteBuffer(col.getValue());
				sms.setServiceType(val);
			} else if (name.equals(Schema.COLUMN_ESM_CLASS)) {
				sms.setEsmClass(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
			} else if (name.equals(Schema.COLUMN_PROTOCOL_ID)) {
				sms.setProtocolId(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
			} else if (name.equals(Schema.COLUMN_PRIORITY)) {
				sms.setPriority(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
			} else if (name.equals(Schema.COLUMN_REGISTERED_DELIVERY)) {
				sms.setRegisteredDelivery(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
			} else if (name.equals(Schema.COLUMN_REPLACE)) {
				sms.setReplaceIfPresent(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
			} else if (name.equals(Schema.COLUMN_DATA_CODING)) {
				sms.setDataCoding(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
			} else if (name.equals(Schema.COLUMN_DEFAULT_MSG_ID)) {
				sms.setDefaultMsgId(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));

			} else if (name.equals(Schema.COLUMN_MESSAGE)) {
				sms.setShortMessage(SERIALIZER_BYTE_ARRAY.fromByteBuffer(col.getValue()));
			} else if (name.equals(Schema.COLUMN_SCHEDULE_DELIVERY)) {
				Date val = SERIALIZER_DATE.fromByteBuffer(col.getValue());
				sms.setScheduleDeliveryTime(val);
			} else if (name.equals(Schema.COLUMN_VALIDITY_PERIOD)) {
				Date val = SERIALIZER_DATE.fromByteBuffer(col.getValue());
				sms.setValidityPeriod(val);

			} else if (name.equals(Schema.COLUMN_OPTIONAL_PARAMETERS)) {
				String s = SERIALIZER_STRING.fromByteBuffer(col.getValue());
				try {
					ByteArrayInputStream bais = new ByteArrayInputStream(s.getBytes());
					XMLObjectReader reader = XMLObjectReader.newInstance(bais);
					TlvSet copy = reader.read(TLV_SET, TlvSet.class);
					sms.getTlvSet().clearAllOptionalParameter();
					sms.getTlvSet().addAllOptionalParameter(copy.getOptionalParameters());
				} catch (XMLStreamException e) {
					String msg = "XMLStreamException when deserializing optional parameters for '" + sms.getDbId() + "'!";
					logger.severe(msg, e);
					throw new PersistenceException(msg, e);
				}
			}
		}

		if (destAddr == null || destAddrTon == -1 || destAddrNpi == -1) {
			throw new PersistenceException("destAddr or destAddrTon or destAddrNpi is absent in LIVE_SMS for ID='" + dbId + "'");
		}
		TargetAddress ta = new TargetAddress(destAddrTon, destAddrNpi, destAddr);
		SmsSet smsSet = this.obtainSmsSet(ta);
		sms.setSmsSet(smsSet);

		return sms;
	}

	private SmsSet createSmsSet(ColumnSlice<Composite, ByteBuffer> cSlice) {
		SmsSet smsSet = new SmsSet();
		for (HColumn<Composite, ByteBuffer> col : cSlice.getColumns()) {
			Composite nm = col.getName();
			String name = nm.get(0, SERIALIZER_STRING);

			if (name.equals(Schema.COLUMN_ADDR_DST_DIGITS)) {
				smsSet.setDestAddr(SERIALIZER_STRING.fromByteBuffer(col.getValue()));
			} else if (name.equals(Schema.COLUMN_ADDR_DST_TON)) {
				smsSet.setDestAddrTon(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
			} else if (name.equals(Schema.COLUMN_ADDR_DST_NPI)) {
				smsSet.setDestAddrNpi(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
			} else if (name.equals(Schema.COLUMN_IN_SYSTEM)) {
				smsSet.setInSystem(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
			} else if (name.equals(Schema.COLUMN_DUE_DATE)) {
				smsSet.setDueDate(SERIALIZER_DATE.fromByteBuffer(col.getValue()));
			} else if (name.equals(Schema.COLUMN_SM_STATUS)) {
				smsSet.setStatus(ErrorCode.fromInt((SERIALIZER_INTEGER.fromByteBuffer(col.getValue()))));
			} else if (name.equals(Schema.COLUMN_DELIVERY_COUNT)) {
				smsSet.setDeliveryCount(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
			} else if (name.equals(Schema.COLUMN_LAST_DELIVERY)) {
				smsSet.setLastDelivery(SERIALIZER_DATE.fromByteBuffer(col.getValue()));
			} else if (name.equals(Schema.COLUMN_ALERTING_SUPPORTED)) {
				smsSet.setAlertingSupported(SERIALIZER_BOOLEAN.fromByteBuffer(col.getValue()));
			}
		}
		return smsSet;
	}

	private void deleteLiveSms(Sms sms) throws PersistenceException {

		try {
			Mutator<UUID> mutator = HFactory.createMutator(keyspace, UUIDSerializer.get());

			mutator.addDeletion(sms.getDbId(), Schema.FAMILY_LIVE_SMS);
			mutator.execute();
		} catch (Exception e) {
			String msg = "Failed to deleteLiveSms for '" + sms.getDbId() + "'!";
			logger.severe(msg, e);
			throw new PersistenceException(msg, e);
		}
	}

	private void doArchiveDeliveredSms(Sms sms) throws PersistenceException {
		try {
			Mutator<UUID> mutator = HFactory.createMutator(keyspace, UUIDSerializer.get());

			this.FillUpdateFields(sms, mutator, Schema.FAMILY_ARCHIVE);

			Composite cc;
			if (sms.getSmsSet().getDestClusterName() != null) {
				cc = new Composite();
				cc.addComponent(Schema.COLUMN_DEST_CLUSTER_NAME, SERIALIZER_STRING);
				mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE,
						HFactory.createColumn(cc, sms.getSmsSet().getDestClusterName(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
			}
			if (sms.getSmsSet().getDestEsmeId() != null) {
				cc = new Composite();
				cc.addComponent(Schema.COLUMN_DEST_ESME_ID, SERIALIZER_STRING);
				mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE,
						HFactory.createColumn(cc, sms.getSmsSet().getDestEsmeId(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
			}
			if (sms.getSmsSet().getDestSystemId() != null) {
				cc = new Composite();
				cc.addComponent(Schema.COLUMN_DEST_SYSTEM_ID, SERIALIZER_STRING);
				mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE,
						HFactory.createColumn(cc, sms.getSmsSet().getDestSystemId(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
			}
			if (sms.getDeliverDate() != null) {
				cc = new Composite();
				cc.addComponent(Schema.COLUMN_DELIVERY_DATE, SERIALIZER_STRING);
				mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE,
						HFactory.createColumn(cc, sms.getDeliverDate(), SERIALIZER_COMPOSITE, SERIALIZER_DATE));
			}

			if (sms.getSmsSet().getImsi() != null) {
				cc = new Composite();
				cc.addComponent(Schema.COLUMN_IMSI, SERIALIZER_STRING);
				mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE,
						HFactory.createColumn(cc, sms.getSmsSet().getImsi().getData(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
			}
			if (sms.getSmsSet().getLocationInfoWithLMSI() != null) {
				cc = new Composite();
				cc.addComponent(Schema.COLUMN_NNN_DIGITS, SERIALIZER_STRING);
				mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE, HFactory.createColumn(cc, sms.getSmsSet().getLocationInfoWithLMSI()
						.getNetworkNodeNumber().getAddress(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
				cc = new Composite();
				cc.addComponent(Schema.COLUMN_NNN_AN, SERIALIZER_STRING);
				mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE, HFactory.createColumn(cc, sms.getSmsSet().getLocationInfoWithLMSI()
						.getNetworkNodeNumber().getAddressNature().getIndicator(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
				cc = new Composite();
				cc.addComponent(Schema.COLUMN_NNN_NP, SERIALIZER_STRING);
				mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE, HFactory.createColumn(cc, sms.getSmsSet().getLocationInfoWithLMSI()
						.getNetworkNodeNumber().getNumberingPlan().getIndicator(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
			}
			if (sms.getSmsSet().getStatus() != null) {
				cc = new Composite();
				cc.addComponent(Schema.COLUMN_SM_STATUS, SERIALIZER_STRING);
				mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE,
						HFactory.createColumn(cc, sms.getSmsSet().getStatus().getCode(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
			}
			if (sms.getSmsSet().getType() != null) {
				cc = new Composite();
				cc.addComponent(Schema.COLUMN_SM_TYPE, SERIALIZER_STRING);
				mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE,
						HFactory.createColumn(cc, sms.getSmsSet().getType().getCode(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
			}
			cc = new Composite();
			cc.addComponent(Schema.COLUMN_DELIVERY_COUNT, SERIALIZER_STRING);
			mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE,
					HFactory.createColumn(cc, sms.getSmsSet().getDeliveryCount(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));

			mutator.execute();
		} catch (Exception e) {
			String msg = "Failed to archiveDeliveredSms SMS for '" + sms.getDbId() + "'!";
			logger.severe(msg, e);
			throw new PersistenceException(msg, e);
		}
	}


	@Override
	public TargetAddress obtainSynchroObject(TargetAddress ta) {
		return this.smsSetCashe.addSmsSet(ta);
	}

	@Override
	public void releaseSynchroObject(TargetAddress ta) {
		this.smsSetCashe.removeSmsSet(ta);
	}


    // ----------------------------------------
    // SLEE Stuff
    // ----------------------------------------
    /*
     * (non-Javadoc)
     * 
     * @see javax.slee.Sbb#sbbActivate()
     */
    @Override
    public void sbbActivate() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.slee.Sbb#sbbCreate()
     */
    @Override
    public void sbbCreate() throws CreateException {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.slee.Sbb#sbbExceptionThrown(java.lang.Exception, java.lang.Object, javax.slee.ActivityContextInterface)
     */
    @Override
    public void sbbExceptionThrown(Exception arg0, Object arg1, ActivityContextInterface arg2) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.slee.Sbb#sbbLoad()
     */
    @Override
    public void sbbLoad() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.slee.Sbb#sbbPassivate()
     */
    @Override
    public void sbbPassivate() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.slee.Sbb#sbbPostCreate()
     */
    @Override
    public void sbbPostCreate() throws CreateException {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.slee.Sbb#sbbRemove()
     */
    @Override
    public void sbbRemove() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.slee.Sbb#sbbRolledBack(javax.slee.RolledBackContext)
     */
    @Override
    public void sbbRolledBack(RolledBackContext arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.slee.Sbb#sbbStore()
     */
    @Override
    public void sbbStore() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.slee.Sbb#setSbbContext(javax.slee.SbbContext)
     */
    @Override
    public void setSbbContext(SbbContext arg0) {
        this.sbbContextExt = (SbbContextExt) arg0;
        this.logger = this.sbbContextExt.getTracer(getClass().getSimpleName());
        this.raSbbInterface = (HectorClientRAInterface) this.sbbContextExt.getResourceAdaptorInterface(HECTOR_ID, LINK);
        this.keyspace = this.raSbbInterface.getKeyspace();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.slee.Sbb#unsetSbbContext()
     */
    @Override
    public void unsetSbbContext() {
        this.sbbContextExt = null;
        this.raSbbInterface = null;
        this.keyspace = null;
        this.logger = null;
    }

}

