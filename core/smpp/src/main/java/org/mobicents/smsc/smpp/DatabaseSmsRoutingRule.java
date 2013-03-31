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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.cloudhopper.smpp.SmppConstants;

/**
 * @author Amit Bhayani
 * 
 */
public class DatabaseSmsRoutingRule implements SmsRoutingRule {

	private static final Logger logger = Logger.getLogger(DatabaseSmsRoutingRule.class);

	private static final String CREATE_STATEMENT = "CREATE TABLE SmsRoutingRule (id int, address varchar(20), systemid varchar(20))";
	private static final String SELECT_STATEMENT = "select * from SmsRoutingRule where address = '%s'";

	private static final String COLUMN_SYSTEM_ID = "systemid";

	private DataSource ds;

	private String username = "root";
	private String password = "Green725210!";

	private static final Pattern pattern = Pattern.compile("(([\\+]?[1])|[0]?)");

	private static final String USA_COUNTRY_CODE = "1";

	/**
	 * 
	 */
	public DatabaseSmsRoutingRule() {
		this.init();

		this.createTable();
	}

	private void init() {
		Context ctx;
		try {
			ctx = new InitialContext();
			ds = (DataSource) ctx.lookup("java:/DefaultDS");
		} catch (NamingException e) {
			logger.error("Error while looking up DataSource DefaultDS", e);
		}

	}

	private void createTable() {
		Connection con = null;
		PreparedStatement pstmt = null;

		try {
			con = ds.getConnection(username, password);
			pstmt = con.prepareStatement(CREATE_STATEMENT);
			pstmt.executeUpdate();
			pstmt.close();

			logger.info("Successfully created table SmsRoutingRule");
		} catch (SQLException e) {
			logger.error("Error while crating table SmsRoutingRule", e);
		} finally {
			if (con != null)
				try {
					con.close();
				} catch (SQLException e) {

				}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SmsRoutingRule#getSystemId(byte, byte,
	 * java.lang.String)
	 */
	@Override
	public String getSystemId(byte ton, byte npi, String address) {

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

		Connection con = null;
		Statement stmt;

		String systemId = null;

		try {
			con = ds.getConnection(username, password);
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(String.format(SELECT_STATEMENT, address));

			if (rs.next()) {
				systemId = rs.getString(COLUMN_SYSTEM_ID);
			} else {
				systemId ="icg_sms_y";
			}
			stmt.close();

		} catch (SQLException e) {
			logger.error("Error while selecting from table SmsRoutingRule", e);
		} finally {
			if (con != null)
				try {
					con.close();
				} catch (SQLException e) {
				}
		}
		// TODO Auto-generated method stub
		return systemId;
	}

}
