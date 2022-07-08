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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.ui.view.menu.file;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Position;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.VerticalLayout;

import com.att.aro.core.packetanalysis.pojo.TimeRange;
import com.att.aro.core.packetanalysis.pojo.TimeRange.TimeRangeType;
import com.att.aro.core.peripheral.pojo.TraceTimeRange;
import com.att.aro.core.util.Util;

import lombok.Getter;
import lombok.Setter;

public class TimeRangeChooser extends JPanel {
	private static final Logger LOG = LogManager.getLogger(TimeRangeChooser.class.getName());

	private static final long serialVersionUID = 1L;
	private TimeRangeEditorDialog timeRangeDialog;
	private TraceTimeRange traceTimeRange;

	private static final String DEFAULT_MARK = "*";
	
	private int currentItemCount;
	private JList<TimeRangeItem> timeRangeJList;
	private List<TimeRangeItem> rangeItemList = new ArrayList<>();
	private DefaultListModel<TimeRangeItem> timeRangeListModel;
	
	private int CHOOSER_WIDTH = 100;

	private int heightBoost;

	@Getter @Setter private TimeRange timeRangeObj;

	private int selectedEntry = 0;
	private Integer cashedEntry;

	private JPanel pickerPane;

	private JScrollPane timeRangeScrollPane;

	private JLabel userHint;
	
