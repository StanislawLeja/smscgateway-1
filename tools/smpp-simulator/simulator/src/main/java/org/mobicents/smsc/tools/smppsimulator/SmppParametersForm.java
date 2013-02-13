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

package org.mobicents.smsc.tools.smppsimulator;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JComboBox;

import com.cloudhopper.smpp.SmppBindType;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmppParametersForm extends JDialog {

	private static final long serialVersionUID = -8945615083883278369L;

	SmppSimulatorParameters data;
	private JTextField tbWindowSize;
	private JComboBox<SmppBindType> cbBindType;
	private JTextField tbHost;
	private JTextField tbPort;

	public SmppParametersForm(JFrame owner) {
		super(owner, true);
		setTitle("SMPP parameters");
		setResizable(false);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 620, 286);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(null);
		
		JLabel lblSmppWindowSize = new JLabel("<html>SMPP window size. The maximum number of requests \r\n<br>permitted to be outstanding (unacknowledged) at a given time\r\n</html>");
		lblSmppWindowSize.setBounds(10, 11, 401, 33);
		panel.add(lblSmppWindowSize);
		
		tbWindowSize = new JTextField();
		tbWindowSize.setBounds(424, 10, 86, 20);
		panel.add(tbWindowSize);
		tbWindowSize.setColumns(10);
		
		JButton btCancel = new JButton("Cancel");
		btCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doCancel();
			}
		});
		btCancel.setBounds(468, 224, 136, 23);
		panel.add(btCancel);
		
		JButton btOK = new JButton("OK");
		btOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doOK();
			}
		});
		btOK.setBounds(327, 224, 136, 23);
		panel.add(btOK);
		
		JLabel lblSmppBindType = new JLabel("SMPP bind type");
		lblSmppBindType.setBounds(10, 50, 401, 14);
		panel.add(lblSmppBindType);
		
		cbBindType = new JComboBox();
		cbBindType.setBounds(424, 47, 180, 20);
		panel.add(cbBindType);
		
		JLabel lblSmscHost = new JLabel("SMSC host");
		lblSmscHost.setBounds(10, 81, 401, 14);
		panel.add(lblSmscHost);
		
		JLabel lblSmscPort = new JLabel("SMSC port");
		lblSmscPort.setBounds(10, 112, 401, 14);
		panel.add(lblSmscPort);
		
		tbHost = new JTextField();
		tbHost.setColumns(10);
		tbHost.setBounds(424, 78, 180, 20);
		panel.add(tbHost);
		
		tbPort = new JTextField();
		tbPort.setColumns(10);
		tbPort.setBounds(424, 109, 86, 20);
		panel.add(tbPort);

	}

	public void setData(SmppSimulatorParameters data) {
		this.tbWindowSize.setText(((Integer) data.getWindowSize()).toString());
		this.tbHost.setText(data.getHost());
		this.tbPort.setText(((Integer) data.getPort()).toString());

		this.cbBindType.removeAllItems();
		SmppBindType[] vall = SmppBindType.values();
		SmppBindType dv = null;
		for (SmppBindType v : vall) {
			this.cbBindType.addItem(v);
			if (v == data.getBindType())
				dv = v;
		}
		if (dv != null)
			this.cbBindType.setSelectedItem(dv);

		// ...........................
	}

	public SmppSimulatorParameters getData() {
		return this.data;
	}

	private void doOK() {
		this.data = new SmppSimulatorParameters();

		this.data.setHost(this.tbHost.getText());

		int intVal = 0;
		try {
			intVal = Integer.parseInt(this.tbWindowSize.getText());
			this.data.setWindowSize(intVal);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Exception when parsing WindowSize value: " + e.toString());
			return;
		}
		try {
			intVal = Integer.parseInt(this.tbPort.getText());
			this.data.setPort(intVal);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Exception when parsing Port value: " + e.toString());
			return;
		}

		this.data.setBindType((SmppBindType) cbBindType.getSelectedItem());

		// ............................

		this.dispose();
	}

	private void doCancel() {
		this.dispose();
	}
}
