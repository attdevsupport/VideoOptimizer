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
package com.att.aro.ui.view.menu.datacollector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.att.aro.core.peripheral.pojo.AttenuatorModel;
import com.att.aro.core.util.Util;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class AttnrLoadProfilePanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	private JButton loadProfileButton;
	private JLabel labelLoadFile;
	private String loadFile;
 	private AttenuatorModel miniAtnr;
	
	public AttnrLoadProfilePanel(AttenuatorModel miniAtnr) {
		setLayout(new GridBagLayout());
		this.miniAtnr = miniAtnr;
		loadFile = ResourceBundleHelper.getMessageString("menu.profile.load");
		loadProfileButton = new JButton(loadFile);
		loadProfileButton.addActionListener(this);
		labelLoadFile = new JLabel(Util.getVideoOptimizerLibrary()+System.getProperty("file.separator")+"Default_Profile.txt");
		miniAtnr.setLocalPath(labelLoadFile.getText());// set initial profile 
 		add(loadProfileButton, new GridBagConstraints(0, 0, 1, 1, 0.1, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		add(labelLoadFile, new GridBagConstraints(1, 0, 1, 1, 1, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		setEnabled(false);
  	}

	@Override
	public void actionPerformed(ActionEvent e) {
 		if (e.getActionCommand().equals(loadFile)) {
			File tracePath = null;
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Network Simulation");
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			String defaultDir = Util.getVideoOptimizerLibrary();
			FileNameExtensionFilter shFilter = new FileNameExtensionFilter(
					"text format file (*.txt)", "txt");
			chooser.setCurrentDirectory(new File(defaultDir));
			chooser.setFileFilter(shFilter);
 			if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				tracePath = chooser.getSelectedFile();
				String tempPath = tracePath.getAbsolutePath(); 
				labelLoadFile.setText(tempPath);
				miniAtnr.setLocalPath(labelLoadFile.getText());				
				miniAtnr.setLoadProfile(true);				
			}
 		}
	}

	public void resetComponent() {
		
		labelLoadFile.setText(Util.getVideoOptimizerLibrary()+System.getProperty("file.separator")+"Default_Profile.txt");
		miniAtnr.setLocalPath(labelLoadFile.getText());
		miniAtnr.setLoadProfile(false);

	}

}
