/*
*  Copyright 2017 AT&T
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
package com.att.aro.ui.view.videotab;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.util.FileCopyUtils;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.fileio.impl.FileManagerImpl;
import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.commonui.TabPanelJPanel;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;

import lombok.Getter;


public class LoadManifestDialog extends TabPanelJPanel implements ActionListener{
	
	private static final Logger LOG = LogManager.getLogger(LoadManifestDialog.class);	
	private static final long serialVersionUID = 1L;
	private JButton loadBtn;
	private JCheckBox checkBoxCSI;
	private MainFrame aroView;
	@Getter
	private AnalysisFilter analysisfilter;

	public LoadManifestDialog(MainFrame aroView) {
		
		this.aroView = aroView;

		setLayout(new FlowLayout());
		setName(LoadManifestDialog.class.getName());
		loadBtn = new JButton(ResourceBundleHelper.getMessageString("videoTab.load"));
		checkBoxCSI = new JCheckBox("CSI");
		checkBoxCSI.setEnabled(false);
		checkBoxCSI.addActionListener(this);
		loadBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadManifest(analysisfilter);
			}
		});
		add(loadBtn);
		add(checkBoxCSI);
	}

	/**
	 * Copy a file, files or a folder of files into tracepath/downloads/ Will traverse folders within a folder
	 */
	protected void loadManifest(AnalysisFilter analysisfilter) {
		
		if (aroView.getTracePath() != null) {
			// Open filechooser
			JFileChooser fileChooser = new JFileChooser(aroView.getTracePath());
			fileChooser.setMultiSelectionEnabled(true);
			fileChooser.addChoosableFileFilter( new FileNameExtensionFilter("mpd, m3u8 or json","mpd","m3u8","json"));
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setAcceptAllFileFilterUsed(false);
			int result = fileChooser.showOpenDialog(this);			
			if (result == JFileChooser.APPROVE_OPTION) {
				if(checkBoxCSI.isSelected() && analysisfilter!=null) {
					String folderNameForCSIStuff = aroView.getTracePath() + File.separator + "CSI";
					String expectedManifestFileName =  folderNameForCSIStuff + File.separator + fileChooser.getSelectedFile().getName();
					File expectedManifestFile = new File(expectedManifestFileName);
					
					try {
						if (!fileChooser.getSelectedFile().getPath().equals(expectedManifestFileName) && !expectedManifestFile.exists()) {
							IFileManager fileManager = new FileManagerImpl();
							if (fileManager.directoryExistAndNotEmpty(expectedManifestFile.getParentFile().getPath())) {
								if (folderExistsDialog() == JOptionPane.YES_OPTION) {
									FileUtils.deleteDirectory(expectedManifestFile.getParentFile());
								}
							}

							if (!Files.exists(expectedManifestFile.getParentFile().toPath())) {
								Files.createDirectory(expectedManifestFile.getParentFile().toPath());
							}

							if (fileChooser.getSelectedFile().getParent().equals(aroView.getTracePath())) {
								Files.move(fileChooser.getSelectedFile().toPath(), expectedManifestFile.toPath());
							} else {
								Files.copy(fileChooser.getSelectedFile().toPath(), expectedManifestFile.toPath());
							}
						}
					} catch (IOException ioe) {
						expectedManifestFileName = fileChooser.getSelectedFile().getPath();
					}
					analysisfilter.setCSI(true);
					analysisfilter.setManifestFilePath(expectedManifestFileName);
					((MainFrame)aroView).updateFilter(analysisfilter);
					
				} else {
					// save selected file/files inside downloads folder
					fileChooser.getSelectedFile().getPath();
					String downloadsPath = aroView.getTracePath() + Util.FILE_SEPARATOR + "downloads";
					try {
						File[] files = fileChooser.getSelectedFiles();
						if (!files[0].toString().equals(downloadsPath)) {
							// expand out of folder
							if (fileChooser.getSelectedFile().isDirectory()) {
								files = files[0].listFiles();
							}
							if( null == files){
								return;
							}
							for (File file : files) {
								if (file.isFile()) {
									FileCopyUtils.copy(file, new File(downloadsPath, file.getName()));
								}
							}
						} else {
							LOG.error("user error :Chose downloads folder, will ignore");
						}
					} catch (IOException e1) {
						LOG.error("IOException :"+e1.getMessage());
					}
					// refresh analyzer
					aroView.updateTracePath(new File(aroView.getTracePath()));
				}
			}
		}
	}
	
	private int folderExistsDialog() {
		MessageDialogFactory.getInstance();
		String message = ResourceBundleHelper.getMessageString("csi.folderexists.confirmdelete");
		String title = MessageFormat.format(ResourceBundleHelper.getMessageString("aro.title.short"),  ApplicationConfig.getInstance().getAppShortName());
		int dialogResults = MessageDialogFactory.showConfirmDialog(aroView.getJFrame(), message, title, JOptionPane.YES_NO_OPTION);
		LOG.info("CSI Folder Dialog :" + dialogResults);
		return dialogResults;
	}

	@Override
	public JPanel layoutDataPanel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void refresh(AROTraceData analyzerResult) {
		if(analyzerResult!=null) {
			checkBoxCSI.setSelected(false);
			checkBoxCSI.setEnabled(true);
			analysisfilter = analyzerResult.getAnalyzerResult().getFilter();
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(checkBoxCSI.isSelected()) {
			if(analysisfilter!=null) {
				analysisfilter.setCSI(true);
 			}
		}
		
	}

}
