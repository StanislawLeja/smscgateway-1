package org.mobicents.smsc.smpp.ihub;

import java.nio.channels.ClosedChannelException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.commons.util.DecimalUtil;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

public class PerformanceClientMain {

	private static final Logger logger = Logger.getLogger(PerformanceClientMain.class);

	//
	// performance testing options (just for this sample)
	//
	// total number of sessions (conns) to create
	private int sessionCount = 5;
	// size of window per session
	private int windowSize = 50000;
	// total number of submit to send total across all sessions
	private int submitToSend = 100000;
	// total number of submit sent
	private volatile AtomicInteger submitSent = new AtomicInteger(0);

	private long startDestNumber = 9960200000l;
	private int destNumberDiff = 10000;
	private long endDestNumber = startDestNumber + destNumberDiff;

	private String sourceNumber = "6666";

	private String peerAddress = "127.0.0.1";
	private int peerPort = 2775;
	private String systemId = "test";
	private String password = "test";
	private String message = "Hello world!";

	private volatile static long expiredRequests = 0;

	public int getSessionCount() {
		return sessionCount;
	}

	public void setSessionCount(int sessionCount) {
		this.sessionCount = sessionCount;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}

	public int getSubmitToSend() {
		return submitToSend;
	}

	public void setSubmitToSend(int submitToSend) {
		this.submitToSend = submitToSend;
	}

	public long getStartDestNumber() {
		return startDestNumber;
	}

	public void setStartDestNumber(long startDestNumber) {
		this.startDestNumber = startDestNumber;
	}

	public int getDestNumberDiff() {
		return destNumberDiff;
	}

	public void setDestNumberDiff(int destNumberDiff) {
		this.destNumberDiff = destNumberDiff;
	}

	public long getEndDestNumber() {
		return endDestNumber;
	}

	public void setEndDestNumber(long endDestNumber) {
		this.endDestNumber = endDestNumber;
	}

	public String getPeerAddress() {
		return peerAddress;
	}

	public void setPeerAddress(String peerAddress) {
		this.peerAddress = peerAddress;
	}

	public int getPeerPort() {
		return peerPort;
	}

	public void setPeerPort(int peerPort) {
		this.peerPort = peerPort;
	}

	public String getSystemId() {
		return systemId;
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSourceNumber() {
		return sourceNumber;
	}

	public void setSourceNumber(String sourceNumber) {
		this.sourceNumber = sourceNumber;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	AtomicInteger getSubmitSent() {
		return submitSent;
	}

	public void start() throws Exception {

		if (startDestNumber < 1) {
			throw new Exception("Start Destination Number cannot be less than 1");
		}

		if (destNumberDiff < 1) {
			throw new Exception("Destination Number difference cannot be less than 1");
		}

		if (sessionCount < 1) {
			throw new Exception("Session count cannot be less than 1");
		}

		if (windowSize < 1) {
			throw new Exception("Windows size cannot be less than 1");
		}

		if (submitToSend < 1) {
			throw new Exception("Submit to send cannot be less than 1");
		}

		if (this.sourceNumber == null || this.sourceNumber == "") {
			throw new Exception("Source Number cannot be less than 1");
		}

		if (this.message == null) {
			throw new Exception("Message cannot be less than 1");
		}

		this.endDestNumber = startDestNumber + destNumberDiff;

		logger.warn("startDestNumber=" + startDestNumber);
		logger.warn("destNumberDiff=" + destNumberDiff);
		logger.warn("endDestNumber=" + endDestNumber);
		logger.warn("sourceNumber=" + sourceNumber);
		logger.warn("message=" + message);
		logger.warn("sessionCount=" + sessionCount);
		logger.warn("windowSize=" + windowSize);
		logger.warn("submitToSend=" + submitToSend);

		//
		// setup 3 things required for any session we plan on creating
		//

		// for monitoring thread use, it's preferable to create your own
		// instance
		// of an executor with Executors.newCachedThreadPool() and cast it to
		// ThreadPoolExecutor
		// this permits exposing thinks like executor.getActiveCount() via JMX
		// possible
		// no point renaming the threads in a factory since underlying Netty
		// framework does not easily allow you to customize your thread names
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

		// to enable automatic expiration of requests, a second scheduled
		// executor
		// is required which is what a monitor task will be executed with - this
		// is probably a thread pool that can be shared with between all client
		// bootstraps
		ScheduledThreadPoolExecutor monitorExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1,
				new ThreadFactory() {
					private AtomicInteger sequence = new AtomicInteger(0);

					@Override
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r);
						t.setName("SmppClientSessionWindowMonitorPool-" + sequence.getAndIncrement());
						return t;
					}
				});

		// a single instance of a client bootstrap can technically be shared
		// between any sessions that are created (a session can go to any
		// different
		// number of SMSCs) - each session created under
		// a client bootstrap will use the executor and monitorExecutor set
		// in its constructor - just be *very* careful with the
		// "expectedSessions"
		// value to make sure it matches the actual number of total concurrent
		// open sessions you plan on handling - the underlying netty library
		// used for NIO sockets essentially uses this value as the max number of
		// threads it will ever use, despite the "max pool size", etc. set on
		// the executor passed in here
		DefaultSmppClient clientBootstrap = new DefaultSmppClient(Executors.newCachedThreadPool(), sessionCount,
				monitorExecutor);

