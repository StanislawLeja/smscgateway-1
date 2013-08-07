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

		Node esme = parent.addChild("esme");
		esme.addChild("create");
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
        set.addChild("hosts");
        set.addChild("keyspacename");
        set.addChild("clusterName");
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
        get.addChild("hosts");
        get.addChild("keyspacename");
        get.addChild("clusterName");
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
		smppServerSet.addChild("bind-timeout");
		smppServerSet.addChild("system-id");
		smppServerSet.addChild("auto-negotiate-version");
		smppServerSet.addChild("interface-version");
		smppServerSet.addChild("max-connection-size");
		smppServerSet.addChild("default-window-size");
		smppServerSet.addChild("default-window-wait-timeout");
		smppServerSet.addChild("default-request-expiry-timeout");
		smppServerSet.addChild("default-window-monitor-interval");
		smppServerSet.addChild("default-session-counters-enabled");

		Node smppServerGet = smppServer.addChild("get");
		smppServerGet.addChild("port");
		smppServerGet.addChild("bind-timeout");
		smppServerGet.addChild("system-id");
		smppServerGet.addChild("auto-negotiate-version");
		smppServerGet.addChild("interface-version");
		smppServerGet.addChild("max-connection-size");
		smppServerGet.addChild("default-window-size");
		smppServerGet.addChild("default-window-wait-timeout");
		smppServerGet.addChild("default-request-expiry-timeout");
		smppServerGet.addChild("default-window-monitor-interval");
		smppServerGet.addChild("default-session-counters-enabled");

        Node databaseRule = parent.addChild("databaserule");
        databaseRule.addChild("update");
        databaseRule.addChild("delete");
        databaseRule.addChild("get");
        databaseRule.addChild("getrange");

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
