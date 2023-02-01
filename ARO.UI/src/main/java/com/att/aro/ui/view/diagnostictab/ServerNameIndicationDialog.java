/*
 *  Copyright 2022 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.ui.view.diagnostictab;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.text.DefaultEditorKit;

import org.apache.commons.lang3.StringUtils;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.impl.ExternalProcessRunnerImpl;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.EnableEscKeyCloseDialog;

import lombok.Getter;


	public class ServerNameIndicationDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	private Session session;
	private JButton okButton;
	private JPanel buttonPanel;
	private JPanel jButtonGrid;
	private JPanel jContentPane;
	private JPanel serverNameIndicationPanel;
	private JTextArea serverNameIndicationTextArea;
	private JPopupMenu serverNameIndicationContextMenu;
	private EnableEscKeyCloseDialog enableEscKeyCloseDialog;
	private static final IExternalProcessRunner externalProcessRunner = ContextAware.getAROConfigContext()
			.getBean(ExternalProcessRunnerImpl.class);
	@Getter
	private Map<String, String> sniMap = null;
	private String trafficFile = "";

	public ServerNameIndicationDialog(Session session, Map<String, String> sniMap, String trafficFile) {

		this.session = session;
		this.sniMap= sniMap;
		this.trafficFile=trafficFile;
		init();
		this.setVisible(true);

	}

	private void init() {
		this.setContentPane(getJContentPane());
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setTitle("Server Name Indication Dialog");
		this.setModal(true);
		enableEscKeyCloseDialog = new EnableEscKeyCloseDialog(getRootPane(), this, false);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowDeactivated(WindowEvent event) {
				if (enableEscKeyCloseDialog.consumeEscPressed()) {
					dispose();
				}
			}
		});
		pack();
		setLocationRelativeTo(getRootPane());
		getRootPane().setDefaultButton(okButton);
	}

	private JComponent getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setPreferredSize(new Dimension(500, 250));
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getServerNameIndicatioPanel(), BorderLayout.CENTER);
			jContentPane.add(getButtonPanel(), BorderLayout.PAGE_END);
		}
		return jContentPane;
	}

	private JPanel getServerNameIndicatioPanel() {
		if (serverNameIndicationPanel == null) {
			serverNameIndicationPanel = new JPanel();
			serverNameIndicationPanel.setLayout(new BorderLayout());
			serverNameIndicationPanel.setPreferredSize(new Dimension(500, 230));

			if (serverNameIndicationTextArea == null) {
				serverNameIndicationTextArea = new JTextArea();

				serverNameIndicationContextMenu = new JPopupMenu();
				serverNameIndicationTextArea.setComponentPopupMenu(serverNameIndicationContextMenu);

				JMenuItem menuItem = new JMenuItem(new DefaultEditorKit.CopyAction());
				menuItem.setText("Copy");
				serverNameIndicationContextMenu.add(menuItem);
			}
			serverNameIndicationTextArea.setEditable(false);
			serverNameIndicationTextArea.setFocusable(true);
			serverNameIndicationTextArea.setLineWrap(true);
			serverNameIndicationTextArea.setWrapStyleWord(true);
			Border padding = BorderFactory.createBevelBorder(BevelBorder.RAISED);
			serverNameIndicationPanel.setBorder(padding);
			serverNameIndicationPanel.add(serverNameIndicationTextArea, BorderLayout.CENTER);

			String sniData = "";
			serverNameIndicationTextArea.setText("Server IP: " + session.getRemoteIP().getHostAddress() + "\n"
					+ "Server Name Indication: " + (session.getServerNameIndication() != null 
					? session.getServerNameIndication() : (StringUtils.isNotEmpty(sniData = getSNIData()) ? sniData : "N/A")) 
					+ "\n" + "Byte Count: " + session.getBytesTransferred());

		}
		return serverNameIndicationPanel;
	}

	private String getSNIData() {
		if (sniMap == null) {
			String[] tsharkCmds = Util.getParentAndCommand(Util.getTshark());

			String cmd = String.format("\"%s\" %s \"%s\"", tsharkCmds[2], " -r ",
					new File(trafficFile).toString());

			formatResult(externalProcessRunner.executeCmd(cmd
					+ " -Tfields -e frame.number -e ip.src -e udp.srcport -e tcp.srcport -e ip.dst -e udp.dstport -e tcp.dstport -e tls.handshake.extensions_server_name  -Y tls.handshake.extension.type==0",
					true, true));
		}
		String sessionKey = session.getLocalIP().getHostAddress() + " " + session.getLocalPort() + " "
				+ session.getRemotePort() + " " + session.getRemoteIP().getHostAddress();
		
		return sniMap.get(sessionKey);
	}

	private void formatResult(String result) {
		sniMap = new HashMap<String, String>();
		String[] lines = result.split("\n");
		for (int i = 0; i < lines.length; i++) {
			String[] data = (lines[i]).split("\t");
			if (data.length == 8 && StringUtils.isNotBlank(data[7]) && StringUtils.isNotBlank(data[1]) && StringUtils.isNotBlank(data[4])) {
				String sessionKey = data[1] + " " + (StringUtils.isNotBlank(data[2]) ? data[2] : data[3]) + " "
						+ (StringUtils.isNotBlank(data[5]) ? data[5] : data[6]) + " " + data[4];
				sniMap.put(sessionKey, data[7]);
			}

		}
	}

	private JPanel getButtonPanel() {
		if (buttonPanel == null) {
			buttonPanel = new JPanel();
			buttonPanel.setLayout(new BorderLayout());
			buttonPanel.add(getJButtonGrid(), BorderLayout.LINE_END);
		}
		return buttonPanel;
	}

	private JPanel getJButtonGrid() {
		if (jButtonGrid == null) {
			GridLayout gridLayout = new GridLayout();
			gridLayout.setRows(1);
			gridLayout.setHgap(10);
			jButtonGrid = new JPanel();
			jButtonGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			jButtonGrid.setLayout(gridLayout);
			jButtonGrid.add(okButton = getButton("OK", (ActionEvent arg) -> dispose()));
		}
		return jButtonGrid;
	}

	private JButton getButton(String text, ActionListener al) {
		JButton button = new JButton();
		button.setText(text);
		button.addActionListener(al);
		return button;
	}
}
