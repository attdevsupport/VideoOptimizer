
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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.att.aro.mvc.IAROView;

public class DownloadManifest extends JDialog implements ActionListener{

	private JButton selectBtn;
	private JButton loadBtn;
	private JLabel nameLbl;
	private JPanel mainPanel;
	private JPanel panel;
	private IAROView aroView;
	//private final String PATH = 
	public DownloadManifest(IAROView aroview){
		this.aroView = aroview;
		setUndecorated(false);
		setDefaultCloseOperation(this.DISPOSE_ON_CLOSE);
		setBounds(300, 200, 1000, 1000);
		
		mainPanel = getMainPanel();
		panel= getPanel();
		
		selectBtn = new JButton("Select File");
		selectBtn.setName("SelectFile");
		selectBtn.addActionListener(this);
		nameLbl = new JLabel("");
		
		panel.add(selectBtn);
		selectBtn.setAlignmentX(LEFT_ALIGNMENT);
		panel.add(nameLbl);
		mainPanel.add(panel);
		panel.setAlignmentX(LEFT_ALIGNMENT);
		
		loadBtn = new JButton("Load Manifest");
		loadBtn.setName("Load");
		loadBtn.addActionListener(this);
		
		mainPanel.add(loadBtn);
		loadBtn.setAlignmentX(LEFT_ALIGNMENT);
		add(mainPanel);

		mainPanel.setSize(mainPanel.getPreferredSize());
		mainPanel.validate();
	}

	private JPanel getMainPanel(){
		if(mainPanel == null){
			mainPanel = new JPanel();
			mainPanel.setPreferredSize(new Dimension(500,80));
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		}
		return mainPanel;
	}
	
	private JPanel getPanel(){
		if(panel == null){
			panel= new JPanel(new FlowLayout());
		}
		return panel;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		JButton btn = (JButton)e.getSource();
		if(btn.getName().equals("SelectFile")){
			JFileChooser fileChooser = new JFileChooser();
			int result = fileChooser.showOpenDialog(this);
			if(result == JFileChooser.APPROVE_OPTION){
				nameLbl.setText(fileChooser.getSelectedFile().getPath());
			}
		}else if(btn.getName().equals("Load")){
			dispose();
			//this.aroView.getTracePath();
			this.aroView.updateTracePath(new File(this.aroView.getTracePath()));
			//this.aroView.refresh();
		}
		
	}
}

