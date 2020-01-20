/*
 *  Copyright 2018 AT&T
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
package com.att.aro.ui.view.menu.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.concurrent.IThreadExecutor;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.settings.impl.SettingsImpl;
import com.att.aro.core.util.IResultSubscriber;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.videouploadanalysis.FileSubmit;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.ConfirmationDialog;

public class PostDialog extends ConfirmationDialog implements IResultSubscriber{
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LogManager.getLogger(PostDialog.class);
	protected IExternalProcessRunner extRunner = (IExternalProcessRunner) ContextAware.getAROConfigContext().getBean("externalProcessRunnerImpl");
	protected IThreadExecutor threadexecutor = (IThreadExecutor) ContextAware.getAROConfigContext().getBean("threadexecutor");
	protected IFileManager filemanager = (IFileManager) ContextAware.getAROConfigContext().getBean("fileManager");

	protected JPanel buttonPanel;
	protected JPanel jButtonGrid;
	protected JButton cancelButton;
	protected JButton startButton;
	protected JPanel statusPane;
	protected JTextArea statusMessage;
	protected FileSubmit fileSubmit;
	protected String urlKey;
	private boolean usesAuthentication = false;

	private String startString;
	
	protected File payloadFile, jsonFile;
	
	/**
	 * Receive and post updates to the status pane
	 * 
	 * @param message
	 */
	public void updateStatus(String message) {
		LOG.debug(String.format("Status :%s", message));
		if (statusMessage != null) {
			try {
				statusMessage.setText(message);
			} catch (Exception e) {
				LOG.error("updateStatus error:" + e.getMessage());
			}
		}
		this.pack();
		this.revalidate();
	}
	
	/**
	 * Trigger POST request, then display results
	 * @return
	 */
	protected void processUpload() {
		LOG.debug("processUpload()");
		updateStatus(" Uploading, Please wait...");

		fileSubmit = new FileSubmit();
		usesAuthentication = true;
		fileSubmit.init(this, SettingsImpl.getInstance().getAttribute(urlKey), payloadFile, jsonFile, usesAuthentication);
		threadexecutor.executeFuture(fileSubmit);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void receiveResults(Class sender, Boolean success, String result) {
		LOG.debug(String.format("receiveResults from %s :%b", sender.getSimpleName(), success));
		switch (sender.getSimpleName()) {

		case "Compressor":
			if (success != null) {
				if (success && result != null && !result.isEmpty()) {
					try {
						payloadFile = new File(result);
						configButton(startButton, startString, true);
						String name = payloadFile.getName(); 
						double size = (double) payloadFile.length() / (1024 * 1024);
						updateStatus(String.format("%s ready for upload\nsize: %.2f MB", name, size)); 
					} catch (Exception e) {
						updateStatus("Exception :" + e.getMessage());
						break;
					}
				} else {
					updateStatus(" Failed " + result);
				}
			} else {
				updateStatus(result);
			}
			break;

		case "FileSubmit":
			if (success && result != null && !result.isEmpty()) {
				LOG.debug(String.format("Success :%b %s", success, result));
				if(usesAuthentication) {
					updateStatus("Response: " + StringParse.findLabeledDataFromString("\"message\":", "\"", result) + "\n" + "Trace ID: " + StringParse.findLabeledDataFromString("\"traceId\":", "\"", result));
					configButton(startButton, "Done", true);
				} else {
					updateStatus("Upload Successful. Response: " + result);
					configButton(startButton, "Done", true);
				}
			} else {
				updateStatus(" Upload failed :  " + result);
				configButton(startButton, "OK", true);
			}
			break;

		default:
			LOG.debug(String.format("Unrecognized sender %s", sender.getSimpleName()));
			configButton(startButton, "OK", true);
			break;
		}
	}

	/**
	 * This method initializes buttonPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	protected JPanel getButtonPanel() {
		if (buttonPanel == null) {
			buttonPanel = new JPanel();
			buttonPanel.setLayout(new BorderLayout());
			buttonPanel.add(getJButtonGrid(), BorderLayout.EAST);
		}
		return buttonPanel;
	}

	/**
	 * This method initializes jButtonGrid
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJButtonGrid() {
		if (jButtonGrid == null) {
			jButtonGrid = new JPanel();
			jButtonGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			jButtonGrid.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
			jButtonGrid.add(getCancelButton());
			jButtonGrid.add(getStartButton());
		}
		return jButtonGrid;
	}

	/**
	 * This method initializes okButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getStartButton() {
		if (startButton == null) {
			startButton = new JButton();
			startString = ResourceBundleHelper.getMessageString("Button.start");
			startButton.setText(startString);
			configButton(startButton, "Wait", false);
			startButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (startString.equals(startButton.getText())) {
						LOG.debug("Button.start pressed");
						processUpload();
						configButton(startButton, "Continue", false);
						cancelButton.setEnabled(false);
						((JDialog) SwingUtilities.getWindowAncestor(panel)).setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
						
					} else {
						disposeDialog();
					}
				}
			});
		}
		
		return startButton;
	}
	
	/**
	 * This method initializes cancelButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton();
			cancelButton.setText(ResourceBundleHelper.getMessageString("Button.cancel"));
			cancelButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					LOG.debug(ResourceBundleHelper.getMessageString("Button.cancel"));
					disposeDialog();
				}
			});
		}
		return cancelButton;
	}

	protected Component getStatusPane() {
		LOG.debug("getStatusPane()");
		statusPane = new JPanel(new BorderLayout());
		statusMessage = new JTextArea(3,1);
		statusMessage.setLineWrap(true);
		statusMessage.setEditable(false);
		statusMessage.setFont(new Font("Lucida Grande", Font.BOLD, 14));
		statusPane.add(statusMessage, BorderLayout.CENTER);

		return statusPane;
	}
	

	void configButton(JButton button, String text, boolean state) {
		button.setText(text);
		button.setEnabled(state);
	}

	protected void disposeDialog() {
		LOG.debug("disposeDialog()");
		if (fileSubmit != null) {
			fileSubmit.setStop();
		}
	}
}