		// same configuration for each client runner
		SmppSessionConfiguration config = new SmppSessionConfiguration();
		config.setWindowSize(windowSize);
		config.setName("Tester.Session.0");
		config.setType(SmppBindType.TRANSCEIVER);
		config.setHost(peerAddress);
		// config.setHost("127.0.0.1");
		config.setPort(peerPort);
		config.setConnectTimeout(10000);
		config.setSystemId(systemId);
		config.setPassword(password);
		config.getLoggingOptions().setLogBytes(false);
		// to enable monitoring (request expiration)
		config.setRequestExpiryTimeout(30000);
		config.setWindowMonitorInterval(15000);
		config.setCountersEnabled(true);

		// various latches used to signal when things are ready
		CountDownLatch allSessionsBoundSignal = new CountDownLatch(sessionCount);
		CountDownLatch startSendingSignal = new CountDownLatch(1);

		// create all session runners and executors to run them
		ThreadPoolExecutor taskExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
		ClientSessionTask[] tasks = new ClientSessionTask[sessionCount];
		for (int i = 0; i < sessionCount; i++) {
			tasks[i] = new ClientSessionTask(this, allSessionsBoundSignal, startSendingSignal, clientBootstrap, config);
			taskExecutor.submit(tasks[i]);
		}

		// wait for all sessions to bind
		logger.info("Waiting up to 7 seconds for all sessions to bind...");
		if (!allSessionsBoundSignal.await(7000, TimeUnit.MILLISECONDS)) {
			throw new Exception("One or more sessions were unable to bind, cancelling test");
		}

		logger.info("Sending signal to start test...");
		long startTimeMillis = System.currentTimeMillis();
		startSendingSignal.countDown();

		// wait for all tasks to finish
		taskExecutor.shutdown();
		taskExecutor.awaitTermination(3, TimeUnit.DAYS);
		long stopTimeMillis = System.currentTimeMillis();

		// did everything succeed?
		int actualSubmitSent = 0;
		int sessionFailures = 0;
		for (int i = 0; i < sessionCount; i++) {
			if (tasks[i].getCause() != null) {
				sessionFailures++;
				logger.error("Task #" + i + " failed with exception: " + tasks[i].getCause());
			} else {
				actualSubmitSent += tasks[i].getSubmitRequestSent();
			}
		}

		logger.warn("Performance client finished:");
		logger.warn("       Sessions: " + sessionCount);
		logger.warn("    Window Size: " + windowSize);
		logger.warn("Sessions Failed: " + sessionFailures);
		logger.warn("           Time: " + (stopTimeMillis - startTimeMillis) + " ms");
		logger.warn("  Target Submit: " + submitToSend);
		logger.warn("  Actual Submit: " + actualSubmitSent);
		logger.warn("  Expired Requests: " + expiredRequests);
		
		double throughput = (double) actualSubmitSent / ((double) (stopTimeMillis - startTimeMillis) / (double) 1000);
		logger.warn("     Throughput: " + DecimalUtil.toString(throughput, 3) + " per sec");

		for (int i = 0; i < sessionCount; i++) {
			if (tasks[i].session != null && tasks[i].session.hasCounters()) {
				logger.warn(" Session " + i + ": submitSM {} " + tasks[i].session.getCounters().getTxSubmitSM());
			}
		}

		// this is required to not causing server to hang from non-daemon
		// threads
		// this also makes sure all open Channels are closed to I *think*
		logger.info("Shutting down client bootstrap and executors...");
		clientBootstrap.destroy();
		executor.shutdownNow();
		monitorExecutor.shutdownNow();

