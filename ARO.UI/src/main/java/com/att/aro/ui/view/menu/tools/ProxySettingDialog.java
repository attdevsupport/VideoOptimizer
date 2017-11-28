/**
 * 
 */
package com.att.aro.ui.view.menu.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.amazonaws.ClientConfiguration;
import com.att.aro.ui.utils.ResourceBundleHelper;

/**
 * Setting up proxy information for internal network
 */
public class ProxySettingDialog extends JDialog {
	
	private static final long serialVersionUID = 1L;

	static final Dimension SCREEN_DIMENSION = Toolkit.getDefaultToolkit().getScreenSize();
	
	//I'd also make this static and final and insert them at the class definition
	int dialogWidth = SCREEN_DIMENSION.width / 4; //example; a quarter of the screen size
	int dialogHeight = SCREEN_DIMENSION.height / 4; //example
	
	int dialogX = SCREEN_DIMENSION.width / 2 - dialogWidth / 2; //position right in the middle of the screen
	int dialogY = SCREEN_DIMENSION.height / 2 - dialogHeight / 2;

	
	private JPanel panel;
	private JLabel ProxyHost;
	private JLabel ProxyPort;
	private JLabel ProxyUserName;
	private JLabel ProxyPassword;
	
	private JTextField txpHost;
	private JTextField txpPort;
	private JTextField txpUserName;
	private JPasswordField txpPwd;	
	private JButton saveButton;
	private JButton cancelButton;
	public AWSDialog parent;
	
	private ClientConfiguration clientConfiguration = new ClientConfiguration();
	
	public ProxySettingDialog(AWSDialog parent) {
		super(parent);
		this.parent = parent;
		setLayout(new BorderLayout());
		panel = new JPanel(new GridBagLayout());
 		initializeDialog();
		this.setPreferredSize(new Dimension(500, 300));
		this.setResizable(true);
		setContentPane(panel);
		setBounds(dialogX, dialogY, dialogWidth, dialogHeight);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		
	}

	/**
	 * 
	 */
	private void initializeDialog() {
		
  		ProxyHost = new JLabel(ResourceBundleHelper.getMessageString("aws.proxy.hostname"));
		ProxyPort = new JLabel(ResourceBundleHelper.getMessageString("aws.proxy.hostport"));
		ProxyUserName = new JLabel(ResourceBundleHelper.getMessageString("aws.proxy.userid"));
		ProxyPassword = new JLabel(ResourceBundleHelper.getMessageString("aws.proxy.password"));
		
		txpHost = new JTextField();
		txpHost.setToolTipText(ResourceBundleHelper.getMessageString("aws.proxy.hostname.ex"));
		txpPort = new JTextField();
		txpPort.setToolTipText(ResourceBundleHelper.getMessageString("aws.proxy.hostport.ex"));
		txpUserName = new JTextField();
		txpUserName.setToolTipText(ResourceBundleHelper.getMessageString("aws.proxy.userid.ex"));
		txpPwd = new JPasswordField();
		txpPwd.setToolTipText(ResourceBundleHelper.getMessageString("aws.proxy.password.ex"));		
 		
		cancelButton = new JButton(ResourceBundleHelper.getMessageString("chart.options.dialog.button.cancel"));
		cancelButton.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();			
			}
		});
		
		saveButton = new JButton(ResourceBundleHelper.getMessageString("fileChooser.Save"));
		saveButton.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				setClientConfiguration(clientConfiguration);
				setVisible(false);
				dispose();
		        
			}
		});

		
		panel.add(ProxyHost, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 1, 0));
		panel.add(ProxyPort, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 1, 0));
		panel.add(ProxyUserName, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 1, 0));
		panel.add(ProxyPassword, new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 1, 0));
 		
		panel.add(txpHost, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		panel.add(txpPort, new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		panel.add(txpUserName, new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		panel.add(txpPwd, new GridBagConstraints(1, 3, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
 		
		panel.add(cancelButton, new GridBagConstraints(0, 4, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		panel.add(saveButton, new GridBagConstraints(1, 4, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

	}
	
 	
	public void setClientConfiguration(ClientConfiguration config) {
		
		config.setProxyHost(getTxpHost().getText());
		config.setProxyPort(Integer.parseInt(getTxpPort().getText()));
		config.setProxyUsername(getTxpUserName().getText());
		config.setProxyPassword(Arrays.toString(getTxpPwd().getPassword()));
		
		parent.setProxySetting(config);
	}
	
	public ClientConfiguration getClientConfiguration(){	 
		return clientConfiguration;
	}
	private JTextField getTxpHost() {
		return txpHost;
	}

	private JTextField getTxpPort() {
		return txpPort;
	}

	private JTextField getTxpUserName() {
		return txpUserName;
	}

	private JPasswordField getTxpPwd() {
		return txpPwd;
	}
	
}
