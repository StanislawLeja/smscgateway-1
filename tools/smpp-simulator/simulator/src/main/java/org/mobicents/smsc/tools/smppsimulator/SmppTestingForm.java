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
import java.awt.Color;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.JButton;

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
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
					JOptionPane.showMessageDialog(getJFrame(), "Before exiting you must Stop the testing process");
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
		tNotif.setModel(new DefaultTableModel(
			new Object[][] {
			},
			new String[] {
				"New column", "New column", "New column", "New column"
			}
		));
		tNotif.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tNotif.setFillsViewportHeight(true);
		tNotif.setBorder(new LineBorder(new Color(0, 0, 0)));
		scrollPane.setViewportView(tNotif);
		
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
		
	}

	private void start() {
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
        config0.getLoggingOptions().setLogBytes(true);
        // to enable monitoring (request expiration)
        config0.setRequestExpiryTimeout(this.param.getRequestExpiryTimeout());
        config0.setWindowMonitorInterval(this.param.getWindowMonitorInterval());
        config0.setCountersEnabled(true);

        try {
			session0 = clientBootstrap.bind(config0, sessionHandler);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        enableStart(false);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
	}

	private void stop() {
		if (session0 != null) {
			session0.unbind(5000);
			session0.destroy();
			session0 = null;
		}

		if (clientBootstrap != null) {
			clientBootstrap.destroy();
			executor.shutdownNow();
			monitorExecutor.shutdownNow();

			clientBootstrap = null;
			executor = null;
			monitorExecutor = null;
		}

		enableStart(true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	}

	private void enableStart(boolean enabled) {
		this.btStart.setEnabled(enabled);
		this.btStop.setEnabled(!enabled);
	}

	private void refreshState() {
		// .................
	}

	private void openEventWindow() {
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

	private JDialog getJFrame() {
		return this;
	}

	private void closingWindow() {
		this.mainForm.testingFormClose();
	}

	public synchronized void addMessage(String msg, String info) {
		// ............................
	}
}
