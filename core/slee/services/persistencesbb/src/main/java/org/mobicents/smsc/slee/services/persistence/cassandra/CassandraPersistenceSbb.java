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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;

import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.DateSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.TimeUUIDSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

import org.mobicents.protocols.ss7.indicator.GlobalTitleIndicator;
import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.sccp.parameter.GT0001;
import org.mobicents.protocols.ss7.sccp.parameter.GT0010;
import org.mobicents.protocols.ss7.sccp.parameter.GT0011;
import org.mobicents.protocols.ss7.sccp.parameter.GT0100;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.slee.resources.hector.client.HectorClientRAInterface;
import org.mobicents.smsc.slee.services.persistence.Persistence;
import org.mobicents.smsc.slee.services.persistence.PersistenceException;
import org.mobicents.smsc.slee.services.persistence.SmType;
import org.mobicents.smsc.slee.services.persistence.Sms;
import org.mobicents.smsc.slee.services.persistence.SmsSet;
import org.mobicents.smsc.slee.services.persistence.TargetAddress;

/**
 * @author baranowb
 * @author sergey vetyutnev
 * 
 */
public abstract class CassandraPersistenceSbb implements Sbb, Persistence {
    
    private static final ResourceAdaptorTypeID HECTOR_ID = new ResourceAdaptorTypeID("HectorClientResourceAdaptorType",
            "org.mobicents", "1.0");
    private static final String LINK = "HectorClientResourceAdaptorType";
    
    private final static BooleanSerializer SERIALIZER_BOOLEAN = BooleanSerializer.get();
    private final static DateSerializer SERIALIZER_DATE = DateSerializer.get();
    private final static StringSerializer SERIALIZER_STRING = StringSerializer.get();
    private final static IntegerSerializer SERIALIZER_INTEGER = IntegerSerializer.get();
    private final static BytesArraySerializer SERIALIZER_BYTE_ARRAY = BytesArraySerializer.get();
    
    private SbbContextExt sbbContextExt;
    private HectorClientRAInterface raSbbInterface;
    protected Keyspace keyspace;

    private Tracer logger;
    // ----------------------------------------
    // SBB LO
    // ----------------------------------------
 
	@Override
	public SmsSet obtainSmsSet(TargetAddress ta) throws PersistenceException {

		SmsSet smsSet = null;
		try {
//			ColumnQuery<String, Composite, ByteBuffer> query = HFactory.createColumnQuery(keyspace, StringSerializer.get(), CompositeSerializer.get(), ByteBufferSerializer.get());
			SliceQuery<String, Composite, ByteBuffer> query = HFactory.createSliceQuery(keyspace, StringSerializer.get(), CompositeSerializer.get(), ByteBufferSerializer.get());
			query.setColumnFamily(Schema.FAMILY_LIVE);
			Composite coKey3 = new Composite();
			coKey3.addComponent(ta.getAddrTon(), IntegerSerializer.get());
			coKey3.addComponent(ta.getAddrNpi(), IntegerSerializer.get());
			coKey3.addComponent(Schema.COLUMN_ADDR_DST_TON, StringSerializer.get());
//			query.setNames(coKey3);
//			query.setColumnNames(arg0);
//			query.setKey(ta.getAddr());

//			QueryResult<HColumn<Composite,ByteBuffer>> result = query.execute();
			QueryResult<ColumnSlice<Composite, ByteBuffer>> result = query.execute();
//			HColumn<Composite, ByteBuffer> rows = result.get();


			int i1 = 0;
			i1++;

//			final IndexedSlicesQuery<String, String, ByteBuffer> query = HFactory.createIndexedSlicesQuery(keyspace, StringSerializer.get(), SERIALIZER_STRING,
//					ByteBufferSerializer.get());
//			query.setColumnFamily(Schema.FAMILY_LIVE);
//			query.setColumnNames(Schema.COLUMNS_LIVE);
//			query.addEqualsExpression(Schema.COLUMN_ADDR_DST_DIGITS, StringSerializer.get().toByteBuffer(ta.getAddr()));
//			query.addEqualsExpression(Schema.COLUMN_ADDR_DST_TON, IntegerSerializer.get().toByteBuffer(ta.getAddrTon()));
//			query.addEqualsExpression(Schema.COLUMN_ADDR_DST_NPI, IntegerSerializer.get().toByteBuffer(ta.getAddrNpi()));
//
//			final QueryResult<OrderedRows<String, String, ByteBuffer>> result = query.execute();
//			final OrderedRows<String, String, ByteBuffer> rows = result.get();
//			final List<Row<String, String, ByteBuffer>> rowsList = rows.getList();

			// for (Row<UUID, String, ByteBuffer> row : rowsList) {
			// final ColumnSlice<String, ByteBuffer> cSlice =
			// row.getColumnSlice();
			// final UUID key = row.getKey();
			// try {
			// PersistableSms psms = createSms(key, true, cSlice);
			// lst.add(psms);
			// } catch (IOException e) {
			// if (logger.isSevereEnabled()) {
			// logger.severe("Failed to deserialize SMS at key '" + key + "'!",
			// e);
			// }
			// }
			// }
		} catch (Exception e) {
			int i1 = 0;
		}

		return smsSet;
	}

	public void createLiveSms(Sms sms) throws PersistenceException {

		try {
			Mutator<UUID> mutator = HFactory.createMutator(keyspace, UUIDSerializer.get());

			this.FillUpdateFields(sms, mutator, Schema.FAMILY_LIVE_SMS);

	        mutator.execute();
		} catch (Exception e) {
			int i1 = 0;
			i1++;
			return;
		}
	}

