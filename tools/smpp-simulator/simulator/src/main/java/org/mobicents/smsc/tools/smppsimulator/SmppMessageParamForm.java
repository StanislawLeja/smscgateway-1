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

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmppMessageParamForm extends JDialog {

	private static final long serialVersionUID = -7694148495845105185L;

	private SmppSimulatorParameters data;

	private JTextArea tbMessage;
	private JComboBox<SmppSimulatorParameters.EncodingType> cbEncodingType;
	private JComboBox<SmppSimulatorParameters.SplittingType> cbSplittingType;
	private JComboBox<SmppSimulatorParameters.TON> cbSrcTON;
	private JComboBox<SmppSimulatorParameters.NPI> cbSrcNPI;
	private JTextField tbSourceAddress;
	private JTextField tbDestAddress;
	private JComboBox<SmppSimulatorParameters.TON> cbDestTON;
	private JComboBox<SmppSimulatorParameters.NPI> cbDestNPI;
	private JComboBox<SmppSimulatorParameters.ValidityType> cbValidityType;

	public SmppMessageParamForm(JDialog owner) {
		super(owner, true);

		setTitle("SMPP message parameters");
		setResizable(false);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 620, 487);

		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(null);
		
		JLabel lblTextEncodingType = new JLabel("Text encoding type");
		lblTextEncodingType.setBounds(10, 109, 329, 14);
		panel.add(lblTextEncodingType);
		
		cbEncodingType = new JComboBox<SmppSimulatorParameters.EncodingType>();
		cbEncodingType.setBounds(349, 106, 255, 20);
		panel.add(cbEncodingType);
		
		JLabel lblMessageText = new JLabel("Message text");
		lblMessageText.setBounds(10, 14, 401, 14);
		panel.add(lblMessageText);
		
		JButton button = new JButton("OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				doOK();
			}
		});
		button.setBounds(325, 418, 136, 23);
		panel.add(button);
		
		JButton button_1 = new JButton("Cancel");
		button_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doCancel();
			}
		});
		button_1.setBounds(466, 418, 136, 23);
		panel.add(button_1);

		cbSplittingType = new JComboBox<SmppSimulatorParameters.SplittingType>();
		cbSplittingType.setBounds(349, 134, 255, 20);
		panel.add(cbSplittingType);
		
		JLabel lblMessageSplittingType = new JLabel("Message splitting type");
		lblMessageSplittingType.setBounds(10, 137, 329, 14);
		panel.add(lblMessageSplittingType);
				
				tbMessage = new JTextArea();
				tbMessage.setBounds(10, 39, 594, 56);
				//		panel.add(tbMessage);
						
						JScrollPane scrollPane = new JScrollPane(tbMessage);
						scrollPane.setBounds(0, 40, 604, 58);
						panel.add(scrollPane);
						
						JLabel lblTypeOfNumber = new JLabel("Source address: Type of number");
						lblTypeOfNumber.setBounds(10, 168, 329, 14);
						panel.add(lblTypeOfNumber);
						
						cbSrcTON = new JComboBox<SmppSimulatorParameters.TON>();
						cbSrcTON.setBounds(349, 165, 255, 20);
						panel.add(cbSrcTON);
						
						JLabel lblNumberingPlanIndicator = new JLabel("Source address: Numbering plan indicator");
						lblNumberingPlanIndicator.setBounds(10, 196, 329, 14);
						panel.add(lblNumberingPlanIndicator);
						
						cbSrcNPI = new JComboBox<SmppSimulatorParameters.NPI>();
						cbSrcNPI.setBounds(349, 193, 255, 20);
						panel.add(cbSrcNPI);
						
						tbSourceAddress = new JTextField();
						tbSourceAddress.setBounds(349, 281, 255, 20);
						panel.add(tbSourceAddress);
						tbSourceAddress.setColumns(10);
						
						JLabel lblSourceAddress = new JLabel("Source address");
						lblSourceAddress.setBounds(10, 284, 329, 14);
						panel.add(lblSourceAddress);
						
						tbDestAddress = new JTextField();
						tbDestAddress.setColumns(10);
						tbDestAddress.setBounds(349, 311, 255, 20);
						panel.add(tbDestAddress);
						
						JLabel lblDestinationAddress = new JLabel("Destination address");
						lblDestinationAddress.setBounds(10, 314, 329, 14);
						panel.add(lblDestinationAddress);
						
						JLabel lblDestinationAddressType = new JLabel("Destination address: Type of number");
						lblDestinationAddressType.setBounds(10, 226, 329, 14);
						panel.add(lblDestinationAddressType);
						
						JLabel lblDestinationAddressNumbering = new JLabel("Destination address: Numbering plan indicator");
						lblDestinationAddressNumbering.setBounds(10, 254, 329, 14);
						panel.add(lblDestinationAddressNumbering);
						
						cbDestTON = new JComboBox<SmppSimulatorParameters.TON>();
						cbDestTON.setBounds(349, 223, 255, 20);
						panel.add(cbDestTON);
						
						cbDestNPI = new JComboBox<SmppSimulatorParameters.NPI>();
						cbDestNPI.setBounds(349, 251, 255, 20);
						panel.add(cbDestNPI);
						
						JLabel lblValidityPeriod = new JLabel("Validity period / schedule delivery time");
						lblValidityPeriod.setBounds(10, 346, 329, 14);
						panel.add(lblValidityPeriod);

						cbValidityType = new JComboBox<SmppSimulatorParameters.ValidityType>();
						cbValidityType.setBounds(349, 343, 255, 20);
						panel.add(cbValidityType);
	}

	public void setData(SmppSimulatorParameters data) {
		this.data = data;

		this.tbMessage.setText(data.getMessageText());
		this.tbSourceAddress.setText(data.getSourceAddress());
		this.tbDestAddress.setText(data.getDestAddress());

		this.cbEncodingType.removeAllItems();
		SmppSimulatorParameters.EncodingType[] vallET = SmppSimulatorParameters.EncodingType.values();
		SmppSimulatorParameters.EncodingType dv = null;
		for (SmppSimulatorParameters.EncodingType v : vallET) {
			this.cbEncodingType.addItem(v);
			if (v == data.getEncodingType())
				dv = v;
		}
		if (dv != null)
			this.cbEncodingType.setSelectedItem(dv);

		this.cbSplittingType.removeAllItems();
		SmppSimulatorParameters.SplittingType[] vallST = SmppSimulatorParameters.SplittingType.values();
		SmppSimulatorParameters.SplittingType dvST = null;
		for (SmppSimulatorParameters.SplittingType v : vallST) {
			this.cbSplittingType.addItem(v);
			if (v == data.getSplittingType())
				dvST = v;
		}
		if (dvST != null)
			this.cbSplittingType.setSelectedItem(dvST);

		this.cbSrcTON.removeAllItems();
		SmppSimulatorParameters.TON[] vallTON = SmppSimulatorParameters.TON.values();
		SmppSimulatorParameters.TON dvTON = null;
		for (SmppSimulatorParameters.TON v : vallTON) {
			this.cbSrcTON.addItem(v);
			if (v == data.getSourceTON())
				dvTON = v;
		}
		if (dvTON != null)
			this.cbSrcTON.setSelectedItem(dvTON);

		this.cbDestTON.removeAllItems();
		vallTON = SmppSimulatorParameters.TON.values();
		dvTON = null;
		for (SmppSimulatorParameters.TON v : vallTON) {
			this.cbDestTON.addItem(v);
			if (v == data.getDestTON())
				dvTON = v;
		}
		if (dvTON != null)
			this.cbDestTON.setSelectedItem(dvTON);

		this.cbSrcNPI.removeAllItems();
		SmppSimulatorParameters.NPI[] vallNPI = SmppSimulatorParameters.NPI.values();
		SmppSimulatorParameters.NPI dvNPI = null;
		for (SmppSimulatorParameters.NPI v : vallNPI) {
			this.cbSrcNPI.addItem(v);
			if (v == data.getSourceNPI())
				dvNPI = v;
		}
		if (dvNPI != null)
			this.cbSrcNPI.setSelectedItem(dvNPI);

		this.cbDestNPI.removeAllItems();
		vallNPI = SmppSimulatorParameters.NPI.values();
		dvNPI = null;
		for (SmppSimulatorParameters.NPI v : vallNPI) {
			this.cbDestNPI.addItem(v);
			if (v == data.getDestNPI())
				dvNPI = v;
		}
		if (dvNPI != null)
			this.cbDestNPI.setSelectedItem(dvNPI);

		this.cbValidityType.removeAllItems();
		SmppSimulatorParameters.ValidityType[] vallValType = SmppSimulatorParameters.ValidityType.values();
		SmppSimulatorParameters.ValidityType dvValType = null;
		for (SmppSimulatorParameters.ValidityType v : vallValType) {
			this.cbValidityType.addItem(v);
			if (v == data.getValidityType())
				dvValType = v;
		}
		if (dvValType != null)
			this.cbValidityType.setSelectedItem(dvValType);
	}

	public SmppSimulatorParameters getData() {
		return this.data;
	}

	private void doOK() {
//		this.data = new SmppSimulatorParameters();

		this.data.setMessageText(this.tbMessage.getText());
		this.data.setSourceAddress(this.tbSourceAddress.getText());
		this.data.setDestAddress(this.tbDestAddress.getText());

		this.data.setEncodingType((SmppSimulatorParameters.EncodingType) cbEncodingType.getSelectedItem());
		this.data.setSplittingType((SmppSimulatorParameters.SplittingType) cbSplittingType.getSelectedItem());
		this.data.setSourceTON((SmppSimulatorParameters.TON) cbSrcTON.getSelectedItem());
		this.data.setSourceNPI((SmppSimulatorParameters.NPI) cbSrcNPI.getSelectedItem());
		this.data.setDestTON((SmppSimulatorParameters.TON) cbDestTON.getSelectedItem());
		this.data.setDestNPI((SmppSimulatorParameters.NPI) cbDestNPI.getSelectedItem());
		this.data.setValidityType((SmppSimulatorParameters.ValidityType) cbValidityType.getSelectedItem());

		this.dispose();
	}

	private void doCancel() {
		this.data = null;
		this.dispose();
	}
}

