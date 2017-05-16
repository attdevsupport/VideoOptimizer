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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.apache.log4j.Logger;

import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.view.diagnostictab.plot.VideoChunksPlot;
import com.att.aro.ui.view.video.IVideoPlayer;

public class SliderDialogBox extends JDialog {
	private static final Logger LOGGER = Logger.getLogger(SliderDialogBox.class.getName()); 

	private static final long serialVersionUID = 1L;
	private JSlider slider;
	private JButton okButton;
	private int maxValue;
	private IVideoPlayer player;
	private double startTime;
	private VideoChunksPlot vcPlot;

	private GraphPanel parentPanel;
	private BufferedImage resizedThumbnail;
	private Map<Integer, VideoEvent> chunkSelectionList = new TreeMap<>();

	DefaultListModel<JTableItems> listModel = new DefaultListModel<JTableItems>();
	List<VideoEvent> selectedSegmentsList = new ArrayList<>();

	private JTable jTable;
	private List<JTableItems> listSegments = new ArrayList<>();
	private List<VideoEvent> allChunks = new ArrayList<>();
	public static List<JTableItems> segmentListChosen; // = new ArrayList<>();

	private int selectedIndex = 0;

	private JButton plusTunerBtn;
	private JButton minusTunerBtn;

	private JPanel panel;

	JLabel imgLabel = new JLabel();

	private static class JTableItems {
		private String value;
		private VideoEvent videoEvent;
		private boolean isSelected;

		JTableItems(VideoEvent videoEvent) {
			StringBuffer sb = new StringBuffer();
			sb.append("Segment: ").append(videoEvent.getSegment()).append(", ");
			sb.append("Quality: ").append(videoEvent.getQuality()).append(", ");
			sb.append("DL Start Time: ").append(String.format("%.2f", videoEvent.getStartTS()));

			this.value = sb.toString();
			this.videoEvent = videoEvent;
		}

		public void setSelected(boolean isSelected) {
			this.isSelected = isSelected;
		}

		public VideoEvent getVideoEvent() {
			return videoEvent;
		}

		@Override
		public String toString() {
			return value;
		}
	}
	

	public SliderDialogBox(GraphPanel parentPanel, double maxVideoTime, Map<Integer, VideoEvent> chunks, int indexKey,
			List<VideoEvent> allChunks) {
		this.parentPanel = parentPanel;
		DiagnosticsTab parent = parentPanel.getGraphPanelParent();
		player = parent.getVideoPlayer();
		vcPlot = parentPanel.getVcPlot();
		chunkSelectionList = chunks;
	
		this.allChunks.addAll(allChunks);
		createSliderDialog(maxVideoTime, player, vcPlot, indexKey);
		
		jTable.setRowSelectionInterval(selectedIndex, selectedIndex);
		jTable.scrollRectToVisible(new Rectangle(jTable.getCellRect(selectedIndex, 0, true)));

	}

	public void makeSelection(int indexKey) {
		VideoEvent segment = chunkSelectionList.get(indexKey);
		for (int index=0;index<listSegments.size();index++) {
			if(listSegments.get(index).getVideoEvent().equals(segment)){
				selectedIndex = index;
				break;
			}
		}
	}
	
	public void populateList(){
		for (VideoEvent ve : allChunks) {
			   boolean added = false;
				for(JTableItems item: segmentListChosen){
					if(ve.equals(item.getVideoEvent())){
						listSegments.add(item);
						added = true;
					}
				}
				if(added == false){
					listSegments.add(new JTableItems(ve));
				}
		}
	}

