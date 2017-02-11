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
package com.att.aro.ui.view.diagnostictab;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToolTip;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.ui.view.diagnostictab.plot.VideoChunksPlot;
import com.att.aro.ui.view.video.IVideoPlayer;

public class SliderDialogBox extends JDialog {

	private static final long serialVersionUID = 1L;
	private JSlider slider;
	private JButton okButton;
	private int maxValue;
	private IVideoPlayer player;
	private double startTime;
	private VideoChunksPlot vcPlot;

	private GraphPanel parentPanel;
	private JComboBox<ComboBoxItem> comboBox;

	private BufferedImage thumbnail;
	private BufferedImage resizedThumbnail;
	private Map<Integer, VideoEvent> chunkSelectionList = new TreeMap<>();

	private int selectedIndex=0;
	
	JLabel imgLabel = new JLabel() {
		public JToolTip createToolTip() {

			JToolTip imgTooltip = new JToolTip() {
				@Override
				public void paint(Graphics g) {
					g.drawImage(resizedThumbnail, 0, 0, null);
					// super.paint(g);
				}

				@Override
				public Dimension getPreferredSize() {
					return new Dimension(resizedThumbnail.getWidth(), resizedThumbnail.getHeight());
				}

			};
			imgTooltip.setComponent(this);
			return imgTooltip;
		}
	};

	private static class ComboBoxItem {
		private String value;
		private VideoEvent videoEvent;

		ComboBoxItem(String value, VideoEvent videoEvent) {
			this.value = value;
			this.videoEvent = videoEvent;
		}

		public VideoEvent getVideoEvent() {
			return videoEvent;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	public SliderDialogBox(GraphPanel parentPanel, double maxVideoTime, Map<Integer, VideoEvent> chunks,int indexKey) {
		this.parentPanel = parentPanel;
		DiagnosticsTab parent = parentPanel.getGraphPanelParent();
		player = parent.getVideoPlayer();
		vcPlot = parentPanel.getVcPlot();
		chunkSelectionList = chunks;
		createSliderDialog(maxVideoTime, player, vcPlot,indexKey);
		comboBox.setSelectedIndex(selectedIndex);
	}

	public void createSliderDialog(double max, IVideoPlayer player, final VideoChunksPlot vcPlot,int indexKey) {

		setUndecorated(false);
		setDefaultCloseOperation(this.DISPOSE_ON_CLOSE);
		setBounds(400, 200, 1000, 500);
		setPreferredSize(new Dimension(500, 160));
		setResizable(false);
        
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		add(panel);

		JPanel labelPanel = new JPanel(new BorderLayout());
		labelPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		labelPanel.add(new JLabel("Position slider to video start up time"));
		panel.add(labelPanel);

		JPanel comboPanel = new JPanel(new FlowLayout());
		comboPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 200));

		comboBox = new JComboBox<ComboBoxItem>();
		comboBox.removeAllItems();
		int indexCount=0;
		for (int key : chunkSelectionList.keySet()) {
			String value = "Segment:" +(int)chunkSelectionList.get(key).getSegment() + " at " + String.format("%.2f", chunkSelectionList.get(key).getDLTimeStamp()) + "S";
			comboBox.addItem(new ComboBoxItem(value, chunkSelectionList.get(key)));
			indexCount++;
			if(key==indexKey){
				selectedIndex = indexCount-1;
			}
		}
		comboPanel.add(comboBox);
	
		comboBox.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub		
					ComboBoxItem itemObject = (ComboBoxItem) comboBox.getSelectedItem();
					thumbnail = itemObject.getVideoEvent().getThumbnail();
					resizedThumbnail = itemObject.getVideoEvent().getImageOriginal();
					ImageIcon img = new ImageIcon(thumbnail);
					imgLabel.setIcon(img);// = new JLabel(img);
						
					int dlTimestamp = (int)itemObject.getVideoEvent().getStartTS();
						
					slider.setValue(dlTimestamp*10);				 
			}
		});
		
		imgLabel.setToolTipText("ImgToolTip");
		comboPanel.add(imgLabel);
		panel.add(comboPanel);

		JPanel sliderPanel = new JPanel(new BorderLayout());
		sliderPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

		setMaxValue(max);
		this.player = player;
		slider = new JSlider(JSlider.HORIZONTAL, 0, getMaxValue(), 0);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				double value = ((JSlider) e.getSource()).getValue();
				double seconds = value / 10;
				getPlayer().setMediaTime(seconds);
				double mediaTime = getPlayer().getMediaTime();

				startTime = mediaTime + getPlayer().getVideoOffset();
			}
		});
		sliderPanel.add(slider);
		panel.add(sliderPanel);

		JPanel btnPanel = new JPanel();
		btnPanel.setLayout(new BorderLayout());
		btnPanel.setBorder(BorderFactory.createEmptyBorder(1, 220, 1, 220));

		okButton = new JButton("Set");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ComboBoxItem selectedItem = (ComboBoxItem) comboBox.getSelectedItem();
				if (getStartTime() >= selectedItem.getVideoEvent().getEndTS()) {
					/*getGraphPanel().videoChunkPlotDataItem = false;
				} else {*/
					vcPlot.refreshPlot(getGraphPanel().getSubplotMap().get(ChartPlotOptions.VIDEO_CHUNKS).getPlot(), getGraphPanel().getTraceData(), getStartTime(),
							selectedItem.getVideoEvent());
				}
				dispose();

			}
		});
		btnPanel.add(okButton);
		panel.add(btnPanel);

		pack();
	}

	public GraphPanel getGraphPanel() {
		return this.parentPanel;
	}

	public int getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(double max) {
		int limit = (int) Math.ceil(max);
		this.maxValue = limit * 10;
	}

	public IVideoPlayer getPlayer() {
		return this.player;
	}

	public double getStartTime() {
		return this.startTime;
	}
}
