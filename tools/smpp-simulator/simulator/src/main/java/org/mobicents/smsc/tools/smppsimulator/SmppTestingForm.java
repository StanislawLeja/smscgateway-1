/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * TeleStax and individual contributors
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

package org.mobicents.smsc.tools.smppsimulator;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JScrollPane;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;

import java.awt.Color;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.JButton;

import org.mobicents.smsc.slee.resources.persistence.MessageUtil;
import org.mobicents.smsc.tools.smppsimulator.SmppSimulatorParameters.EncodingType;
import org.mobicents.smsc.tools.smppsimulator.SmppSimulatorParameters.SplittingType;

import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.type.Address;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmppTestingForm extends JDialog {

	private static final long serialVersionUID = 4969830723671541575L;

	private DefaultTableModel model = new DefaultTableModel();
	private EventForm eventForm;
	private SmppSimulatorForm mainForm;
	private SmppSimulatorParameters param;
	private JTable tNotif;
	private JButton btStart;
	private JButton btStop;
	
	private ThreadPoolExecutor executor;
	private ScheduledThreadPoolExecutor monitorExecutor;
	private DefaultSmppClient clientBootstrap;
	private SmppSession session0;

	public SmppTestingForm(JFrame owner){
		super(owner, true);
		setModal(false);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (getDefaultCloseOperation() == JDialog.DO_NOTHING_ON_CLOSE) {
					JOptionPane.showMessageDialog(getJDialog(), "Before exiting you must Stop the testing process");
				} else {
					closingWindow();
				}
			}
		});
		setBounds(100, 100, 772, 677);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(new GridLayout(0, 1, 0, 0));
		
		JPanel panel_1 = new JPanel();
		panel.add(panel_1);
		panel_1.setLayout(new GridLayout(1, 0, 0, 0));
		
		JScrollPane scrollPane = new JScrollPane((Component) null);
		panel_1.add(scrollPane);
		
		tNotif = new JTable();
		tNotif.setFillsViewportHeight(true);
		tNotif.setBorder(new LineBorder(new Color(0, 0, 0)));
		tNotif.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tNotif.setModel(new DefaultTableModel(new Object[][] {}, new String[] { "TimeStamp", "Message", "UserData" }) {
			Class[] columnTypes = new Class[] { String.class, String.class, String.class };

			public Class getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}

			boolean[] columnEditables = new boolean[] { false, false, false };

			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		});
		tNotif.getColumnModel().getColumn(0).setPreferredWidth(46);
		tNotif.getColumnModel().getColumn(1).setPreferredWidth(221);
		tNotif.getColumnModel().getColumn(2).setPreferredWidth(254);

		scrollPane.setViewportView(tNotif);

		model = (DefaultTableModel) tNotif.getModel();

		tNotif.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {

				if (e.getValueIsAdjusting())
					return;
				if (eventForm == null)
					return;

				// Номер текущей строки таблицы
				setEventMsg();
			}
		});

		JPanel panel_2 = new JPanel();
		panel.add(panel_2);
		panel_2.setLayout(null);
		
		btStart = new JButton("Start");
		btStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				start();
			}
		});
		btStart.setBounds(10, 11, 90, 23);
		panel_2.add(btStart);
		
		btStop = new JButton("Stop");
		btStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stop();
			}
		});
		btStop.setEnabled(false);
		btStop.setBounds(104, 11, 90, 23);
		panel_2.add(btStop);
		
		JButton btRefreshState = new JButton("Refresh state");
		btRefreshState.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refreshState();
			}
		});
		btRefreshState.setBounds(204, 11, 148, 23);
		panel_2.add(btRefreshState);
		
		JButton btOpeEventWindow = new JButton("Open event window");
		btOpeEventWindow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openEventWindow();
			}
		});
		btOpeEventWindow.setBounds(357, 11, 159, 23);
		panel_2.add(btOpeEventWindow);
		
		JButton btConfigSubmitData = new JButton("Configure data for a message submitting");
		btConfigSubmitData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SmppMessageParamForm frame = new SmppMessageParamForm(getJDialog());
				frame.setData(param);
				frame.setVisible(true);

				SmppSimulatorParameters newPar = frame.getData();
				if (newPar != null) {
					param = newPar;

					try {
						BufferedOutputStream bis = new BufferedOutputStream(new FileOutputStream("SmppSimulatorParameters.xml"));
						XMLEncoder d = new XMLEncoder(bis);
						d.writeObject(newPar);
						d.close();
					} catch (Exception ee) {
						ee.printStackTrace();
						JOptionPane.showMessageDialog(null, "Failed when saving the parameter file SmppSimulatorParameters.xml: " + ee.getMessage());
					}
				}
			}
		});
		btConfigSubmitData.setBounds(11, 46, 341, 23);
		panel_2.add(btConfigSubmitData);
		
		JButton btSendMessage = new JButton("Submit a message");
		btSendMessage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				submitMessage();
			}
		});
		btSendMessage.setBounds(11, 80, 341, 23);
		panel_2.add(btSendMessage);
		
	}

	private int msgRef = 0;

	public SmppSimulatorParameters getSmppSimulatorParameters() {
		return this.param;
	}

	private int getNextMsgRef() {
		msgRef++;
		if (msgRef > 255)
			msgRef = 1;
		return msgRef;
	}

	private void submitMessage() {
        try {
        	int dcs = 0;
			ArrayList<byte[]> msgLst = new ArrayList<byte[]>();
        	int msgRef = 0;

            EncodingType et = param.getEncodingType();
            byte[] buf = null;
            int maxLen = 0;
            int maxSplLen = 0;
            boolean addSegmTlv = false;
            int esmClass = 0;
    		switch (et) {
    		case GSM7:
    			dcs = 0;
    			buf = this.param.getMessageText().getBytes();
                maxLen = 160;
                maxSplLen = 153;
    			break;
    		case UCS2:
    			dcs = 8;
    			Charset ucs2Charset = Charset.forName("UTF-16BE");
				ByteBuffer bb = ucs2Charset.encode(this.param.getMessageText());
				buf = new byte[bb.limit()];
				bb.get(buf);
    			maxLen = 140;
                maxSplLen = 134;
                break;
    		}

			if (buf.length > maxLen) { // may be message splitting
				SplittingType st = param.getSplittingType();
				switch (st) {
				case DoNotSplit:
					// we do not split
					msgLst.add(buf);
					break;
				case SplitWithParameters:
					msgRef = getNextMsgRef();
					ArrayList<byte[]> r1 = this.splitByteArr(buf, maxSplLen);
					for (byte[] bf : r1) {
						msgLst.add(bf);
					}
					addSegmTlv = true;
					break;
				case SplitWithUdh:
					msgRef = getNextMsgRef();
					r1 = this.splitByteArr(buf, maxSplLen);
					byte[] bf1 = new byte[6];
					bf1[0] = 5; // total UDH length
					bf1[1] = 0; // UDH id
					bf1[2] = 3; // UDH length
					bf1[3] = (byte) msgRef; // refNum
					bf1[4] = (byte) r1.size(); // segmCnt
					int i1 = 0;
					for (byte[] bf : r1) {
						i1++;
						bf1[5] = (byte) i1; // segmNum
						byte[] bf2 = new byte[bf1.length + bf.length];
						System.arraycopy(bf1, 0, bf2, 0, bf1.length);
						System.arraycopy(bf, 0, bf2, bf1.length, bf.length);
						msgLst.add(bf2);
					}
					esmClass = 0x40;
					break;
				}
			} else {
				msgLst.add(buf);
			}

        	this.doSubmitMessage(dcs, msgLst, msgRef, addSegmTlv, esmClass, param.getValidityType());
		} catch (Exception e) {
			this.addMessage("Failure to submit message", e.toString());
			return;
		}
	}

	private ArrayList<byte[]> splitByteArr(byte[] buf, int maxLen) {
		ArrayList<byte[]> res = new ArrayList<byte[]>();

		byte[] prevBuf = buf;

		while (true) {
			if (prevBuf.length <= maxLen) {
				res.add(prevBuf);
				break;
			}

			byte[] segm = new byte[maxLen];
			byte[] newBuf = new byte[prevBuf.length - maxLen];

			System.arraycopy(prevBuf, 0, segm, 0, maxLen);
			System.arraycopy(prevBuf, maxLen, newBuf, 0, prevBuf.length - maxLen);
			
			res.add(segm);
			prevBuf = newBuf;
		}

		return res;
	}

	private void doSubmitMessage(int dcs, ArrayList<byte[]> msgLst, int msgRef, boolean addSegmTlv, int esmClass,
			SmppSimulatorParameters.ValidityType validityType) throws Exception {
		int i1 = 0;
		for (byte[] buf : msgLst) {
			i1++;

			SubmitSm pdu = new SubmitSm();

	        pdu.setSourceAddress(new Address((byte)this.param.getSourceTON().getCode(), (byte)this.param.getSourceNPI().getCode(), this.param.getSourceAddress()));
	        pdu.setDestAddress(new Address((byte)this.param.getDestTON().getCode(), (byte)this.param.getDestNPI().getCode(), this.param.getDestAddress()));
	        pdu.setEsmClass((byte) esmClass);

			switch (validityType) {
			case ValidityPeriod_5min:
				pdu.setValidityPeriod(MessageUtil.printSmppRelativeDate(0, 0, 0, 0, 5, 0));
				break;
			case ScheduleDeliveryTime_5min:
				pdu.setScheduleDeliveryTime(MessageUtil.printSmppRelativeDate(0, 0, 0, 0, 5, 0));
				break;
			}

			pdu.setDataCoding((byte) dcs);

			if (buf.length < 250)
				pdu.setShortMessage(buf);
			else {
				Tlv tlv = new Tlv(SmppConstants.TAG_MESSAGE_PAYLOAD, buf);
				pdu.addOptionalParameter(tlv);
			}

			if (addSegmTlv) {
				byte[] buf1 = new byte[2];
				buf1[0] = 0;
				buf1[1] = (byte)msgRef;
				Tlv tlv = new Tlv(SmppConstants.TAG_SAR_MSG_REF_NUM, buf1);
				pdu.addOptionalParameter(tlv);
				buf1 = new byte[1];
				buf1[0] = (byte) msgLst.size();
				tlv = new Tlv(SmppConstants.TAG_SAR_TOTAL_SEGMENTS, buf1);
				pdu.addOptionalParameter(tlv);
				buf1 = new byte[1];
				buf1[0] = (byte)i1;
				tlv = new Tlv(SmppConstants.TAG_SAR_SEGMENT_SEQNUM, buf1);
				pdu.addOptionalParameter(tlv);
			}

	        WindowFuture<Integer,PduRequest,PduResponse> future0 = session0.sendRequestPdu(pdu, 10000, false);

	        this.addMessage("Request=" + pdu.getName(), pdu.toString());
		}
	}
	
	private void setEventMsg() {
		ListSelectionModel l = tNotif.getSelectionModel();
		if (!l.isSelectionEmpty()) {
			int index = l.getMinSelectionIndex();
			String s1 = (String) model.getValueAt(index, 0);
			String s2 = (String) model.getValueAt(index, 1);
			String s3 = (String) model.getValueAt(index, 2);
			eventForm.setData(s1, s2, s3);
		}
	}

	private void start() {
		this.addMessage("Trying to start a new session", "");

		this.executor = (ThreadPoolExecutor)Executors.newCachedThreadPool();
        this.monitorExecutor = (ScheduledThreadPoolExecutor)Executors.newScheduledThreadPool(1, new ThreadFactory() {
            private AtomicInteger sequence = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("SmppClientSessionWindowMonitorPool-" + sequence.getAndIncrement());
                return t;
            }
        });
        clientBootstrap = new DefaultSmppClient(Executors.newCachedThreadPool(), 1, monitorExecutor);

        DefaultSmppSessionHandler sessionHandler = new ClientSmppSessionHandler(this);

        SmppSessionConfiguration config0 = new SmppSessionConfiguration();
        config0.setWindowSize(this.param.getWindowSize());
        config0.setName("Tester.Session.0");
        config0.setType(this.param.getBindType());
        config0.setHost(this.param.getHost());
        config0.setPort(this.param.getPort());
        config0.setConnectTimeout(this.param.getConnectTimeout());
        config0.setSystemId(this.param.getSystemId());
        config0.setPassword(this.param.getPassword());
		config0.setAddressRange(new Address((byte) 1, (byte) 1, "6666"));
        config0.getLoggingOptions().setLogBytes(true);
        // to enable monitoring (request expiration)
        config0.setRequestExpiryTimeout(this.param.getRequestExpiryTimeout());
        config0.setWindowMonitorInterval(this.param.getWindowMonitorInterval());
        config0.setCountersEnabled(true);

        try {
			session0 = clientBootstrap.bind(config0, sessionHandler);
		} catch (Exception e) {
			this.addMessage("Failure to start a new session", e.toString());
			return;
		}

        enableStart(false);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

		this.addMessage("Session has been successfully started", "");
	}

	public void stop() {
		this.addMessage("Trying to stop a session", "");

		this.doStop();
	}
	
	public void doStop() {
		if (session0 != null) {
			session0.unbind(5000);
			session0.destroy();
			session0 = null;
		}

		if (clientBootstrap != null) {
			try {
				clientBootstrap.destroy();
				executor.shutdownNow();
				monitorExecutor.shutdownNow();
			} catch (Exception e) {

			}

			clientBootstrap = null;
			executor = null;
			monitorExecutor = null;
		}

		enableStart(true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		this.addMessage("Session has been stopped", "");
	}

	private void enableStart(boolean enabled) {
		this.btStart.setEnabled(enabled);
		this.btStop.setEnabled(!enabled);
	}

	private void refreshState() {
		// .................
	}

	public void setData(SmppSimulatorForm mainForm, SmppSimulatorParameters param) {
		this.param = param;
		this.mainForm = mainForm;

//		this.tm = new javax.swing.Timer(5000, new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				refreshState();
//			}
//		});
//		this.tm.start();
	}

	private JDialog getJDialog() {
		return this;
	}

	private void closingWindow() {
		this.mainForm.testingFormClose();
	}

	public void eventFormClose() {
		this.eventForm = null;
	}
	
	private void openEventWindow() {
		if (this.eventForm != null)
			return;

		this.eventForm = new EventForm(this);
		this.eventForm.setVisible(true);
		setEventMsg();
	}

	public synchronized void addMessage(String msg, String info) {
		
		Date d1 = new Date();
		String s1 = d1.toLocaleString();

		Vector newRow = new Vector();
		newRow.add(s1);
		newRow.add(msg);
		newRow.add(info);
		model.getDataVector().add(0,newRow);

		model.newRowsAdded(new TableModelEvent(model));
	}
}
