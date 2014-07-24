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

package org.mobicents.smsc.domain;

/**
 * 
 * @author Amit Bhayani
 * 
 */
public interface SMSCOAMMessages {

	/**
	 * Pre defined messages
	 */
	public static final String INVALID_COMMAND = "Invalid Command";

	public static final String ILLEGAL_ARGUMENT = "Illegal argument %s: %s";

	/*
	 * public static final String ADD_ROUTING_RULE_SUCESSFULL =
	 * "Successfully added Routing rule name=%s";
	 * 
	 * public static final String ADD_ROUTING_RULE_FAIL_NO_SYSTEM_ID =
	 * "Creation of Routing rule failed, as no ESME added with the System Id name=%s"
	 * ;
	 */

	public static final String ESME_START_SUCCESSFULL = "Successfully started ESME name=%s";

	public static final String ESME_STOP_SUCCESSFULL = "Successfully stopped ESME name=%s";

	public static final String CREATE_ESME_SUCCESSFULL = "Successfully created ESME name=%s";
	
	public static final String MODIFY_ESME_SUCCESSFULL = "Successfully modified ESME name=%s";

	public static final String CREATE_ROUTING_RULE_SUCCESSFULL = "Successfully created Routing rule name=%s";

	public static final String CREATE_ROUTING_RULE_FAIL_ALREADY_EXIST = "Creation of Routing rule failed. Other Route with name=%s already exist"; // name
																																					// =

	public static final String UPDATE_DATABASE_RULE_SUCCESSFULL = "Successfully updated databaseRule address=%s";

	public static final String DELETE_DATABASE_RULE_SUCCESSFULL = "Successfully deleted databaseRule address=%s";

	public static final String ACCEPTED_ARCHIVE_GENERATE_CDR_SUCCESSFULL = "Successfully accepted request to generate CDR";

	public static final String BAD_FORMATTED_FROM_FIELD = "Bad formatted dateFrom field";

	public static final String BAD_FORMATTED_TO_FIELD = "Bad formatted dateTo field";
	// systemid

	public static final String NOT_SUPPORTED_YET = "Not supported yet";

	public static final String NO_ESME_DEFINED_YET = "No ESME defined yet";

	public static final String NO_ROUTING_RULE_DEFINED_YET = "No Routing rule defined yet for address %s and type %s";

	public static final String DELETE_ESME_SUCCESSFUL = "Successfully deleted Esme with given name %s";

	public static final String INVALID_SMPP_BIND_TYPE = "Invalid SMPP Bind Type %s. Allowed are TRANSCEIVER, TRANSMITTER or RECEIVER";

	public static final String NULL_ESME_ADDRESS_RANGE = "esme-range is compulsory for TRANSCEIVER and RECEIVER";

	public static final String PARAMETER_SUCCESSFULLY_SET = "Parameter has been successfully set";

	public static final String PARAMETER_SUCCESSFULLY_REMOVED = "Parameter has been successfully removed";

	public static final String SMPP_SERVER_PARAMETER_SUCCESSFULLY_SET = "Parameter has been successfully set. The changed value will take effect after SmppServer is restarted";

	public static final String MAP_VERSION_CACHE_NOT_FOUND = "No map version found in the cache";

	public static final String MAP_VERSION_CACHE_INVALID_VERSION = "Invalid version passed, valid values are 1,2 or 3";

	public static final String MAP_VERSION_CACHE_SUCCESSFULLY_SET = "Successfully set map version cache";

	public static final String MAP_VERSION_CACHE_SUCCESSFULLY_CLEARED = "Successfully cleared version cache";

	public static final String CREATE_SIP_FAIL_ALREADY_EXIST = "Creation of SIP failed. Other SIP with name=%s already exist";

	public static final String SIP_NOT_FOUND = "No Sip found with given name %s";

	public static final String SIP_MODIFY_SUCCESS = "Successfully modified SIP name %s";

	public static final String NO_SIP_DEFINED_YET = "No SIP defined yet";

    public static final String NO_DATABASE_SMS_ROUTING_RULE = "DatabaseSmsRoutingRule is not used";

    public static final String REMOVING_LIVE_ARCHIVE_TABLES_DAYS_BAD_VALUES = "Value of removingLiveTablesDays or removingArcjiveTablesDays must be 0 (disabling of the feature) or >=3";

	/**
	 * Generic constants
	 */
	public static final String TAB = "        ";

	public static final String NEW_LINE = "\n";

	public static final String COMMA = ",";

	/**
	 * Show command specific constants
	 */
	public static final String SHOW_ASSIGNED_TO = "Assigned to :\n";

	public static final String SHOW_COUNTERS_ENABLED = " countersEnabled";

	public static final String SHOW_ESME_TON = " ton=";

	public static final String SHOW_ESME_NPI = " npi=";

	public static final String SHOW_ESME_ADDRESS_RANGE = " addressRange=";

	public static final String SHOW_ROUTING_RULE_NAME = "Routing rule name=";

	public static final String SHOW_STARTED = " started=";

	public static final String SHOW_ADDRESS_RANGE = " range=";

	public static final String SHOW_SIP_NAME = "SIP name=";

}
