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
import javax.swing.filechooser.FileNameExtensionFilter;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.springframework.util.FileCopyUtils;

import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.util.Util;
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

		checkBoxCSI = new JCheckBox("CSI");
		checkBoxCSI.setEnabled(false);
		checkBoxCSI.addActionListener(this);
		loadBtn = new JButton(ResourceBundleHelper.getMessageString("videoTab.load"));

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
			fileChooser.addChoosableFileFilter( new FileNameExtensionFilter("manifest file or json","mpd","m3u8","json"));
			fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			int result = fileChooser.showOpenDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {
				if(checkBoxCSI.isSelected() && analysisfilter!=null) {
					analysisfilter.setCSI(true);
					analysisfilter.setManifestFilePath(fileChooser.getSelectedFile().getPath());
					((MainFrame)aroView).updateFilter(analysisfilter);

				}else {
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