	private void FillUpdateFields(Sms sms, Mutator<UUID> mutator, String columnFamilyName) {
		Composite cc = new Composite();
		cc.addComponent(Schema.COLUMN_ID, StringSerializer.get());
		mutator.addInsertion(sms.getDbId(), columnFamilyName, HFactory.createColumn(cc, sms.getDbId(), CompositeSerializer.get(), UUIDSerializer.get()));
		cc = new Composite();
		cc.addComponent(Schema.COLUMN_ADDR_DST_DIGITS, StringSerializer.get());
		mutator.addInsertion(sms.getDbId(), columnFamilyName,
				HFactory.createColumn(cc, sms.getSmsSet().getDestAddr(), CompositeSerializer.get(), StringSerializer.get()));
		cc = new Composite();
		cc.addComponent(Schema.COLUMN_ADDR_DST_TON, StringSerializer.get());
		mutator.addInsertion(sms.getDbId(), columnFamilyName,
				HFactory.createColumn(cc, sms.getSmsSet().getDestAddrTon(), CompositeSerializer.get(), IntegerSerializer.get()));
		cc = new Composite();
		cc.addComponent(Schema.COLUMN_ADDR_DST_NPI, StringSerializer.get());
		mutator.addInsertion(sms.getDbId(), columnFamilyName,
				HFactory.createColumn(cc, sms.getSmsSet().getDestAddrNpi(), CompositeSerializer.get(), IntegerSerializer.get()));

		// ............................
	}

	public void archiveDeliveredSms(Sms sms) throws PersistenceException {
		// ............................
		this.doArchiveDeliveredSms(sms);
	}

	public void archiveFailuredSms(Sms sms) throws PersistenceException {
		// ............................
		this.doArchiveDeliveredSms(sms);
	}

	private void doArchiveDeliveredSms(Sms sms) {
		try {
			Mutator<UUID> mutator = HFactory.createMutator(keyspace, UUIDSerializer.get());

			this.FillUpdateFields(sms, mutator, Schema.FAMILY_ARCHIVE);

			Composite cc = new Composite();
			cc.addComponent(Schema.COLUMN_ADDR_SRC_DIGITS, StringSerializer.get());
			mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE,
					HFactory.createColumn(cc, sms.getSourceAddr(), CompositeSerializer.get(), StringSerializer.get()));
			cc = new Composite();
			cc.addComponent(Schema.COLUMN_ADDR_SRC_TON, StringSerializer.get());
			mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE,
					HFactory.createColumn(cc, sms.getSourceAddrTon(), CompositeSerializer.get(), IntegerSerializer.get()));
			cc = new Composite();
			cc.addComponent(Schema.COLUMN_ADDR_SRC_NPI, StringSerializer.get());
			mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE,
					HFactory.createColumn(cc, sms.getSourceAddrNpi(), CompositeSerializer.get(), IntegerSerializer.get()));

			// ............................

			mutator.execute();
		} catch (Exception e) {
			int i1 = 0;
			i1++;
			return;
		}
	}

	public Sms obtainLiveSms(UUID dbId) throws PersistenceException {
		return null;
	}

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.mobicents.smsc.slee.services.persistence.Persistence#createInstance(org.mobicents.smsc.slee.services.smpp.server.
     * events.SmsEvent, org.mobicents.smsc.slee.services.persistence.SmType,
     * org.mobicents.protocols.ss7.sccp.parameter.SccpAddress, org.mobicents.protocols.ss7.sccp.parameter.SccpAddress)
     */
//    @Override
//    public PersistableSms createInstance(Sms event, SmType type, SccpAddress scAddress, SccpAddress mxAddress) {
//        PersistableSmsImpl persistableSms = new PersistableSmsImpl();
//        persistableSms.setEvent(event);
//        persistableSms.setType(type);
//        persistableSms.setScAddress(scAddress);
//        persistableSms.setMxAddress(mxAddress);
//        return persistableSms;
//    }

