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
package com.att.aro.ui.view.menu.file;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.preferences.UserPreferencesFactory;
import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.EnableEscKeyCloseDialog;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.SharedAttributesProcesses;

import lombok.Getter;

public class OpenPcapFileDialog extends JDialog {
	private static final long serialVersionUID = 496350871409866509L;

	private static final Logger LOG = LogManager.getLogger(OpenPcapFileDialog.class);

	private final SharedAttributesProcesses parent;
	private final Component caller;

	private JPanel jContentPane;
	private JPanel jButtonPanel;
	private JPanel jButtonGrid;
	private JButton okButton;

	@Getter
	private final String newPcapFileTracePath;
	private final File originalPcapFileObj;

	@Getter
	private boolean retainDirectory;

	private IFileManager fileManager = ContextAware.getAROConfigContext().getBean(IFileManager.class);

	private File pcapFile;

	public OpenPcapFileDialog(SharedAttributesProcesses parent, Component caller, File pcapFile, String dotReadme) {
		this.parent = parent;
		this.caller = caller;
		caller.setEnabled(false);
		this.pcapFile = pcapFile;

		String newDirectoryName = pcapFile.getName().substring(0, pcapFile.getName().lastIndexOf("."));
		newPcapFileTracePath = pcapFile.getParent() + Util.FILE_SEPARATOR + newDirectoryName;
		originalPcapFileObj = pcapFile;

		init(newDirectoryName);
		setModal(true);
	}

