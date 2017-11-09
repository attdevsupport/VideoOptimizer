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

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.springframework.util.FileCopyUtils;

import com.att.aro.core.ILogger;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;

public class LoadManifestDialog extends JPanel {
	
	private ILogger log = ContextAware.getAROConfigContext().getBean(ILogger.class);
	
	private static final long serialVersionUID = 1L;
	private JButton loadBtn;
	private MainFrame aroView;

	public LoadManifestDialog(MainFrame aroView) {
		this.aroView = aroView;

		setLayout(new FlowLayout());

		loadBtn = new JButton(ResourceBundleHelper.getMessageString("videoTab.load"));

		loadBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				loadManifest();
			}
		});
		add(loadBtn);
	}

	/**
	 * Copy a file, files or a folder of files into tracepath/downloads/ Will traverse folders within a folder
	 */
	protected void loadManifest() {

		if (aroView.getTracePath() != null) {
			// Open filechooser
			JFileChooser fileChooser = new JFileChooser(aroView.getTracePath());
			fileChooser.setMultiSelectionEnabled(true);
			fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			int result = fileChooser.showOpenDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {

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
						log.error("user error :Chose downloads folder, will ignore");
					}
				} catch (IOException e1) {
					log.error("IOException :"+e1.getMessage());
				}
				// refresh analyzer
				aroView.updateTracePath(new File(aroView.getTracePath()));
			}
		}
	}
}
