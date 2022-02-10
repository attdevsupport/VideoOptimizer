package com.att.aro.ui.view.diagnostictab;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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

import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.ui.commonui.EnableEscKeyCloseDialog;


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
	
	public ServerNameIndicationDialog(Session session) {
		
		this.session = session;
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
			jContentPane.setPreferredSize(new Dimension(500,250));
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
			
			serverNameIndicationTextArea.setText("Server IP: " + session.getRemoteIP().getHostAddress() + 
					"\n" + "Server Name Indication: " + (session.getServerNameIndication() != null ? session.getServerNameIndication() : "N/A") +
					"\n" + "Byte Count: " + session.getBytesTransferred());
			
		}
		return serverNameIndicationPanel;
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
