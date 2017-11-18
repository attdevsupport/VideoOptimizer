package com.att.aro.ui.view.menu.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;

import org.apache.log4j.Logger;
import org.jfree.util.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.att.aro.core.cloud.State;
import com.att.aro.core.cloud.TraceManager;
import com.att.aro.core.cloud.aws.AWSInfoCredentials;
import com.att.aro.core.cloud.aws.AwsRepository;
import com.att.aro.core.preferences.UserPreferences;
import com.att.aro.core.preferences.UserPreferencesFactory;
import com.att.aro.mvc.IAROView;
import com.att.aro.ui.commonui.AROProgressDialog;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.menu.datacollector.DeviceTablePanel;

public class AWSDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private static ResourceBundle resourceBundle = ResourceBundleHelper.getDefaultBundle();
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(DeviceTablePanel.class.getName());

	private enum DialogItem {
		upload_trace_dialog_button_ok, upload_trace_dialog_button_cancel, upload_trace_dialog_button_legend,
	}
	private AWS mode;

	public enum AWS {
		UPLOAD, DOWNLOAD
	}

	private ProxySettingDialog proxySettingDialog; 
	private JPanel jDialogPanel;
	private JPanel uploadPanel;
	private JPanel uploadGrid;
	private JPanel ctrlPanel;
	private JPanel buttonGrid;
	private JButton okButton;
	private JButton loadRemoteButton;
	private JButton cancelButton;
	private JButton proxyButton;

	private JTextField accessIdText = createTextFieldAndProperties("AccessID");
	private JTextField secretText = createTextFieldAndProperties("Secret");
 	private JTextField regionText = createTextFieldAndProperties("Region");
	private JTextField bucketNameText = createTextFieldAndProperties("Bucket Name");
	@SuppressWarnings("unused")
	private IAROView parent;
	private JList<String> list;
	private DefaultListModel<String> model;

	private HashMap<AWSInfoCredentials, String> awsInfo = new HashMap<AWSInfoCredentials, String>();
	private ClientConfiguration config = new ClientConfiguration();
 	private String errorMessage = "AUTHENTICATION";


	public AWSDialog(IAROView parent, AWS awsMode) {
		this.parent = parent;
		this.mode = awsMode;
		this.setSize(550, 370);
		this.setResizable(false);
		this.setModal(true);
		if (awsMode == AWS.UPLOAD) {
			this.setTitle(resourceBundle.getString("upload.trace.dialog.title"));
		} else {
			this.setTitle(resourceBundle.getString("download.trace.dialog.title"));
		}
		this.setLocationRelativeTo(getOwner());
		this.setContentPane(getDialogPanel());
	}

	public void setAWSMode(AWS awsMode) {
		this.mode = awsMode;
		if (awsMode == AWS.UPLOAD) {
			this.setTitle(resourceBundle.getString("upload.trace.dialog.title"));
			list.clearSelection();
		} else {
			this.setTitle(resourceBundle.getString("download.trace.dialog.title"));
			list.setSelectedIndex(0);
		}
	}

	private JPanel getDialogPanel() {
		if (jDialogPanel == null) {
			jDialogPanel = new JPanel();
			jDialogPanel.setLayout(new BorderLayout());
			jDialogPanel.add(getUploadTracePanel(), BorderLayout.CENTER);
			jDialogPanel.add(getCtrlPanel(), BorderLayout.SOUTH);
		}
		return jDialogPanel;
	}

	private JPanel getUploadTracePanel() {
		if (uploadPanel == null) {
			uploadPanel = new JPanel();
			uploadPanel.setLayout(new BorderLayout());
			uploadPanel.add(getUploadTraceGrid(), BorderLayout.CENTER);
		}
		return uploadPanel;
	}

	private JPanel getUploadTraceGrid() {
		if (uploadGrid == null) {
			GridLayout gridLayout = new GridLayout();
			gridLayout.setRows(7);
			gridLayout.setVgap(5);
			uploadGrid = new JPanel();
			uploadGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			uploadGrid.setLayout(gridLayout);
			uploadGrid.add(accessIdText);
			uploadGrid.add(secretText);
			uploadGrid.add(regionText);
			uploadGrid.add(bucketNameText);
			uploadGrid.add(getProxySettingButton());
			uploadGrid.add(getRemoteListButton());
			uploadGrid.add(getRemoteListPanel());
		}
		return uploadGrid;
	}

	private JButton getProxySettingButton(){
		if(proxyButton ==null){
			proxyButton = new JButton();
			proxyButton.setText(resourceBundle.getString(("aws.proxy.setting")));			
			proxyButton.addActionListener(new ActionListener(){

				@Override
				public void actionPerformed(ActionEvent e) {
					runProxyDialog();					
				}			
			});
			
		}
		return proxyButton;
	}
	
	private void runProxyDialog(){
	
		proxySettingDialog = new ProxySettingDialog(this);
		proxySettingDialog.setVisible(true);
		
	}
	
	public void  setProxySetting(ClientConfiguration config){
		this.config = config;
	}
	
	public ClientConfiguration getProxySetting(){		 
		Log.info(config.getProxyHost());
		return config;
	}
	
	private JButton getRemoteListButton() {
		if (loadRemoteButton == null) {
			loadRemoteButton = new JButton();
			loadRemoteButton.setText(ResourceBundleHelper.getMessageString(DialogItem.upload_trace_dialog_button_ok));
			loadRemoteButton.setText("Get Traces");
			loadRemoteButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setCredentials();
					AwsRepository awsRepo = new AwsRepository(awsInfo,getProxySetting());
					if (true == awsRepo.isAuthenticated()) {
						List<S3ObjectSummary> listRemote = awsRepo.getlist();
						if (listRemote != null) {
							for (S3ObjectSummary o : listRemote) {
								if (o.getKey().endsWith(".zip")) {
									model.addElement(o.getKey());
								}
							}
							if (listRemote.size() > 1) {
								if (mode == AWS.DOWNLOAD) {
									list.setSelectedIndex(0);
								}
							}
						} else {
							errorDialog("Unable to connect AWS");
						}
					} else {
						errorDialog(errorMessage);
					}
				}
			});
		}
		return loadRemoteButton;
	}

	private JScrollPane getRemoteListPanel() {
		model = new DefaultListModel<String>();
		list = new JList<String>(model);
		JScrollPane pane = new JScrollPane(list);
		return pane;
	}

	private JPanel getCtrlPanel() {
		if (ctrlPanel == null) {
			ctrlPanel = new JPanel();
			ctrlPanel.setLayout(new BorderLayout());
			ctrlPanel.add(getButtonGrid(), BorderLayout.CENTER);
		}
		return ctrlPanel;
	}

	private JPanel getButtonGrid() {
		if (buttonGrid == null) {
			GridLayout gridLayout = new GridLayout();
			gridLayout.setRows(1);
			gridLayout.setHgap(1);
			buttonGrid = new JPanel();
			buttonGrid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			buttonGrid.setLayout(gridLayout);
			buttonGrid.add(getOkButton());
			buttonGrid.add(getCancelButton());
		}
		return buttonGrid;
	}

	private JButton getOkButton() {
		if (okButton == null) {
			okButton = new JButton();
			okButton.setText(ResourceBundleHelper.getMessageString(DialogItem.upload_trace_dialog_button_ok));
			okButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {				
					executeOkButton();
				}
			});
		}
		return okButton;
	}

	private void executeOkButton() {
		setCredentials();
		AwsRepository awsRepo = new AwsRepository(awsInfo,getProxySetting());		
		if(awsRepo.isAuthenticated()) {
			processTrace(mode,awsInfo);
		}else {
			errorDialog(errorMessage);
		}
		
 	}
	
	private void processTrace(AWS awsMode, HashMap<AWSInfoCredentials, String> awsInfo){
		AROProgressDialog progressBar = new AROProgressDialog(this,"in progress.....");
		progressBar.setVisible(true);
		AwsRepository awsRepo = new AwsRepository(awsInfo,getProxySetting());

		File foder = chooseTraceFolder(JFileChooser.DIRECTORIES_ONLY, "Choose folder");
		TraceManager tm = new TraceManager(awsRepo);
		
		SwingWorker<State, Void> worker = new SwingWorker<State,Void>(){
			@Override
			protected State doInBackground() throws Exception {					
				 if(awsMode ==AWS.DOWNLOAD) {
					 return tm.download(list.getSelectedValue(), foder.getAbsolutePath());
				 }else if(awsMode == AWS.UPLOAD) {
					 return tm.upload(foder.getAbsolutePath());
				 }else {
					 errorDialog(errorMessage);
					 progressBar.dispose();
					 clean();
					 return State.FAILURE;
				 }
			}
			@Override
			protected void done() {					 
				super.done();
				try {
					if(State.COMPLETE == get()){
						progressBar.setVisible(false);
						progressBar.dispose();
						MessageDialogFactory.showMessageDialog(getOwner(), ResourceBundleHelper.getMessageString("aws.proxy.message"));
						clean();
					}else {
						 errorDialog(errorMessage);
						 progressBar.dispose();
						 clean();
					}
				} catch (InterruptedException e) {
 					e.printStackTrace();
				} catch (ExecutionException e) {
 					e.printStackTrace();
				}
			}
		};
		worker.execute();
		
	}

	private void errorDialog(String errorMessage) {
		String rawMessage = resourceBundle.getString("aws.error.auth");
		String message = MessageFormat.format(rawMessage, errorMessage);
		MessageDialogFactory.getInstance().showErrorDialog(this, message);
	}


	private File chooseTraceFolder(int mode, String title) {
		UserPreferences userPreferences = UserPreferencesFactory.getInstance().create();
		File tracePath = null;
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(title);
		chooser.setFileSelectionMode(mode);
		String defaultDir = userPreferences.getLastTraceDirectory() == null ? System.getProperty("user.home")
				: userPreferences.getLastTraceDirectory().toString();
		chooser.setCurrentDirectory(new File(defaultDir));
		chooser.setAcceptAllFileFilterUsed(false);
		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			tracePath = chooser.getSelectedFile();			
		}
		return tracePath;
	}

	private JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton();
			cancelButton.setText(ResourceBundleHelper.getMessageString(DialogItem.upload_trace_dialog_button_cancel));
			cancelButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					executeCancelButton();
				}
			});
		}
		return cancelButton;
	}

	private void executeCancelButton() {
		clean();
	}

	private void clean() {
		setVisible(false);
		dispose();
	}

	private JTextField createTextFieldAndProperties(String title) {
		JTextField textField = new JTextField();
		TitledBorder ttlBorder = BorderFactory.createTitledBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createRaisedBevelBorder(), BorderFactory.createLoweredBevelBorder()), title);
		ttlBorder.setTitleColor(Color.BLUE);
		ttlBorder.setTitleFont(ttlBorder.getTitleFont().deriveFont(Font.BOLD));
		textField.setBorder(ttlBorder);
		return textField;
	}

	private void setCredentials() {
		awsInfo.put(AWSInfoCredentials.AccessID, accessIdText.getText());
		awsInfo.put(AWSInfoCredentials.SecretKey, secretText.getText());
		awsInfo.put(AWSInfoCredentials.Region, regionText.getText());
		awsInfo.put(AWSInfoCredentials.BucketName, bucketNameText.getText());
	}	

}