	private void init(String newDirectoryName) {
		setContentPane(this.getContentPane(newDirectoryName));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setTitle("Open PCAP File");

		EnableEscKeyCloseDialog escKeyCloseDialog = new EnableEscKeyCloseDialog(getRootPane(), this, false);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowDeactivated(WindowEvent event) {
				if (escKeyCloseDialog.consumeEscPressed()) {
					dispose();
				}
			}
		});

		pack();
		setLocationRelativeTo(parent.getFrame());
		getRootPane().setDefaultButton(okButton);
	}

	private JComponent getContentPane(String newDirectoryName) {
		if (jContentPane == null) {
			jContentPane = new JPanel(new BorderLayout());

			JLabel desc = new JLabel(MessageFormat.format(ResourceBundleHelper.getMessageString("menu.file.pcap.dialog.desc")
					, newDirectoryName), SwingConstants.LEFT);
			desc.setFont(new Font("Arial", Font.PLAIN, 15));
			JPanel lblPanel = new JPanel(new BorderLayout());
			lblPanel.add(desc);

			jContentPane.add(lblPanel, BorderLayout.NORTH);
			jContentPane.add(getButtonPanel(), BorderLayout.SOUTH);
			jContentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

			jContentPane.setPreferredSize(new Dimension(600, 225));
		}

		return jContentPane;
	}

	private JPanel getButtonPanel() {
		if (jButtonPanel == null) {
			jButtonPanel = new JPanel();
			BorderLayout layout = new BorderLayout();
			layout.setHgap(0);
			jButtonPanel.setLayout(new BorderLayout());
			jButtonPanel.add(getButtonGrid(), BorderLayout.EAST);
		}

		return jButtonPanel;
	}

	private JPanel getButtonGrid() {
		if (jButtonGrid == null) {
			GridLayout gridLayout = new GridLayout();
			gridLayout.setRows(1);
			gridLayout.setHgap(10);
			jButtonGrid = new JPanel();
			jButtonGrid.setLayout(gridLayout);
			jButtonGrid.add(getButton("Yes", (ActionEvent arg) -> process(true)));
			jButtonGrid.add(okButton = getButton("No", (ActionEvent arg) -> process(false)));
			jButtonGrid.add(getButton("Cancel", (ActionEvent arg) -> dispose()));
		}

		return jButtonGrid;
	}

	private JButton getButton(String text, ActionListener al) {
		JButton button = new JButton();
		button.setText(text);
		button.addActionListener(al);
		return button;
	}

	@Override
	public void dispose() {
		caller.setEnabled(true);
		super.dispose();
	}

	private void process(boolean retainDirectory) {
		setVisible(false);
		wrapPcap(retainDirectory);
		
		this.retainDirectory = retainDirectory;
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				return processPcapFile(retainDirectory);
			}

			@Override
			protected void done() {
				try {
					get();
				} catch (Exception ex) {
					LOG.error("Something went wrong while processing PCAP file", ex);
				}

				dispose();
			}
		};
		worker.execute();
	}

	private Void processPcapFile(boolean retainDirectory) {
		((MainFrame)parent).wipeCurrentTraceInitialAnalyzerResult();
		loadTrace();
		GoogleAnalyticsUtil.getAndIncrementTraceCounter();

		// Delete the new directory after analysis is completed, if don't need to retain
		if (!retainDirectory) {
			if (parent instanceof MainFrame) {
				while (!((MainFrame) parent).getAroSwingWorker().isDone()) {
					LOG.debug("Waiting for analysis to be completed");
					Util.sleep(1000);
				}

				PacketAnalyzerResult analyzerResult = ((MainFrame) parent).getController().getCurrentTraceInitialAnalyzerResult();
				if (analyzerResult != null && analyzerResult.getStreamingVideoData() != null && !analyzerResult.getStreamingVideoData().isFinished()) {
					StreamingVideoData streamingVideoData = analyzerResult.getStreamingVideoData();
					// Wait for FFMpegRunner to complete
					if (!streamingVideoData.getVideoStreamMap().isEmpty()) {
						Runnable waitingForUpdate = () -> {
							int count = 1;
							while (!streamingVideoData.isFinished()) {
								LOG.info(String.format("(%d) Waiting for FFmpegRunner to complete", count++));
								Util.sleep(1000);
							}
							Util.sleep(1000);
							deleteDirectory(newPcapFileTracePath);
						};
						new Thread(waitingForUpdate, "FFMpegRunnerWaitingThread").start();
					} else {
						deleteDirectory(newPcapFileTracePath);
					}
				} else {
					deleteDirectory(newPcapFileTracePath);
				}
			} else {
				deleteDirectory(newPcapFileTracePath);
			}
			PreferenceHandlerImpl.getInstance().setPref("TRACE_PATH", originalPcapFileObj.toString());
		}

		return null;
	}

	/**
	 * Wraps pcap file inside of a new folder with the same name as the pcap file, minus the extension
	 * setting an invisible file ".readme" or ".temp_trace"
	 * 
	 * @param retainDirectory
	 */
	private void wrapPcap(boolean retainDirectory) {
		fileManager.mkDir(newPcapFileTracePath);
		File destination = new File(newPcapFileTracePath, pcapFile.getName());
		if (retainDirectory) {
			fileManager.move(originalPcapFileObj.toString(), destination.toString());
			Util.createTimeFile(destination.getParentFile(), destination.getName(), TraceDataConst.FileName.DOT_README);
		} else {
			fileManager.createEmptyFile(newPcapFileTracePath, ".temp_trace");
			fileManager.createLink(destination, originalPcapFileObj);
			parent.setPcapTempWrap(true);
		}
	}

	private void deleteDirectory(String directoryPath) {
		try {
			FileUtils.deleteDirectory(new File(directoryPath));
			LOG.info("Successfully deleted temporary directory: " + directoryPath);
		} catch (IOException e) {
			LOG.error("Failed to delete temporary directory", e);
		}
	}

	private void loadTrace() {
		LOG.info("Starting analysis");
		if (parent instanceof MainFrame) {
			if (StringUtils.isNotBlank(newPcapFileTracePath)) {
				// open PCAP
				File tracePath = new File(newPcapFileTracePath, originalPcapFileObj.getName());
				parent.updateTracePath(tracePath);
				UserPreferencesFactory.getInstance().create().setLastTraceDirectory(tracePath.isFile() 
									? tracePath.getParentFile().getParentFile()
									: tracePath.getParentFile().getParentFile().getParentFile());
				GoogleAnalyticsUtil.getAndIncrementTraceCounter();
			}
		}
		LOG.info("Launched PCAP analysis");
	}
}
