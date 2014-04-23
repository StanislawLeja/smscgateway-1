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

package org.mobicents.ss7.management.console.impl;

import org.mobicents.ss7.management.console.CommandContext;
import org.mobicents.ss7.management.console.CommandHandlerWithHelp;
import org.mobicents.ss7.management.console.Tree;
import org.mobicents.ss7.management.console.Tree.Node;

/**
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public class SmscCommandHandler extends CommandHandlerWithHelp {

	static final Tree commandTree = new Tree("smsc");
	static {
		Node parent = commandTree.getTopNode();
		
		Node sip = parent.addChild("sip");
		sip.addChild("modify");
		sip.addChild("show");

		Node esme = parent.addChild("esme");
		esme.addChild("create");
		esme.addChild("modify");
		esme.addChild("delete");
		esme.addChild("start");
		esme.addChild("stop");
		esme.addChild("show");

		Node set = parent.addChild("set");
		set.addChild("scgt");
		set.addChild("scssn");
		set.addChild("hlrssn");
		set.addChild("mscssn");
        set.addChild("maxmapv");
        set.addChild("defaultvalidityperiodhours");
        set.addChild("maxvalidityperiodhours");
        set.addChild("defaultton");
        set.addChild("defaultnpi");
        set.addChild("subscriberbusyduedelay");
        set.addChild("firstduedelay");
        set.addChild("secondduedelay");
        set.addChild("maxduedelay");
        set.addChild("duedelaymultiplicator");
        set.addChild("maxmessagelengthreducer");
        set.addChild("smshomerouting");
        set.addChild("revisesecondsonsmscstart");
        set.addChild("processingsmssettimeout");
        set.addChild("generatereceiptcdr");
        set.addChild("mocharging");
        Node txsmppcharging = set.addChild("txsmppcharging");
        Node txsipcharging = set.addChild("txsipcharging");
        set.addChild("diameterdestrealm");
        set.addChild("diameterdesthost");
        set.addChild("diameterdestport");
        set.addChild("diameterusername");
        set.addChild("removinglivetablesdays");
        set.addChild("removingarchivetablesdays");

        txsmppcharging.addChild("None");
        txsmppcharging.addChild("Selected");
        txsmppcharging.addChild("All");
        
        txsipcharging.addChild("None");
        txsipcharging.addChild("Selected");
        txsipcharging.addChild("All");

        Node smppencodingforucs2 = set.addChild("smppencodingforucs2");
        smppencodingforucs2.addChild("utf8");
        smppencodingforucs2.addChild("unicode");
        set.addChild("hosts");
        set.addChild("keyspacename");
        set.addChild("clustername");
        set.addChild("fetchperiod");
        set.addChild("fetchmaxrows");
        set.addChild("maxactivitycount");
//        set.addChild("cdrdatabaseexportduration");
        set.addChild("esmedefaultcluster");

		Node get = parent.addChild("get");
		get.addChild("scgt");
		get.addChild("scssn");
		get.addChild("hlrssn");
		get.addChild("mscssn");
		get.addChild("maxmapv");
        get.addChild("defaultvalidityperiodhours");
        get.addChild("maxvalidityperiodhours");
        get.addChild("defaultton");
        get.addChild("defaultnpi");
        get.addChild("subscriberbusyduedelay");
        get.addChild("firstduedelay");
        get.addChild("secondduedelay");
        get.addChild("maxduedelay");
        get.addChild("duedelaymultiplicator");
        get.addChild("maxmessagelengthreducer");
        get.addChild("smshomerouting");
        get.addChild("revisesecondsonsmscstart");
        get.addChild("processingsmssettimeout");
        get.addChild("generatereceiptcdr");
        get.addChild("mocharging");
        get.addChild("txsmppcharging");
        get.addChild("diameterdestrealm");
        get.addChild("diameterdesthost");
        get.addChild("diameterdestport");
        get.addChild("diameterusername");
        get.addChild("removinglivetablesdays");
        get.addChild("removingarchivetablesdays");

        smppencodingforucs2 = get.addChild("smppencodingforucs2");
        smppencodingforucs2.addChild("utf8");
        smppencodingforucs2.addChild("unicode");
        get.addChild("hosts");
        get.addChild("keyspacename");
        get.addChild("clustername");
        get.addChild("fetchperiod");
        get.addChild("fetchmaxrows");
        get.addChild("maxactivitycount");
//        get.addChild("cdrdatabaseexportduration");
        get.addChild("esmedefaultcluster");
        
        Node remove = parent.addChild("remove");
        remove.addChild("esmedefaultcluster");

		Node smppServer = parent.addChild("smppserver");

		Node smppServerSet = smppServer.addChild("set");
		smppServerSet.addChild("port");
		smppServerSet.addChild("bindtimeout");
		smppServerSet.addChild("systemid");
		smppServerSet.addChild("autonegotiateversion");
		smppServerSet.addChild("interfaceversion");
		smppServerSet.addChild("maxconnectionsize");
		smppServerSet.addChild("defaultwindowsize");
		smppServerSet.addChild("defaultwindowwaittimeout");
		smppServerSet.addChild("defaultrequestexpirytimeout");
		smppServerSet.addChild("defaultwindowmonitorinterval");
		smppServerSet.addChild("defaultsessioncountersenabled");

		Node smppServerGet = smppServer.addChild("get");
		smppServerGet.addChild("port");
		smppServerGet.addChild("bindtimeout");
		smppServerGet.addChild("systemid");
		smppServerGet.addChild("autonegotiateversion");
		smppServerGet.addChild("interfaceversion");
		smppServerGet.addChild("maxconnectionsize");
		smppServerGet.addChild("defaultwindowsize");
		smppServerGet.addChild("defaultwindowwaittimeout");
		smppServerGet.addChild("defaultrequestexpirytimeout");
		smppServerGet.addChild("defaultwindowmonitorinterval");
		smppServerGet.addChild("defaultsessioncountersenabled");

        Node databaseRule = parent.addChild("databaserule");
        databaseRule.addChild("update");
        databaseRule.addChild("delete");
        databaseRule.addChild("get");
        databaseRule.addChild("getrange");
        
        Node mapcache = parent.addChild("mapcache");
        mapcache.addChild("get");
        mapcache.addChild("set");
        mapcache.addChild("clear");

        Node stat = parent.addChild("stat");
        stat.addChild("get");

	};

	public SmscCommandHandler() {
		super(commandTree, CONNECT_MANDATORY_FLAG);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.ss7.management.console.CommandHandler#isValid(java.lang
	 * .String)
	 */
	@Override
	public void handle(CommandContext ctx, String commandLine) {
		// TODO Validate command
		if (commandLine.contains("--help")) {
			this.printHelp(commandLine, ctx);
			return;
		}

		ctx.sendMessage(commandLine);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.ss7.management.console.CommandHandler#isAvailable(org.mobicents
	 * .ss7.management.console.CommandContext)
	 */
	@Override
	public boolean isAvailable(CommandContext ctx) {
		if (!ctx.isControllerConnected()) {
			ctx.printLine("The command is not available in the current context. Please connnect first");
			return false;
		}
		return true;
	}

}
