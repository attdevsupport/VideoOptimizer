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
package com.att.aro.ui.view.video;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.datacollector.DataCollectorType;
import com.att.aro.core.datacollector.IDataCollector;
import com.att.aro.core.datacollector.IVideoImageSubscriber;
import com.att.aro.core.util.ImageHelper;
import com.att.aro.core.util.Util;
import com.att.aro.core.video.pojo.VideoOption;
import com.att.aro.ui.commonui.ImagePanel;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.SharedAttributesProcesses;

public class LiveScreenViewDialog extends JDialog implements IVideoImageSubscriber {
	
	private static final Logger LOG = LogManager.getLogger(LiveScreenViewDialog.class.getName());
	private static final long serialVersionUID = 1L;

	private final JPanel contentPanel = new JPanel();

	private ImagePanel imagePanel;

	private JTextField timeBox;

	private long startTime;

	private SharedAttributesProcesses theView;
	
	private IDataCollector collector;
	
	private VideoOption videoOption;

	/**
	 * Create the dialog.
	 * subscribes to the collector.
	 * 
	 * @param mainFrame
	 * 
	 * @param collector
	 */
	public LiveScreenViewDialog(SharedAttributesProcesses theView, IDataCollector collector) {
		this.theView = theView;
		this.collector = collector;
		setModalityType(ModalityType.MODELESS);
		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		setTitle("Live Video Screen Capture");
		setResizable(false);
		setBounds(100, 100, 367, 722);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);

		Image image = (new ImageIcon(getClass().getResource(ResourceBundleHelper.getImageString("ImageBasePath") 
									+ ResourceBundleHelper.getImageString("Image.blackScreen"))))
									.getImage();

		imagePanel = new ImagePanel(image);
		imagePanel.setBounds(0, 0, 360, 640);

		contentPanel.add(imagePanel);

		getContentPane().add(dashBoardPane(), BorderLayout.SOUTH);

		LOG.info("subscribed");
		setVisible(true);
		collector.addVideoImageSubscriber(this);
	}
	
	public void setVideoOption(VideoOption videoOption) {
		this.videoOption = videoOption;
		onStart();
	}

	private JPanel dashBoardPane() {

		JPanel dashBoardPane = new JPanel(new BorderLayout());

		{ // timer display
			JPanel timerPane = new JPanel();
			timeBox = new JTextField();
			timeBox.setText("00:00:00");
			timerPane.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
			JTextArea timeBoxLabel = new JTextArea();
			timeBoxLabel.setBackground(SystemColor.window);
			timeBoxLabel.setText("Elapsed Time:");
			timerPane.add(timeBoxLabel);
			timerPane.add(timeBox);
			dashBoardPane.add(timerPane, BorderLayout.WEST);
		}

		{ // stop button
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			dashBoardPane.add(buttonPane, BorderLayout.EAST);
			{
				JButton stopButton = new JButton("Stop");
				stopButton.setIcon(//new ImageIcon(LiveScreenViewDialog.class.getResource("/com/att/aro/images/X_active.png")));
						new ImageIcon(getClass().getResource(ResourceBundleHelper.getImageString("ImageBasePath") + ResourceBundleHelper.getImageString("Image.bpFailDark"))));

				stopButton.setActionCommand("Stop");
				stopButton.setFocusable(false);
				stopButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						onStop();
					}
				});
				buttonPane.add(stopButton);
				getRootPane().setDefaultButton(stopButton);
			}
		}
		
		return dashBoardPane;
	}

	/**
	 * forwards user action START command to theView
	 */
	void onStart() {
		if (this.collector.getType().equals(DataCollectorType.IOS) && VideoOption.HDEF.equals(videoOption)) {
			MessageDialogFactory.showMessageDialog(null, ResourceBundleHelper.getMessageString("Message.start.screenrecording"), "Start Collection",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	/**
	 * forwards user action STOP command to theView
	 */
	void onStop() {
		setVisible(false);
		if (this.collector.getType().equals(DataCollectorType.IOS) && VideoOption.HDEF.equals(videoOption)) {
			MessageDialogFactory.showMessageDialog(null, ResourceBundleHelper.getMessageString("Message.stop.screenrecording"), "Stop Collection", JOptionPane.INFORMATION_MESSAGE);
		}
		theView.stopCollector();
	}


	/**
	 * Used to fit image to panel
	 * 
	 * @return width
	 */
	public int getViewWidth() {
		return imagePanel.getWidth();
	}

	/**
	 * Used to fit image to panel
	 * 
	 * @return height
	 */
	public int getViewHeight() {
		return imagePanel.getHeight();
	}

	@Override
	public void receiveImage(BufferedImage image) {
		LOG.debug("receiveImage");
		if (isVisible()) {
			if (startTime == 0) {
				startTimer();
			}
			BufferedImage newimg = ImageHelper.resize(image, getViewWidth(), getViewHeight());
			imagePanel.setImage(newimg);
			updateTimer();
		}
	}

	/**
	 * 
	 */
	private void startTimer() {
		startTime = System.currentTimeMillis();
	}

	/**
	 * 
	 */
	private void updateTimer() {
		String theTime = "";
		int elapsed = (int) ((System.currentTimeMillis() - startTime) / 1000);

		theTime = Util.formatHHMMSS(elapsed);
		
		LOG.info("elapsed = " + elapsed + "  time:" + theTime);
		timeBox.setText(theTime);

	}
	
}
