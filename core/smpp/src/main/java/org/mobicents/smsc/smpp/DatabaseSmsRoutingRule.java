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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.prettyprint.cassandra.model.ConfigurableConsistencyLevel;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;

import org.apache.log4j.Logger;

import com.cloudhopper.smpp.SmppConstants;

/**
 * @author Amit Bhayani
 * 
 */
public class DatabaseSmsRoutingRule implements SmsRoutingRule {

	private static final Logger logger = Logger.getLogger(DatabaseSmsRoutingRule.class);

    private Cluster cluster = null;
	private Keyspace keyspace = null;

	private static final Pattern pattern = Pattern.compile("(([\\+]?[1])|[0]?)");

	private static final String USA_COUNTRY_CODE = "1";

	/**
	 * 
	 */
	public DatabaseSmsRoutingRule() {
		this.init();
	}

	private void init() {
        try {
            SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
            cluster = HFactory.getOrCreateCluster(smscPropertiesManagement.getClusterName(), smscPropertiesManagement.getHosts());
            ConfigurableConsistencyLevel ccl = new ConfigurableConsistencyLevel();
            ccl.setDefaultReadConsistencyLevel(HConsistencyLevel.ONE);
            keyspace = HFactory.createKeyspace(smscPropertiesManagement.getKeyspaceName(), cluster, ccl);
        } catch (Exception e) {
            logger.error("Error initializing cassandra database for DatabaseSmsRoutingRule", e);
        }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SmsRoutingRule#getSystemId(byte, byte,
	 * java.lang.String)
	 */
	@Override
	public String getEsmeClusterName(int ton, int npi, String address) {

		// lets convert national to international
		if (ton == SmppConstants.TON_NATIONAL) {
			String origAddress = address;
			Matcher matcher = pattern.matcher(address);
			address = matcher.replaceFirst(USA_COUNTRY_CODE);

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Converted national address=%s to international address=%s", origAddress,
						address));
			}
		}

		String systemId = null;

		try {
            DbSmsRoutingRule rr = DBOperations.fetchSmsRoutingRule(keyspace, address);
            if (rr != null) {
                systemId = rr.getSystemId();
            } else {
                systemId = "icg_sms_y";
            }
		} catch (PersistenceException e) {
			logger.error("PersistenceException while selecting from table SmsRoutingRule", e);
		}

		return systemId;
	}

}