	public TimeRangeChooser(TimeRangeEditorDialog timeRangeDialog, TraceTimeRange traceTimeRange) {
		this.timeRangeDialog = timeRangeDialog;
		this.traceTimeRange = traceTimeRange;
		setLayout(new VerticalLayout());
		
		userHint = new JLabel("* Denotes the default time range");
		userHint.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 0));
		userHint.setFont(new Font("Dialog", Font.BOLD, 14));
		userHint.setVisible(timeRangeDialog.countEntriesWithAutoLaunch("") > 0);

		JLabel label = new JLabel("Choose a line:");
		label.setFont(new Font("Dialog", Font.PLAIN, 18));
		label.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 0));
		add(label);
		add(createContentPanel());
		addComponentListener(new ComponentListener() {
			
			private String currentTimeRangeObj;

			@Override public void componentResized(ComponentEvent e) {}
			@Override public void componentMoved(ComponentEvent e) {}
			
			@Override
			public void componentShown(ComponentEvent e) {
				if (timeRangeJList != null && (currentItemCount != traceTimeRange.getTimeRangeList().size()
						|| !currentTimeRangeObj.equals(timeRangeObj.toString()))) {

					pickerPane.remove(0);
					pickerPane.add(createListPane());
					userHint.setVisible(timeRangeDialog.countEntriesWithAutoLaunch("") > 0);
				}
			}
			
			@Override
			public void componentHidden(ComponentEvent e) {
				currentItemCount = traceTimeRange.getTimeRangeList().size();
				currentTimeRangeObj = timeRangeObj.toString();
				
			}
		});
	}

	private Component createContentPanel() {
		JPanel contentPane = new JPanel(new VerticalLayout());
		contentPane.setPreferredSize(new Dimension(CHOOSER_WIDTH + 100, 400 + heightBoost));
		contentPane.add((pickerPane = createPickerPane()));
		contentPane.add(createButtonPane());
		return contentPane;
	}

	private JPanel createPickerPane() {
		JPanel pane = new JPanel(new VerticalLayout());
		pane.add(createListPane());
		return pane;
	}

	private JPanel createListPane() {
		JPanel listPane = new JPanel(new BorderLayout(5, 0));
		listPane.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
		listPane.setPreferredSize(new Dimension(100, (Util.isMacOS() ? 293:270) - heightBoost));
		
		populateTimeRangeJList();
		timeRangeScrollPane = new JScrollPane(timeRangeJList);
		listPane.add(timeRangeScrollPane);
		return listPane;
	}

	private void populateTimeRangeJList() {
		timeRangeListModel = new DefaultListModel<>();
		for (TimeRangeItem item : populateRangeItemList()) {
			timeRangeListModel.addElement(item);
		}
		timeRangeJList = new JList<>(timeRangeListModel);
		timeRangeJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		timeRangeJList.setFont(new Font(Font.MONOSPACED, Util.isMacOS() ? Font.PLAIN : Font.BOLD, 22));
		timeRangeJList.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					timeRangeDialog.analyze(getTimeRangeObj());
					destroy();
				}
			}
			@Override public void mouseReleased(MouseEvent e) {}
			@Override public void mousePressed(MouseEvent e) {}
			@Override public void mouseExited(MouseEvent e) {}
			@Override public void mouseEntered(MouseEvent e) {}
		});
		timeRangeJList.addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				selectedEntry = timeRangeJList.getSelectedValue().row;
				setTimeRangeObj(timeRangeJList.getModel().getElementAt(selectedEntry).getTimeRangeObj());
			}
		});
		if (cashedEntry != null) {
			selectedEntry = cashedEntry;
		} else if (timeRangeJList.getComponentCount() > 0 && timeRangeDialog.countEntriesWithAutoLaunch("") > 0) {
			int temp;
			if ((temp = timeRangeJList.getNextMatch(DEFAULT_MARK, 0, Position.Bias.Forward)) > -1) {
				selectedEntry = temp;
			}
		}
		timeRangeJList.setSelectedIndex(selectedEntry);
	}
	
	@Getter @Setter public class TimeRangeItem {

		private String value;
		private TimeRange timeRangeObj;
		private boolean selected;
		private int row;

		TimeRangeItem(TimeRange timeRangeObj, int row) {
			StringBuffer sb = new StringBuffer();
			sb.append(timeRangeObj.getTimeRangeType().equals(TimeRangeType.DEFAULT) ?  DEFAULT_MARK : " ");
			sb.append(" ");
			sb.append(StringUtils.rightPad(timeRangeObj.getTitle(), 40));
			sb.append(String.format("% 8.03f  -% 8.03f"
					, timeRangeObj.getBeginTime(), timeRangeObj.getEndTime()));

			this.value = sb.toString();
			this.row = row;
			this.timeRangeObj = timeRangeObj;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	// button panel & ActionListener	
	private Component createButtonPane() {
		JPanel btnPane = new JPanel(new BorderLayout());
		btnPane.setPreferredSize(new Dimension(60, Util.getAdjustedHeight(50)));

		JPanel btnPanel = new JPanel(new FlowLayout());
		// build the 'Save' JButton
		btnPanel.add(buildButton("Add", "Create a new entry"));
		btnPanel.add(buildButton("Edit", "Edit the selected entry"));
		btnPanel.add(buildButton("Delete", "Will delete the selected entry"));
		btnPanel.add(buildButton("Launch",
				"Will Analyze based on selected range, otherwise, If no selecton will analyze the whole trace"));
		btnPanel.add(buildButton("Cancel", "Will not analyze"));

		btnPane.add(userHint, BorderLayout.WEST);

		btnPane.add(btnPanel, BorderLayout.EAST);
		return btnPane;
	}

	private JButton buildButton(String buttonName, String tip) {
		JButton button = new JButton(buttonName);
		button.setFont(new Font("Dialog", Font.BOLD, 14));
		button.setPreferredSize(new Dimension(CHOOSER_WIDTH, Util.getAdjustedHeight(30)));
		button.addActionListener(createButtonListener());
		if (!StringUtils.isEmpty(tip)) {
			button.setToolTipText(tip);
		}
		return button;
	}

	public ActionListener createButtonListener() {
		return new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				String button = e.getActionCommand();
				cashedEntry = selectedEntry;
				
				switch (button) {
				case "Add":
					setVisible(false);
					timeRangeDialog.createEntry();
					break;
				case "Cancel":
					timeRangeDialog.cancel();
					destroy();
					break;
				case "Launch":
					if (getTimeRangeObj() != null) {
						if (timeRangeObj.getEndTime() - timeRangeObj.getBeginTime() > 0) {
							setVisible(false);
							LOG.debug("Launch :" + getTimeRangeObj());
							timeRangeDialog.analyze(getTimeRangeObj());
							destroy();
						} else {
							LOG.debug("Failed attempt to Launch :" + getTimeRangeObj());
							timeRangeDialog.userErrorMessage("Invalid Time range, please edit and save");
							setVisible(false);
							LOG.debug("Send user back to the editor with:" + getTimeRangeObj());
							timeRangeDialog.edit(getTimeRangeObj());
						}
					} else {
						errorMustChooseLine();
					}
					break;
				case "Edit":
					if (getTimeRangeObj() != null) {
						setVisible(false);
						timeRangeDialog.edit(getTimeRangeObj());
					} else {
						errorMustChooseLine();
					}
					break;
				case "Delete":
					if (getTimeRangeObj() != null) {
						setVisible(false);
						LOG.debug("Delete :" + getTimeRangeObj());
						timeRangeDialog.delete(getTimeRangeObj());
					} else {
						errorMustChooseLine();
					}
					break;

				default:
					errorMustChooseLine();
					break;
				}
			}

			public void errorMustChooseLine() {
				timeRangeDialog.userErrorMessage("You must select a line first!");
			}
		};
	}
	
	protected void destroy() {
		timeRangeDialog.destroy();
	}

	private List<TimeRangeItem> populateRangeItemList() {
		rangeItemList.clear();
		int row = 0;
		if (traceTimeRange != null) {
			for (TimeRange timeRange : traceTimeRange.getTimeRangeList()) {
				rangeItemList.add(new TimeRangeItem(timeRange, row++));
			}
		}
		return rangeItemList;
	}
	
}
