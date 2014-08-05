package org.mobicents.protocols.smpp.load;

import org.apache.log4j.Logger;

import com.cloudhopper.smpp.SmppServer;
import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.SmppProcessingException;

public class SlowServer extends TestHarness {

	private static Logger logger = Logger.getLogger(SlowServer.class);
	private static final long DELAY_BEFORE_RESPONSE = 3000;

	public SlowServer() {

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception  {
		SlowServer ss = new SlowServer();
		ss.test();
	}

	private void test() throws Exception {
		SmppServerConfiguration configuration = new SmppServerConfiguration();
		configuration.setPort(2775);
		configuration.setMaxConnectionSize(10);
		configuration.setNonBlockingSocketsEnabled(false);

		SmppServer smppServer = new DefaultSmppServer(configuration, new DefaultSmppServerHandler());

		logger.info("About to start SMPP slow server");
		smppServer.start();
		logger.info("SMPP slow server started");

		System.out.println("Press any key to stop server");
		System.in.read();

		logger.info("SMPP server stopping");
		smppServer.stop();
		logger.info("SMPP server stopped");
	}

	public static class DefaultSmppServerHandler implements SmppServerHandler {
		@Override
		public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration,
				final BaseBind bindRequest) throws SmppProcessingException {
			// this name actually shows up as thread context....
			sessionConfiguration.setName("Application.SMPP." + sessionId);
		}

		@Override
		public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse)
				throws SmppProcessingException {
			logger.info("Session created: " + session);
			// need to do something it now (flag we're ready)
			session.serverReady(new SlowSmppSessionHandler());
		}

		@Override
		public void sessionDestroyed(Long sessionId, SmppServerSession session) {
			logger.info("Session destroyed: " + session);
		}

	}

	public static class SlowSmppSessionHandler extends DefaultSmppSessionHandler {
		@Override
		public PduResponse firePduRequestReceived(PduRequest pduRequest) {
			try {
				Thread.sleep(DELAY_BEFORE_RESPONSE);
			} catch (Exception e) {
			}

			// ignore for now (already logged)
			return pduRequest.createResponse();
		}
	}

}