//    /*
//     * (non-Javadoc)
//     * 
//     * @see
//     * org.mobicents.smsc.slee.services.persistence.Persistence#create(org.mobicents.smsc.slee.services.persistence.PersistableSms
//     * )
//     */
//    @Override
//    public void create(PersistableSms psms) throws PersistenceException {
//        if (!(psms instanceof PersistableSmsImpl)) {
//            throw new PersistenceException("Wrong type of PersistableSms!");
//        }
//        PersistableSmsImpl persistableSms = (PersistableSmsImpl) psms;
//        if (persistableSms.getDbId() != null) {
//            throw new PersistenceException("Can not create the same record twice!");
//        }
//        Mutator<UUID> mutator = HFactory.createMutator(keyspace, new TimeUUIDSerializer());
//        final UUID liveKey = new UUID();
//        createSms(mutator, liveKey, Schema.FAMILY_LIVE, persistableSms,true);
//        mutator.execute();
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see
//     * org.mobicents.smsc.slee.services.persistence.Persistence#updateTargetAddress(org.mobicents.smsc.slee.services.persistence
//     * .PersistableSms)
//     */
//    @Override
//    public void updateTargetAddress(PersistableSms psms) throws PersistenceException {
//        if (!(psms instanceof PersistableSmsImpl)) {
//            throw new PersistenceException("Wrong type of PersistableSms!");
//        }
//        PersistableSmsImpl persistableSms = (PersistableSmsImpl) psms;
//        if (persistableSms.getDbId() == null) {
//            throw new PersistenceException("Can not update record since its not in LIVE!");
//        }
//        Mutator<UUID> mutator = HFactory.createMutator(keyspace, new TimeUUIDSerializer());
//        createSccpAddress(mutator, Schema.FAMILY_LIVE,(UUID) persistableSms.getDbId(),Schema.SCCP_PREFIX_MX, persistableSms.getMxAddress());
//        mutator.execute();
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see
//     * org.mobicents.smsc.slee.services.persistence.Persistence#updateDeliveryCount(org.mobicents.smsc.slee.services.persistence
//     * .PersistableSms)
//     */
//    @Override
//    public void updateDeliveryCount(PersistableSms psms) throws PersistenceException {
//        if (!(psms instanceof PersistableSmsImpl)) {
//            throw new PersistenceException("Wrong type of PersistableSms!");
//        }
//        PersistableSmsImpl persistableSms = (PersistableSmsImpl) psms;
//        if (persistableSms.getDbId() == null) {
//            throw new PersistenceException("Can not update record since its not in LIVE!");
//        }
//        
//        Mutator<UUID> mutator = HFactory.createMutator(keyspace, new TimeUUIDSerializer());
//        mutator.addInsertion((UUID) persistableSms.getDbId(), Schema.FAMILY_LIVE, HFactory.createColumn(
//                Schema.COLUMN_DELIVERY_COUNT, persistableSms.getDeliveryCount(), SERIALIZER_STRING,
//                SERIALIZER_INTEGER));
//        // TODO: address update required?
//        mutator.execute();
//
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see
//     * org.mobicents.smsc.slee.services.persistence.Persistence#archive(org.mobicents.smsc.slee.services.persistence.PersistableSms
//     * )
//     */
//    @Override
//    public void archive(PersistableSms psms) throws PersistenceException {
//        if (!(psms instanceof PersistableSmsImpl)) {
//            throw new PersistenceException("Wrong type of PersistableSms!");
//        }
//        final PersistableSmsImpl persistableSms = (PersistableSmsImpl) psms;
//
//        // ARCHIVE: create
//        Mutator<UUID> mutator = HFactory.createMutator(keyspace, new TimeUUIDSerializer());
//        // XX
//        // TODO: XXX this can be null, since psms may not exist in live
//        UUID liveKey = (UUID) persistableSms.getDbId();
//        UUID archiveKey = null;
//        
//        if(liveKey!=null){
//            archiveKey = liveKey;
//        } else {
//            liveKey = new UUID();
//        }
//        
//        createSms(mutator, liveKey, Schema.FAMILY_ARCHIVE, persistableSms,false);
//
//        if (archiveKey != null) {
//            // REMOVE from LIVE;
//            mutator.addDeletion(archiveKey, Schema.FAMILY_LIVE);
//        }
//        mutator.execute();
//
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.mobicents.smsc.slee.services.persistence.Persistence#markAlertingNotSupported(org.mobicents.smsc.slee.services.
//     * persistence.PersistableSms)
//     */
//    @Override
//    public void markAlertingNotSupported(PersistableSms psms) throws PersistenceException {
//     // this op is done for live
//        if (!(psms instanceof PersistableSmsImpl)) {
//            throw new PersistenceException("Wrong type of PersistableSms!");
//        }
//        PersistableSmsImpl persistableSms = (PersistableSmsImpl) psms;
//        if (persistableSms.getDbId() == null) {
//            throw new PersistenceException("Can not update record since its not in LIVE!");
//        }
//        final UUID liveKey = (UUID) persistableSms.getDbId();
//        Mutator<UUID> mutator = HFactory.createMutator(keyspace, new TimeUUIDSerializer());
//        mutator.addInsertion(liveKey, Schema.FAMILY_LIVE, HFactory.createColumn(Schema.COLUMN_ALERTING_SUPPORTED,
//                persistableSms.isAlertingSupported(), SERIALIZER_STRING, SERIALIZER_BOOLEAN));
//        mutator.execute();
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.mobicents.smsc.slee.services.persistence.Persistence#fetchOutstandingSms(java.lang.String)
//     */
//    @Override
//    public List<PersistableSms> fetchOutstandingSms(ISDNAddressString msisdn) throws PersistenceException {
//        //This is performed on alertSC
//        //COLUMN_ADDR_DST_DIGITS == msisdn.getAddress() && COLUMN_IN_SYSTEM == false
//        final List<PersistableSms> schedulableSms = new ArrayList<PersistableSms>();
//        final IndexedSlicesQuery<UUID, String, ByteBuffer> query = HFactory.createIndexedSlicesQuery(keyspace,
//                new TimeUUIDSerializer(), SERIALIZER_STRING, new ByteBufferSerializer());
//        query.setColumnFamily(Schema.FAMILY_LIVE);
//        query.setColumnNames(Schema.COLUMNS_LIVE);
//        query.addEqualsExpression(Schema.COLUMN_ADDR_DST_DIGITS, SERIALIZER_STRING.toByteBuffer(msisdn.getAddress()));
//        query.addEqualsExpression(Schema.COLUMN_IN_SYSTEM, SERIALIZER_BOOLEAN.toByteBuffer(false));
//        
//
//        final QueryResult<OrderedRows<UUID, String, ByteBuffer>> result = query.execute();
//        final OrderedRows<UUID, String, ByteBuffer> rows = result.get();
//        final List<Row<UUID, String, ByteBuffer>> rowsList = rows.getList();
//        
//        for (Row<UUID, String, ByteBuffer> row : rowsList) {
//            final ColumnSlice<String, ByteBuffer> cSlice = row.getColumnSlice();
//            final UUID key = row.getKey();
//            try{
//                PersistableSms psms = createSms(key, true, cSlice);
//                schedulableSms.add(psms);
//            }catch(IOException e){
//                if(logger.isSevereEnabled()){
//                    logger.severe("Failed to deserialize SMS at key '"+key+"'!",e);
//                }
//            }
//        }
//
//        return schedulableSms;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.mobicents.smsc.slee.services.persistence.Persistence#fetchSchedulableSms()
//     */
//    @Override
//    public List<PersistableSms> fetchSchedulableSms() throws PersistenceException {
//        // COLUMN_IN_SYSTEM == false && COLUMN_DUE_DATE < now
//        // Damn index/slice queries required here...
//        final List<PersistableSms> schedulableSms = new ArrayList<PersistableSms>();
//        final IndexedSlicesQuery<UUID, String, ByteBuffer> query = HFactory.createIndexedSlicesQuery(keyspace,
//                new TimeUUIDSerializer(), SERIALIZER_STRING, new ByteBufferSerializer());
//        query.setColumnFamily(Schema.FAMILY_LIVE);
//        query.setColumnNames(Schema.COLUMNS_LIVE);
//        query.addEqualsExpression(Schema.COLUMN_IN_SYSTEM, SERIALIZER_BOOLEAN.toByteBuffer(false));
//        query.addGteExpression(Schema.COLUMN_SCHEDULE_DELIVERY, SERIALIZER_DATE.toByteBuffer(new Date()));
//
//        final QueryResult<OrderedRows<UUID, String, ByteBuffer>> result = query.execute();
//        final OrderedRows<UUID, String, ByteBuffer> rows = result.get();
//        final List<Row<UUID, String, ByteBuffer>> rowsList = rows.getList();
//        
//        for (Row<UUID, String, ByteBuffer> row : rowsList) {
//            final ColumnSlice<String, ByteBuffer> cSlice = row.getColumnSlice();
//            final UUID key = row.getKey();
//            try{
//                PersistableSms psms = createSms(key, true, cSlice);
//                schedulableSms.add(psms);
//            }catch(IOException e){
//                if(logger.isSevereEnabled()){
//                    logger.severe("Failed to deserialize SMS at key '"+key+"'!",e);
//                }
//            }
//        }
//
//        return schedulableSms;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see
//     * org.mobicents.smsc.slee.services.persistence.Persistence#passivate(org.mobicents.smsc.slee.services.persistence.PersistableSms
//     * )
//     */
//    @Override
//    public void passivate(PersistableSms psms) throws PersistenceException {
//        if (!(psms instanceof PersistableSmsImpl)) {
//            throw new PersistenceException("Wrong type of PersistableSms!");
//        }
//        PersistableSmsImpl persistableSms = (PersistableSmsImpl) psms;
//        if (persistableSms.getDbId() == null) {
//            throw new PersistenceException("Can not update record since its not in LIVE!");
//        }
//
//        Mutator<UUID> mutator = HFactory.createMutator(keyspace, new TimeUUIDSerializer());
//        mutator.addInsertion((UUID) persistableSms.getDbId(), Schema.FAMILY_LIVE,
//                HFactory.createColumn(Schema.COLUMN_DUE_DATE, psms.getDueDate(), SERIALIZER_STRING, SERIALIZER_DATE));
//        mutator.addInsertion((UUID) persistableSms.getDbId(), Schema.FAMILY_LIVE,
//                HFactory.createColumn(Schema.COLUMN_IN_SYSTEM, false, SERIALIZER_STRING, SERIALIZER_BOOLEAN));
//       
//        mutator.execute();
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see
//     * org.mobicents.smsc.slee.services.persistence.Persistence#activate(org.mobicents.smsc.slee.services.persistence.PersistableSms
//     * )
//     */
//    @Override
//    public void activate(PersistableSms psms) throws PersistenceException {
//        if (!(psms instanceof PersistableSmsImpl)) {
//            throw new PersistenceException("Wrong type of PersistableSms!");
//        }
//        PersistableSmsImpl persistableSms = (PersistableSmsImpl) psms;
//        if (persistableSms.getDbId() == null) {
//            throw new PersistenceException("Can not update record since its not in LIVE!");
//        }
//
//        Mutator<UUID> mutator = HFactory.createMutator(keyspace, new TimeUUIDSerializer());
//        mutator.addInsertion((UUID) persistableSms.getDbId(), Schema.FAMILY_LIVE,
//                HFactory.createColumn(Schema.COLUMN_IN_SYSTEM, true, SERIALIZER_STRING, SERIALIZER_BOOLEAN));
//        // TODO: address update required?
//        mutator.execute();
//    }
//
//    // ----------------------------------------
//    // SLEE Stuff
//    // ----------------------------------------
//    /*
//     * (non-Javadoc)
//     * 
//     * @see javax.slee.Sbb#sbbActivate()
//     */
//    @Override
//    public void sbbActivate() {
//        // TODO Auto-generated method stub
//
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see javax.slee.Sbb#sbbCreate()
//     */
//    @Override
//    public void sbbCreate() throws CreateException {
//        // TODO Auto-generated method stub
//
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see javax.slee.Sbb#sbbExceptionThrown(java.lang.Exception, java.lang.Object, javax.slee.ActivityContextInterface)
//     */
//    @Override
//    public void sbbExceptionThrown(Exception arg0, Object arg1, ActivityContextInterface arg2) {
//        // TODO Auto-generated method stub
//
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see javax.slee.Sbb#sbbLoad()
//     */
//    @Override
//    public void sbbLoad() {
//        // TODO Auto-generated method stub
//
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see javax.slee.Sbb#sbbPassivate()
//     */
//    @Override
//    public void sbbPassivate() {
//        // TODO Auto-generated method stub
//
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see javax.slee.Sbb#sbbPostCreate()
//     */
//    @Override
//    public void sbbPostCreate() throws CreateException {
//        // TODO Auto-generated method stub
//
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see javax.slee.Sbb#sbbRemove()
//     */
//    @Override
//    public void sbbRemove() {
//        // TODO Auto-generated method stub
//
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see javax.slee.Sbb#sbbRolledBack(javax.slee.RolledBackContext)
//     */
//    @Override
//    public void sbbRolledBack(RolledBackContext arg0) {
//        // TODO Auto-generated method stub
//
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see javax.slee.Sbb#sbbStore()
//     */
//    @Override
//    public void sbbStore() {
//        // TODO Auto-generated method stub
//
//    }

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