		logger.info("Done. Exiting");
	}

	public void stop() {

	}

	public static class ClientSessionTask implements Runnable {

		private SmppSession session;
		private CountDownLatch allSessionsBoundSignal;
		private CountDownLatch startSendingSignal;
		private DefaultSmppClient clientBootstrap;
		private SmppSessionConfiguration config;
		private volatile int submitRequestSent;
		private volatile int submitResponseReceived;
		private AtomicBoolean sendingDone;
		private Exception cause;
		private Random r = new Random();

		private PerformanceClientMain performanceClientMain;

		public ClientSessionTask(PerformanceClientMain performanceClientMain, CountDownLatch allSessionsBoundSignal,
				CountDownLatch startSendingSignal, DefaultSmppClient clientBootstrap, SmppSessionConfiguration config) {
			this.allSessionsBoundSignal = allSessionsBoundSignal;
			this.startSendingSignal = startSendingSignal;
			this.clientBootstrap = clientBootstrap;
			this.config = config;
			this.submitRequestSent = 0;
			this.submitResponseReceived = 0;
			this.sendingDone = new AtomicBoolean(false);
			this.performanceClientMain = performanceClientMain;
		}

		public Exception getCause() {
			return this.cause;
		}

		public int getSubmitRequestSent() {
			return this.submitRequestSent;
		}

		@Override
		public void run() {
			// a countdownlatch will be used to eventually wait for all
			// responses
			// to be received by this thread since we don't want to exit too
			// early
			CountDownLatch allSubmitResponseReceivedSignal = new CountDownLatch(1);

			SmppSessionHandler sessionHandler = new ClientSmppSessionHandler(allSubmitResponseReceivedSignal);
			String text160 = "Hello World!";
			byte[] textBytes = CharsetUtil.encode(text160, CharsetUtil.CHARSET_GSM);

			try {
				// create session a session by having the bootstrap connect a
				// socket, send the bind request, and wait for a bind response
				session = clientBootstrap.bind(config, sessionHandler);

				// don't start sending until signalled
				allSessionsBoundSignal.countDown();
				startSendingSignal.await();

				// AtomicInteger submitSent =
				// this.performanceClientMain.getSubmitSent();
				int destNumberDiff = this.performanceClientMain.getDestNumberDiff();
				long startDestNumber = this.performanceClientMain.getStartDestNumber();

				// all threads compete for processing
				while (this.performanceClientMain.getSubmitSent().getAndIncrement() < this.performanceClientMain
						.getSubmitToSend()) {
					SubmitSm submit = new SubmitSm();
					submit.setSourceAddress(new Address((byte) 0x01, (byte) 0x01, this.performanceClientMain
							.getSourceNumber()));

					long destination = r.nextInt(destNumberDiff) + startDestNumber;

					submit.setDestAddress(new Address((byte) 0x01, (byte) 0x01, Long.toString(destination)));
					submit.setShortMessage(textBytes);
					// asynchronous send
					this.submitRequestSent++;
					sendingDone.set(true);
					session.sendRequestPdu(submit, 30000, false);
				}

				// all threads have sent all submit, we do need to wait for
				// an acknowledgement for all "inflight" though (synchronize
				// against the window)
				logger.info("before waiting sendWindow.size: " + session.getSendWindow().getSize());

				allSubmitResponseReceivedSignal.await();

				logger.info("after waiting sendWindow.size: " + session.getSendWindow().getSize());

				session.unbind(5000);
			} catch (Exception e) {
				logger.error("", e);
				this.cause = e;
			}
		}

		class ClientSmppSessionHandler implements SmppSessionHandler {

			private CountDownLatch allSubmitResponseReceivedSignal;

			public ClientSmppSessionHandler(CountDownLatch allSubmitResponseReceivedSignal) {
				this.allSubmitResponseReceivedSignal = allSubmitResponseReceivedSignal;
			}

			@Override
			public void fireChannelUnexpectedlyClosed() {
				// this is an error we didn't really expect for perf testing
				// its best to at least countDown the latch so we're not waiting
				// forever
				logger.error("Unexpected close occurred...");
				this.allSubmitResponseReceivedSignal.countDown();
			}

			@Override
			public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
				submitResponseReceived++;
				// if the sending thread is finished, check if we're done
				if (sendingDone.get()) {
					if (submitResponseReceived >= submitRequestSent) {
						this.allSubmitResponseReceivedSignal.countDown();
					}
				}
			}

			@Override
			public String lookupResultMessage(int arg0) {
				return null;
			}

			@Override
			public String lookupTlvTagName(short arg0) {
				return null;
			}

			@Override
			public void firePduRequestExpired(PduRequest pduRequest) {
				if (logger.isInfoEnabled()) {
					logger.info("Default handling is to discard expired request PDU: " + pduRequest);
				}
				submitResponseReceived++;
				// if the sending thread is finished, check if we're done
				if (sendingDone.get()) {
					if (submitResponseReceived >= submitRequestSent) {
						this.allSubmitResponseReceivedSignal.countDown();
					}
				}

				expiredRequests++;
			}

			@Override
			public PduResponse firePduRequestReceived(PduRequest pduRequest) {
				logger.warn("Default handling is to discard unexpected request PDU: " + pduRequest);
				return null;
			}

			@Override
			public void fireRecoverablePduException(RecoverablePduException e) {
				logger.warn("Default handling is to discard a recoverable exception:", e);
			}

			@Override
			public void fireUnexpectedPduResponseReceived(PduResponse pduResponse) {
				logger.warn("Default handling is to discard unexpected response PDU: " + pduResponse);
			}

			@Override
			public void fireUnknownThrowable(Throwable t) {
				if (t instanceof ClosedChannelException) {
					logger.warn("Unknown throwable received, but it was a ClosedChannelException, calling fireChannelUnexpectedlyClosed instead");
					fireChannelUnexpectedlyClosed();
				} else {
					logger.warn("Default handling is to discard an unknown throwable:", t);
				}
			}

			@Override
			public void fireUnrecoverablePduException(UnrecoverablePduException pduRequest) {
				logger.warn("Default handling is to discard expired request PDU: {}", pduRequest);
			}
		}
	}

}
