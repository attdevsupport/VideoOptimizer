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
package com.att.aro.ui.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.annotation.Nonnull;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.concurrent.IThreadExecutor;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.settings.impl.SettingsImpl;
import com.att.aro.core.util.IResultSubscriber;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.core.videouploadanalysis.FileSubmit;
import com.att.aro.core.videouploadanalysis.ImageBoundsGrabber;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class VideoPostDialog extends ConfirmationDialog implements IResultSubscriber{
	private static final long serialVersionUID = 1L;

	private JLabel msgLabel;
	private static final Logger LOG = LogManager.getLogger(VideoPostDialog.class);	
	private static IExternalProcessRunner extRunner = (IExternalProcessRunner) ContextAware.getAROConfigContext().getBean("externalProcessRunnerImpl");
	private static ImageBoundsGrabber imageBoundsGrabber = (ImageBoundsGrabber) ContextAware.getAROConfigContext().getBean("imageBoundsGrabber");
	private static IThreadExecutor threadexecutor = (IThreadExecutor) ContextAware.getAROConfigContext().getBean("threadexecutor");
	private static IFileManager filemanager = (IFileManager) ContextAware.getAROConfigContext().getBean("fileManager");
	
	@Nonnull
	private VideoStream videoStream;
	
	@Nonnull
	private String traceFolder;

	private JPanel buttonPanel;
	private JPanel jButtonGrid;
	private JButton cancelButton;
	private JButton startButton;

	protected boolean proceed;

	private File file1, file2;

	private JPanel statusPane;

	private JTextArea statusMessage;

	private JPanel detail;

	private String startString;

	private double videoSize;

	private FileSubmit fileSubmit;

	private TraceDirectoryResult traceResult;

	public VideoPostDialog(AbstractTraceResult traceResult, VideoStream videoStream) {
		this.videoStream = videoStream;
		this.traceFolder = traceResult.getTraceDirectory();
		if (TraceResultType.TRACE_DIRECTORY.equals(traceResult.getTraceResultType())) {
			this.traceResult = (TraceDirectoryResult) traceResult;
		}
	}

	@Override
	public void createDialog() {
		setUndecorated(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(resourceBundle.getString("vpost.dialog.title"));
		setResizable(false);
		setBounds(400, 300, 400, 150);
		setPreferredSize(new Dimension(730, 270));
		
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		add(panel);
		
		GridBagConstraints constraint = new GridBagConstraints();
		constraint.fill = GridBagConstraints.HORIZONTAL;
		constraint.gridx = 1;
		constraint.gridy = 0;
		constraint.insets = new Insets(0, 10, 0, 0);
		constraint.weightx = 1;
		
		panel.add(getMessage(constraint));
		panel.add(getDetails());
		panel.add(getStatusPane(), BorderLayout.PAGE_END);
		panel.add(getButtonPanel(), BorderLayout.PAGE_END);
		
		updateStatus("Collecting meta data...");
		
		pack();
		panel.setSize(panel.getPreferredSize());
		panel.validate();
	}
	
	private Component getDetails() {
		detail = new JPanel(new GridBagLayout());
		collectVideoData();
		return detail;
	}

	private String getFormattedTime(double strTime) {
		try {
			return Util.formatHHMMSS((int) strTime);
		} catch (Exception e) {
			return "Exception :" + e.getMessage();
		}
	}

	private String dateStrFromLongString(String timeStamp) {
		try {
			return (new Date(Long.valueOf(timeStamp))).toString();
		} catch (NumberFormatException e) {
			return "Exception :" + e.getMessage();
		}
	}

	private JPanel getMessage(GridBagConstraints constraint) {
		msgLabel = new JLabel(getLabelMsg());
		msgLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		JPanel labelPanel = new JPanel();
		labelPanel.setLayout(new GridBagLayout());
		labelPanel.add(msgLabel, constraint);
		return labelPanel;
	}

	@Override
	public String getLabelMsg() {
		return resourceBundle.getString("vpost.dialog.message");
	}

	/**
	 * This method initializes buttonPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getButtonPanel() {
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
			startButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (startString.equals(startButton.getText())) {
						LOG.debug("Button.start pressed");
						proceed = true;
						if (processUpload()) {
							configButton(startButton, "Continue", false);
						} else {
							proceed = false;
							configButton(startButton, "Continue", true);
						}
					} else {
						disposeDialog();
					}
				}
			});
			if (videoStream.isSelected()) {
				startButton.setEnabled(true);
			} else {
				startButton.setEnabled(false);
			}
		}
		
		// Submitted: InboundJaxrsResponse{context=ClientResponse{method=POST, uri=https://www.amvots.com/amvots_rs/rest/file/multipleFiles, status=200, reason=OK}}
		
		return startButton;
	}

	protected Component getStatusPane() {
		statusPane = new JPanel(new BorderLayout());
		statusMessage = new JTextArea(3,1);
		statusMessage.setLineWrap(true);
		statusMessage.setEditable(false);
		statusMessage.setFont(new Font("Lucida Grande", Font.BOLD, 14));
		statusPane.add(statusMessage, BorderLayout.CENTER);

		return statusPane;
	}
	
	/**
	 * Receive and post updates to the status pane
	 * 
	 * @param message
	 */
	public void updateStatus(String message) {
		LOG.info(String.format("Status :%s", message));
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
	protected boolean processUpload() {		
		
		updateStatus(" Uploading, Please wait...");

		fileSubmit = new FileSubmit();
		fileSubmit.setSubscriber(this);
		fileSubmit.init(this, SettingsImpl.getInstance().getAttribute("amvotsURL"), file1, file2, false);
		
		threadexecutor.executeFuture(fileSubmit);
	
		return true;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void receiveResults(Class sender, Boolean success, String result) {
		LOG.info(String.format("Received Status from %s :%b", sender.getSimpleName(), success));
		switch (sender.getSimpleName()) {
		case "ImageBoundsGrabber":
			if (success != null) {
				if (success && result != null && !result.isEmpty()) {
					LOG.info(String.format("Success :%b %s", success, result));
					updateStatus("  Ready to Send, press Start button.");
					configButton(startButton, "Start", true);
					pack();
					panel.validate();
				} else {
					configButton(startButton, "Continue", true);
					if (ImageBoundsGrabber.Status.FAILED.toString().equals(result)) {
						LOG.info(String.format("FAILED %s", result));
						updateStatus("Upload error :failed to find video image bounds");
					} else {
						LOG.error("Failure :" + (result != null ? result : "unknown"));
						updateStatus("Failure :" + (result != null ? result : "unknown"));
					}
				}
			} else {
				updateStatus(result);
			}
			break;

		case "FileSubmit":
			configButton(startButton, "Continue", true);
			if (success && result != null && !result.isEmpty()) {
				LOG.info(String.format("Success :%b %s", success, result));
				updateStatus(" Upload finished successfully.");
			} else {
				updateStatus(" Upload failed " + result);
			}
			break;

		default:
			LOG.info(String.format("Unrecognized sender %s", sender.getSimpleName()));
			configButton(startButton, "Continue", true);
			break;
		}
	}

	void configButton(JButton button, String text, boolean state) {
		button.setText(text);
		button.setEnabled(state);
	}
	
	protected void collectVideoData() {
		try {
			imageBoundsGrabber.init((IResultSubscriber)this, traceFolder, videoStream);
			threadexecutor.executeFuture(imageBoundsGrabber);
			LOG.info("ImageBoundsGrabber launched in background");
		} catch (Exception e) {
			LOG.error("Amvots upload error :" + e.getMessage());
			updateStatus("Upload error :" + e.getMessage());
			configButton(startButton, "Continue", true);
		}
	}


	private String getPlayer() {
		String results = "unknown";
		String path = filemanager.createFile(traceFolder, "videoApp").toString();
		if (filemanager.fileExist(path)) {
			try {
				results = filemanager.readAllLine(path)[0];
			} catch (IOException e) {
				results = "unknown";
			}
		}
		return results;
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
					proceed = false;
					LOG.debug(ResourceBundleHelper.getMessageString("Button.cancel"));
					disposeDialog();
				}
			});
		}
		return cancelButton;
	}

	protected void disposeDialog() {
		if (imageBoundsGrabber!=null) {
			imageBoundsGrabber.setStop();
		}
		if (fileSubmit!=null) {
			fileSubmit.setStop();
		}
		VideoPostDialog.this.dispose();
	}

}