	@SuppressWarnings("serial")
	public void createSliderDialog(double max, IVideoPlayer player, final VideoChunksPlot vcPlot, int indexKey) {

		setUndecorated(false);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setBounds(300, 200, 1000, 1000);

		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		add(panel);

		JPanel labelPanel = new JPanel(new BorderLayout());
		labelPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		labelPanel.add(new JLabel("Position slider to video start up time"));
		panel.add(labelPanel);

		JPanel comboPanel = new JPanel(new BorderLayout());
		comboPanel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 250));
		comboPanel.setSize(500, 100);
		GridBagConstraints constraint = new GridBagConstraints();	
		
		listSegments.clear();
		populateList();
		
		Collections.sort(listSegments, new Comparator<JTableItems>() {
			@Override
			public int compare(JTableItems o1, JTableItems o2) {
				// TODO Auto-generated method stub
				if (o1.getVideoEvent().getSegment() < o2.getVideoEvent().getSegment()) {
					return -1;
				} else if (o1.getVideoEvent().getSegment() > o2.getVideoEvent().getSegment()) {
					return 1;
				} else {
					return 0;
				}
			}

		});

		
		//making the correct selection of JTable row based on user input --> Then scroll JTable to selected row
		makeSelection(indexKey);

		//JTable model having two columns
		DefaultTableModel tableModel = new DefaultTableModel(listSegments.size(), 2);

		//JTable Renderer listeners 
		jTable = new JTable(tableModel) {
			@Override
			public TableCellRenderer getCellRenderer(int row, int column) {
				if (getValueAt(row, column) instanceof Boolean) {
					JTableItems itemObject = listSegments.get(row);
					boolean checkBoxStatus =  (boolean) getValueAt(row, column);
					itemObject.setSelected(checkBoxStatus);
					return super.getDefaultRenderer(Boolean.class);
				} else {
					return super.getCellRenderer(row, column);
				}
			}

			@Override
			public TableCellEditor getCellEditor(int row, int column) {
				if (getValueAt(row, column) instanceof Boolean) {
					return super.getDefaultEditor(Boolean.class);
				} else {
					return super.getCellEditor(row, column);
				}
			}

		};

		jTable.getColumnModel().getColumn(0).setMaxWidth(30);
		jTable.setTableHeader(null);
		
		//Setting JTable model with data values
		for (int i = 0; i < listSegments.size(); i++) {
			JTableItems item = listSegments.get(i);
			jTable.getModel().setValueAt(item.isSelected, i, 0);
			jTable.getModel().setValueAt(item.value, i, 1);
		}

		//Mouse click listener for check boxes which are on column 0 of the JTable
		jTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
	             int row = jTable.getSelectedRow();
	             int column = jTable.getSelectedColumn();
	             JTableItems selectedChunk = listSegments.get(row);
	             
	              if (column ==0 && (!selectedChunk.isSelected)) { // add the selected segment
						boolean found = false;
						for(JTableItems segment: segmentListChosen){
							if(segment.getVideoEvent().equals(selectedChunk.getVideoEvent())){			
								found = true;
								break;
							}
						}
						if(found==false){
							segmentListChosen.add(selectedChunk);
						}

					} else if(column ==0){// remove the selected segment
						boolean found = false;
						for(JTableItems segment: segmentListChosen){
							if(segment.getVideoEvent().equals(selectedChunk.getVideoEvent())){	
								found = true;
								break;
							}
						}
						if(found==true){
							segmentListChosen.remove(selectedChunk);
						}
					}
			}
		});
		
		jTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				// TODO Auto-generated method stub
				int index = jTable.getSelectedRow();
				JTableItems itemObject = listSegments.get(index);
				resizedThumbnail = itemObject.getVideoEvent().getImageOriginal();
			
				ImageIcon img = new ImageIcon(resizedThumbnail);// thumbnail);
				imgLabel.setIcon(img);// = new JLabel(img);
				int dlTimestamp = (int) itemObject.getVideoEvent().getStartTS();

				slider.setValue(dlTimestamp * 25);

				panel.setSize(panel.getPreferredSize());
				panel.revalidate();

				JDialog parentDialog = (JDialog) panel.getRootPane().getParent();
				parentDialog.setSize(parentDialog.getPreferredSize());
				parentDialog.revalidate();	
			}
		});

		
		JScrollPane listScrollPanel = new JScrollPane(jTable); // listComponent);
		listScrollPanel.setPreferredSize(new Dimension(500, 100));
		comboPanel.add(listScrollPanel); // comboBox); //,constraint);

		panel.add(comboPanel); // comboBoxPanel);

		JPanel imgLabelPanel = new JPanel(new BorderLayout());
		imgLabelPanel.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 1));
		imgLabelPanel.add(imgLabel);
		imgLabelPanel.setSize(imgLabelPanel.getPreferredSize());

		panel.add(imgLabelPanel);

		JPanel sliderBoxPanel = new JPanel(new BorderLayout());
		sliderBoxPanel.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 1));

		JPanel sliderPanel = new JPanel();
		sliderPanel.setLayout(new GridBagLayout());

		constraint = new GridBagConstraints();
		constraint.fill = GridBagConstraints.HORIZONTAL;
		constraint.gridx = 0;
		constraint.gridy = 0;
		constraint.weightx = 2;

		setMaxValue(max);
		this.player = player;
		slider = new JSlider(JSlider.HORIZONTAL, 0, getMaxValue(), 0);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);

		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				double value = ((JSlider) e.getSource()).getValue();
				// double seconds = value / 10;
				double seconds = value * 0.04; /// 100;
				getPlayer().setMediaTime(seconds);
				double mediaTime = getPlayer().getMediaTime();

				startTime = mediaTime + getPlayer().getVideoOffset();
			}
		});
	
		sliderPanel.add(slider, constraint);

		minusTunerBtn = getTunerButton("-");
		constraint.fill = GridBagConstraints.HORIZONTAL;
		constraint.gridx = 1;
		constraint.gridy = 0;
		constraint.insets = new Insets(0, 0, 0, 1);
		constraint.weightx = 0.2;

		sliderPanel.add(minusTunerBtn, constraint);
		
		plusTunerBtn = getTunerButton("+");
		constraint.fill = GridBagConstraints.HORIZONTAL;
		constraint.gridx = 2;
		constraint.gridy = 0;
		constraint.insets = new Insets(0, 0, 0, 2);
		constraint.weightx = 0.2;

		sliderPanel.add(plusTunerBtn, constraint);

		sliderBoxPanel.add(sliderPanel);
		panel.add(sliderBoxPanel);

		JPanel btnPanel = new JPanel();
		btnPanel.setLayout(new BorderLayout());
		btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 220, 1, 220));

		okButton = new JButton("Set");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTableItems selectedItem = (JTableItems) listSegments.get(jTable.getSelectedRow()); // comboBox.getSelectedItem();
				try {
					if (getStartTime() >= selectedItem.getVideoEvent().getEndTS() && selectedItem.isSelected) {
						vcPlot.refreshPlot(getGraphPanel().getSubplotMap().get(ChartPlotOptions.VIDEO_CHUNKS).getPlot(),
								getGraphPanel().getTraceData(), getStartTime(), selectedItem.getVideoEvent());
					}
				} catch (Exception ex) {
					LOGGER.error("Error generating video chunk and buffer plots", ex);
					MessageDialogFactory.showMessageDialog(parentPanel, "Error in drawing buffer graphs",
							"Failed to generate buffer plots", JOptionPane.ERROR_MESSAGE);
				}
				dispose();

			}
		});
		btnPanel.add(okButton);
		panel.add(btnPanel);

		// pack();
		panel.setSize(panel.getPreferredSize());
		panel.validate();

	}

	public GraphPanel getGraphPanel() {
		return this.parentPanel;
	}

	public int getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(double max) {
		int limit = (int) Math.ceil(max);
		// this.maxValue = limit * 10;
		this.maxValue = limit * 25;// 100;
	}

	public IVideoPlayer getPlayer() {
		return this.player;
	}

	public double getStartTime() {
		return this.startTime;
	}
	
	public JButton getTunerButton(String btnName) {
		JButton tunerBtn = new JButton(btnName);
		tunerBtn.setPreferredSize(new Dimension(5, 15));

		tunerBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JButton button = (JButton) e.getSource();
				if (button.getText().trim().equals("+")) {
					int sliderValue = (slider.getValue());
					sliderValue = (sliderValue + 1);
					slider.setValue(sliderValue);
				} else {
					int sliderValue = (slider.getValue());
					sliderValue = (sliderValue - 1);
					slider.setValue(sliderValue);
				}

			}
		});
		return tunerBtn;
	}
}