//    // ----------------------------------------
//    // Helper methods
//    // ----------------------------------------
//    private void createSccpAddress(final Mutator<UUID> mutator, final String FAMILY,final UUID key,final String PREFIX,final SccpAddress adr){
//
//        //COLUMN_AI_RI
//        mutator.addInsertion(key, FAMILY, HFactory.createColumn(PREFIX+Schema.COLUMN_AI_RI,adr.getAddressIndicator().getRoutingIndicator().getIndicator() ,SERIALIZER_STRING,SERIALIZER_INTEGER));
//        //COLUMN_AI_GT_I
//        mutator.addInsertion(key, FAMILY, HFactory.createColumn(PREFIX+Schema.COLUMN_AI_GT_I,adr.getAddressIndicator().getGlobalTitleIndicator().getValue() ,SERIALIZER_STRING,SERIALIZER_INTEGER));
//        if(adr.getAddressIndicator().getRoutingIndicator() == RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE){
//            final GlobalTitle gt = adr.getGlobalTitle();
//            final GlobalTitleIndicator gti = gt.getIndicator();
//            //NOTE: EncodingScheme is derived from digits LEN
//            switch(gti){
//                case GLOBAL_TITLE_INCLUDES_NATURE_OF_ADDRESS_INDICATOR_ONLY:
//                    //GT0001
//                    GT0001 gt0001 = (GT0001) gt;                    
//                    mutator.addInsertion(key, FAMILY, HFactory.createColumn(PREFIX+Schema.COLUMN_GT_NOA,gt0001.getNoA().getValue() ,SERIALIZER_STRING,SERIALIZER_INTEGER));
//                    break;
//                case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_ONLY:
//                    //GT0010
//                    GT0010 gt0010 = (GT0010) gt;
//                    mutator.addInsertion(key, FAMILY, HFactory.createColumn(PREFIX+Schema.COLUMN_GT_TT,gt0010.getTranslationType() ,SERIALIZER_STRING,SERIALIZER_INTEGER));
//                    break;
//                case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_AND_ENCODING_SCHEME:
//                    //GT0011
//                    GT0011 gt0011 = (GT0011) gt;
//                    mutator.addInsertion(key, FAMILY, HFactory.createColumn(PREFIX+Schema.COLUMN_GT_TT,gt0011.getTranslationType() ,SERIALIZER_STRING,SERIALIZER_INTEGER));
//                    mutator.addInsertion(key, FAMILY, HFactory.createColumn(PREFIX+Schema.COLUMN_GT_NP,gt0011.getNp().getValue() ,SERIALIZER_STRING,SERIALIZER_INTEGER));
//                break;
//                case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_ENCODING_SCHEME_AND_NATURE_OF_ADDRESS:
//                    //GT0100
//                    GT0100 gt0100 = (GT0100) gt;
//                    mutator.addInsertion(key, FAMILY, HFactory.createColumn(PREFIX+Schema.COLUMN_GT_NP,gt0100.getNumberingPlan().getValue() ,SERIALIZER_STRING,SERIALIZER_INTEGER));
//                    mutator.addInsertion(key, FAMILY, HFactory.createColumn(PREFIX+Schema.COLUMN_GT_TT,gt0100.getTranslationType() ,SERIALIZER_STRING,SERIALIZER_INTEGER));
//                    mutator.addInsertion(key, FAMILY, HFactory.createColumn(PREFIX+Schema.COLUMN_GT_NOA,gt0100.getNatureOfAddress().getValue() ,SERIALIZER_STRING,SERIALIZER_INTEGER));
//                    break;
//            }
//            //COLUMN_GT_NOA
//            //COLUMN_GT_TT
//            //COLUMN_GT_NP
//            //COLUMN_GT_DIGITS
//            mutator.addInsertion(key, FAMILY, HFactory.createColumn(PREFIX+Schema.COLUMN_GT_DIGITS,gt.getDigits() ,SERIALIZER_STRING,SERIALIZER_STRING));
//            //COLUMN_GT_INDICATOR
//            //mutator.addInsertion(key, FAMILY, HFactory.createColumn(PREFIX+Schema.COLUMN_GT_INDICATOR,gti.getValue() ,SERIALIZER_STRING,SERIALIZER_INTEGER));
//        } else{
//            //COLUMN_PC
//            mutator.addInsertion(key, FAMILY, HFactory.createColumn(PREFIX+Schema.COLUMN_PC,adr.getSignalingPointCode() ,SERIALIZER_STRING,SERIALIZER_INTEGER));
//            //COLUMN_SSN
//            mutator.addInsertion(key, FAMILY, HFactory.createColumn(PREFIX+Schema.COLUMN_SSN,adr.getSubsystemNumber() ,SERIALIZER_STRING,SERIALIZER_INTEGER));
//            //COLUMN_GT_INDICATOR
//            //mutator.addInsertion(key, FAMILY, HFactory.createColumn(PREFIX+Schema.COLUMN_GT_INDICATOR,GlobalTitleIndicator.NO_GLOBAL_TITLE_INCLUDED.getValue() ,SERIALIZER_STRING,SERIALIZER_INTEGER));
//        }
//    }
//    
//    private void createSms(final Mutator<UUID> mutator, final UUID archiveKey, final String FAMILY,final PersistableSms persistableSms,final boolean includeRuntime){
//        
//        final Sms sms = persistableSms.getEvent();
//        // COLUMN_MESSAGE_ID
//        if (sms.getMessageId() != null)
//            mutator.addInsertion(archiveKey, FAMILY,
//                    HFactory.createStringColumn(Schema.COLUMN_MESSAGE_ID, sms.getMessageId()));
//        // COLUMN_SYSTEM_ID
//        if (sms.getOrigSystemId() != null)
//            mutator.addInsertion(archiveKey, FAMILY,
//                    HFactory.createStringColumn(Schema.COLUMN_SYSTEM_ID, sms.getOrigSystemId()));
//        // COLUMN_ADDR_SRC_TON
//        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_ADDR_SRC_TON,
//                (int) sms.getSourceAddrTon(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//        // COLUMN_ADDR_SRC_NPI
//        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_ADDR_SRC_NPI,
//                (int) sms.getSourceAddrNpi(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//        // COLUMN_ADDR_SRC_DIGITS
//        mutator.addInsertion(archiveKey, FAMILY,
//                HFactory.createStringColumn(Schema.COLUMN_ADDR_SRC_DIGITS, sms.getSourceAddr()));
//
//        // COLUMN_ADDR_DST_TON
//        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_ADDR_DST_TON,
//                (int) sms.getDestAddrTon(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//        // COLUMN_ADDR_DST_NPI
//        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_ADDR_DST_NPI,
//                (int) sms.getDestAddrNpi(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//        // COLUMN_ADDR_DST_DIGITS
//        mutator.addInsertion(archiveKey, FAMILY,
//                HFactory.createStringColumn(Schema.COLUMN_ADDR_DST_DIGITS, sms.getDestAddr()));
//
//        // COLUMN_ESM_CLASS
//        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_ESM_CLASS,
//                (int) sms.getEsmClass(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//        // COLUMN_PROTOCOL_ID
//        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_PROTOCOL_ID,
//                (int) sms.getProtocolId(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//        // COLUMN_PRIORITY
//        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_PRIORITY,
//                (int) sms.getPriority(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//        // COLUMN_REGISTERED_DELIVERY
//        // TODO: XXX: improve SMS.getRegisteredDelivery() its [1,0], but its not set as boolean.
//        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_PRIORITY,
//                new Integer(sms.getRegisteredDelivery()) , SERIALIZER_STRING, IntegerSerializer.get()));
//        // COLUMN_REPLACE
//        // TODO: XXX: improve SMS.getReplaceIfPresent() its [1,0], but its not set as boolean.
//        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_REPLACE,
//                new Integer(sms.getReplaceIfPresent()) , SERIALIZER_STRING, IntegerSerializer.get()));
//        // COLUMN_DATA_CODING
//        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_DATA_CODING,
//                (int) sms.getDataCoding(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//        // COLUMN_DEFAULT_MSG_ID
//        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_DEFAULT_MSG_ID,
//                (int) sms.getDefaultMsgId(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//        // COLUMN_MESSAGE
//        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_MESSAGE, sms.getShortMessage(),
//                SERIALIZER_STRING, SERIALIZER_BYTE_ARRAY));
//        // COLUMN_OPTIONAL_PARAMETERS
//        if (sms.getOptionalParameterCount() > 0) {
//            // TODO: XXX: tricky stuff
//        }
//
//        // COLUMN_SUBMIT_DATE
//        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_SUBMIT_DATE, sms.getSubmitDate(),
//                SERIALIZER_STRING, SERIALIZER_DATE));
//        // COLUMN_SCHEDULE_DELIVERY
//        // TODO: XXX: ??
//        final Date scheduledDelivery = sms.getScheduleDeliveryTime();
//        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_SCHEDULE_DELIVERY,
//                scheduledDelivery, SERIALIZER_STRING, SERIALIZER_DATE));
//        // COLUMN_VALIDITY_PERIOD
//        // TODO: XXX: ??
//        final Date validityPeriod = sms.getValidityPeriod();
//        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_VALIDITY_PERIOD, validityPeriod,
//                SERIALIZER_STRING, SERIALIZER_DATE));
//        // COLUMN_EXPIRY_DATE
//        // TODO: XXX: XXX: what is that ?
//        // COLUMN_SM_TYPE
//        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_SM_TYPE, persistableSms.getType()
//                .getCode(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//        // COLUMN_DELIVERY_COUNT
//        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_DELIVERY_COUNT,
//                persistableSms.getDeliveryCount(), SERIALIZER_STRING, SERIALIZER_INTEGER));
////        // COLUMN_LAST_DELIVERY
////        mutator.addInsertion(archiveKey, FAMILY,
////                HFactory.createColumn(Schema.COLUMN_LAST_DELIVERY, new Date(), SERIALIZER_STRING, SERIALIZER_DATE));
//        if(includeRuntime){
//            // COLUMN_IN_SYSTEM
//            mutator.addInsertion(archiveKey, FAMILY,
//                    HFactory.createColumn(Schema.COLUMN_IN_SYSTEM, true, SERIALIZER_STRING, SERIALIZER_BOOLEAN));
//            // COLUMN_ALERTING_SUPPORTED
//            mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_ALERTING_SUPPORTED,
//                    persistableSms.isAlertingSupported(), SERIALIZER_STRING, SERIALIZER_BOOLEAN));
//            //DUE_DATE is set on passivation
//        }
//        // Address Creation.
//
//        ((PersistableSmsImpl)persistableSms).setDbId(archiveKey);
//        createSccpAddress(mutator, FAMILY,archiveKey,Schema.SCCP_PREFIX_SC, persistableSms.getScAddress());
//
//        if (persistableSms.getMxAddress() != null)
//            createSccpAddress(mutator, FAMILY,archiveKey,Schema.SCCP_PREFIX_MX, persistableSms.getMxAddress());
//   
//    }
//    /**
//     * @param sccpPrefixSc
//     * @param row
//     * @return
//     */
//    private SccpAddress createSccpAddress(String PREFIX, ColumnSlice<String, ByteBuffer> row) throws IOException{
//        SccpAddress address = null;
//        HColumn<String, ByteBuffer> cell;
//        
//        //COLUMN_AI_RI
//        cell = row.getColumnByName(PREFIX+Schema.COLUMN_AI_RI);
//        final RoutingIndicator routingIndicator = RoutingIndicator.valueOf(SERIALIZER_INTEGER.fromByteBuffer(cell.getValue()));
//        if(routingIndicator == RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE){
//            GlobalTitle globalTitle;
//            //COLUMN_GT_INDICATOR
//            cell = row.getColumnByName(PREFIX+Schema.COLUMN_AI_GT_I);
//            final GlobalTitleIndicator gti = GlobalTitleIndicator.valueOf(SERIALIZER_INTEGER.fromByteBuffer(cell.getValue()));
//            //COLUMN_GT_DIGITS
//            cell = row.getColumnByName(PREFIX+Schema.COLUMN_GT_DIGITS);
//            final String digits = SERIALIZER_STRING.fromByteBuffer(cell.getValue());
//            //NOTE: EncodingScheme is derived from digits LEN
//
//            switch(gti){
//                case GLOBAL_TITLE_INCLUDES_NATURE_OF_ADDRESS_INDICATOR_ONLY:
//                    //GT0001
//                    cell = row.getColumnByName(PREFIX+Schema.COLUMN_GT_NOA);
//                    NatureOfAddress natureOfAddress = NatureOfAddress.valueOf(SERIALIZER_INTEGER.fromByteBuffer(cell.getValue()));
//                    globalTitle = GT0001.getInstance(natureOfAddress,digits);
//                    break;
//                case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_ONLY:
//                    //GT0010
//                    cell = row.getColumnByName(PREFIX+Schema.COLUMN_GT_TT);
//                    int translationType = SERIALIZER_INTEGER.fromByteBuffer(cell.getValue());
//                    globalTitle = GT0010.getInstance(translationType,digits);
//                    break;
//                case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_AND_ENCODING_SCHEME:
//                    //GT0011
//                    cell = row.getColumnByName(PREFIX+Schema.COLUMN_GT_TT);
//                    translationType = SERIALIZER_INTEGER.fromByteBuffer(cell.getValue());
//                    cell = row.getColumnByName(PREFIX+Schema.COLUMN_GT_NP);
//                    NumberingPlan numberingPlan = NumberingPlan.valueOf(SERIALIZER_INTEGER.fromByteBuffer(cell.getValue()));
//                    globalTitle = GT0011.getInstance(translationType, numberingPlan, digits);
//                break;
//                case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_ENCODING_SCHEME_AND_NATURE_OF_ADDRESS:
//                    //GT0100
//                    cell = row.getColumnByName(PREFIX+Schema.COLUMN_GT_NOA);
//                    natureOfAddress = NatureOfAddress.valueOf(SERIALIZER_INTEGER.fromByteBuffer(cell.getValue()));
//                    cell = row.getColumnByName(PREFIX+Schema.COLUMN_GT_NP);
//                    numberingPlan = NumberingPlan.valueOf(SERIALIZER_INTEGER.fromByteBuffer(cell.getValue()));
//                    cell = row.getColumnByName(PREFIX+Schema.COLUMN_GT_TT);
//                    translationType = SERIALIZER_INTEGER.fromByteBuffer(cell.getValue());
//                    globalTitle = GT0100.getInstance(translationType, numberingPlan, natureOfAddress, digits);
//                    break;
//                    default:
//                        throw new IllegalArgumentException("Wrong GT type '"+gti+"'");
//            }
//            address = new SccpAddress(routingIndicator,0,globalTitle,0);
//            
//            
//        } else{
//            //COLUMN_PC
//            cell = row.getColumnByName(PREFIX+Schema.COLUMN_PC);
//            final int dpc = SERIALIZER_INTEGER.fromByteBuffer(cell.getValue());
//            //COLUMN_SSN
//            cell = row.getColumnByName(PREFIX+Schema.COLUMN_SSN);
//            final int ssn = SERIALIZER_INTEGER.fromByteBuffer(cell.getValue());
//
//
//            address = new SccpAddress(routingIndicator,dpc,null,ssn);
//        }
//        return address;
//    }
//    
//    private PersistableSms createSms(final UUID archiveKey,final boolean includeRuntime, final ColumnSlice<String, ByteBuffer> row ) throws IOException{
//        Sms smsEvent = new Sms();
//        
//        HColumn<String, ByteBuffer> cell;
//        // COLUMN_MESSAGE_ID
//        
//        cell = row.getColumnByName(Schema.COLUMN_MESSAGE_ID);
//        if(cell != null && cell.getValueBytes() != null && cell.getValueBytes().limit()>0){
//            final String messageId = SERIALIZER_STRING.fromByteBuffer(cell.getValue());
//            smsEvent.setMessageId(messageId);
//        }
//        // COLUMN_SYSTEM_ID
//        cell = row.getColumnByName(Schema.COLUMN_SYSTEM_ID);
//        if(cell != null && cell.getValueBytes() != null && cell.getValueBytes().limit()>0){
//           final String systemId = SERIALIZER_STRING.fromByteBuffer(cell.getValue());
//           smsEvent.setOrigSystemId(systemId);
//        }
//
//        // COLUMN_ADDR_SRC_TON
//        cell = row.getColumnByName(Schema.COLUMN_ADDR_SRC_TON);
//        final byte addrSrcTon = (byte) SERIALIZER_INTEGER.fromByteBuffer(cell.getValue()).intValue();
//        smsEvent.setSourceAddrTon(addrSrcTon);
//        // COLUMN_ADDR_SRC_NPI
//        cell = row.getColumnByName(Schema.COLUMN_ADDR_SRC_NPI);
//        final byte addrSrcNpi = (byte) SERIALIZER_INTEGER.fromByteBuffer(cell.getValue()).intValue();
//        smsEvent.setSourceAddrNpi(addrSrcNpi);
//        // COLUMN_ADDR_SRC_DIGITS
//        cell = row.getColumnByName(Schema.COLUMN_ADDR_SRC_DIGITS);
//        final String addrSrcDigits = SERIALIZER_STRING.fromByteBuffer(cell.getValue());
//        smsEvent.setSourceAddr(addrSrcDigits);
//         // COLUMN_ADDR_DST_TON
//        cell = row.getColumnByName(Schema.COLUMN_ADDR_DST_TON);
//        final byte addrDstTon = (byte) SERIALIZER_INTEGER.fromByteBuffer(cell.getValue()).intValue();
//        smsEvent.setDestAddrTon(addrDstTon);
//        // COLUMN_ADDR_DST_NPI
//        cell = row.getColumnByName(Schema.COLUMN_ADDR_DST_NPI);
//        final byte addrDstNpi = (byte) SERIALIZER_INTEGER.fromByteBuffer(cell.getValue()).intValue();
//        smsEvent.setDestAddrNpi(addrDstNpi);
//        // COLUMN_ADDR_DST_DIGITS
//        cell = row.getColumnByName(Schema.COLUMN_ADDR_DST_DIGITS);
//        final String addrDstDigits = SERIALIZER_STRING.fromByteBuffer(cell.getValue());
//        smsEvent.setDestAddr(addrDstDigits);
//        // COLUMN_ESM_CLASS
//        cell = row.getColumnByName(Schema.COLUMN_ESM_CLASS);
//        final byte esmClass = (byte) SERIALIZER_INTEGER.fromByteBuffer(cell.getValue()).intValue();
//        smsEvent.setEsmClass(esmClass);
//        // COLUMN_PROTOCOL_ID
//        cell = row.getColumnByName(Schema.COLUMN_PROTOCOL_ID);
//        final byte protocolId = (byte) SERIALIZER_INTEGER.fromByteBuffer(cell.getValue()).intValue();
//        smsEvent.setProtocolId(protocolId);
//        // COLUMN_PRIORITY
//        cell = row.getColumnByName(Schema.COLUMN_PRIORITY);
//        final byte priority = (byte) SERIALIZER_INTEGER.fromByteBuffer(cell.getValue()).intValue();
//        smsEvent.setPriority(priority);
//        // COLUMN_REGISTERED_DELIVERY
//        // TODO: XXX: improve SMS.getRegisteredDelivery() its [1,0], but its not set as boolean.
//        cell = row.getColumnByName(Schema.COLUMN_REGISTERED_DELIVERY);
//        final byte registeredDelivery =  (SERIALIZER_INTEGER.fromByteBuffer(cell.getValue())).byteValue();
//        smsEvent.setRegisteredDelivery(registeredDelivery);
//        // COLUMN_REPLACE
//        // TODO: XXX: improve SMS.getReplaceIfPresent() its [1,0], but its not set as boolean.
//        cell = row.getColumnByName(Schema.COLUMN_REPLACE);
//        final byte replace =  (SERIALIZER_INTEGER.fromByteBuffer(cell.getValue())).byteValue();
//        smsEvent.setReplaceIfPresent(replace);
//        // COLUMN_DATA_CODING
//        cell = row.getColumnByName(Schema.COLUMN_DATA_CODING);
//        final byte dataCoding =  (byte)(SERIALIZER_BOOLEAN.fromByteBuffer(cell.getValue()) ? 1 : 0);
//        smsEvent.setDataCoding(dataCoding);
//        // COLUMN_DEFAULT_MSG_ID
//        cell = row.getColumnByName(Schema.COLUMN_DEFAULT_MSG_ID);
//        final byte defaultMsgId =  (byte)(SERIALIZER_BOOLEAN.fromByteBuffer(cell.getValue()) ? 1 : 0);
//        smsEvent.setDefaultMsgId(defaultMsgId);
//        // COLUMN_MESSAGE
//        cell = row.getColumnByName(Schema.COLUMN_MESSAGE);
//        final byte[] message = SERIALIZER_BYTE_ARRAY.fromByteBuffer(cell.getValue());
//        smsEvent.setShortMessage(message);
//        // COLUMN_OPTIONAL_PARAMETERS
//        cell = row.getColumnByName(Schema.COLUMN_OPTIONAL_PARAMETERS);
//        if (false) {
//            // TODO: XXX: tricky stuff
//        }
//
//        // COLUMN_SUBMIT_DATE
//        cell = row.getColumnByName(Schema.COLUMN_SUBMIT_DATE);
//        final Date submitDate =  SERIALIZER_DATE.fromByteBuffer(cell.getValue());
//        smsEvent.setSubmitDate(submitDate);
//        // COLUMN_SCHEDULE_DELIVERY
//        // TODO: XXX: this is string type in SmsEvent
//        cell = row.getColumnByName(Schema.COLUMN_SCHEDULE_DELIVERY);
//        final Date scheduleDeliveryTime =  SERIALIZER_DATE.fromByteBuffer(cell.getValue());
//        smsEvent.setScheduleDeliveryTime(scheduleDeliveryTime);
//        // COLUMN_VALIDITY_PERIOD
//        // TODO: XXX: this is string type in SmsEvent
//        cell = row.getColumnByName(Schema.COLUMN_VALIDITY_PERIOD);
//        final Date validityPeriod =  SERIALIZER_DATE.fromByteBuffer(cell.getValue());
//        smsEvent.setValidityPeriod(validityPeriod);
//        // COLUMN_EXPIRY_DATE
//        // TODO: XXX: XXX: what is that ?
//        
//        
//
//        final PersistableSmsImpl psms = new PersistableSmsImpl();      
//        psms.setDbId(archiveKey);
//        psms.setEvent(smsEvent);
//        // COLUMN_SM_TYPE
//        cell = row.getColumnByName(Schema.COLUMN_SM_TYPE);
//        final SmType smType =  SmType.fromInt(SERIALIZER_INTEGER.fromByteBuffer(cell.getValue()));
//        psms.setType(smType);
//        // COLUMN_DELIVERY_COUNT
//        cell = row.getColumnByName(Schema.COLUMN_DELIVERY_COUNT);
//        final int deliveryCount =  SERIALIZER_INTEGER.fromByteBuffer(cell.getValue());
//        psms.setDeliveryCount(deliveryCount);
//        // COLUMN_LAST_DELIVERY
////        mutator.addInsertion(archiveKey, FAMILY,
////                HFactory.createColumn(Schema.COLUMN_LAST_DELIVERY, new Date(), SERIALIZER_STRING, SERIALIZER_DATE));
//
//        if(includeRuntime){
////            // COLUMN_IN_SYSTEM
////            cell = row.getColumnByName(Schema.COLUMN_IN_SYSTEM);
////            final boolean inSystem =  SERIALIZER_BOOLEAN.fromByteBuffer(cell.getValue());
//            // COLUMN_ALERTING_SUPPORTED
//            cell = row.getColumnByName(Schema.COLUMN_ALERTING_SUPPORTED);
//            final boolean alertingSupported =  SERIALIZER_BOOLEAN.fromByteBuffer(cell.getValue());
//            psms.setAlertingSupported(alertingSupported);
//            //DUE_DATE is set on passivation
//            cell = row.getColumnByName(Schema.COLUMN_DUE_DATE);
//            final Date dueDate =  SERIALIZER_DATE.fromByteBuffer(cell.getValue());
//            psms.setDueDate(dueDate);
//        }
//
//        // Address Creation.
//        SccpAddress scAddress = createSccpAddress(Schema.SCCP_PREFIX_SC, row);
//        SccpAddress mxAddress = createSccpAddress(Schema.SCCP_PREFIX_MX, row);
//        psms.setScAddress(scAddress);
//        if(mxAddress!=null){
//            psms.setMxAddress(mxAddress);
//        }
//        return psms;
//    }
//    
//, MX_GT_NOA  , MX_GT_TT , MX_GT_NP  , MX_GT_DIGITS  ,
//, SC_GT_NOA  , SC_GT_TT , SC_GT_NP  , SC_GT_DIGITS 
}
