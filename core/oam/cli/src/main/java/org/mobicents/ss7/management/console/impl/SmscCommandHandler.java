package org.mobicents.ss7.management.console.impl;

import org.mobicents.ss7.management.console.CommandContext;
import org.mobicents.ss7.management.console.CommandHandlerWithHelp;
import org.mobicents.ss7.management.console.Tree;
import org.mobicents.ss7.management.console.Tree.Node;

/**
 * @author amit bhayani
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

		Node get = parent.addChild("get");
		get.addChild("scgt");
		get.addChild("scssn");
		get.addChild("hlrssn");
		get.addChild("mscssn");
		get.addChild("maxmapv");

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
