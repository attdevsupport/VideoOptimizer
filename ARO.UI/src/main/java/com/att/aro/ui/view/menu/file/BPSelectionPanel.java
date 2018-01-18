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
package com.att.aro.ui.view.menu.file;

import static com.att.aro.core.settings.SettingsUtil.retrieveBestPractices;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.ScrollPane;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.TitledBorder;

import org.apache.log4j.Logger;

import com.att.aro.core.bestpractice.pojo.BestPracticeType;
import com.att.aro.core.bestpractice.pojo.BestPracticeType.Category;;

/**
 * This provides a panel with all the best practices
 * 
 * @author bharath
 *
 */
public class BPSelectionPanel extends JPanel {
	public static final Logger LOGGER = Logger.getLogger(BPSelectionPanel.class.getName());
	private static final long serialVersionUID = 1L;
	private static Component panel;
	private static BPSelectionPanel instance;
	int noOfBestPractices = 0;
	

	Map<String, JCheckBox> map = new HashMap<>();
	List<JCheckBox> selectAllList = new ArrayList<>();

	public static synchronized Component getBPPanel() {
		if (panel == null) {
			panel = new ScrollPane();
			((ScrollPane) panel).add(getInstance());
		} else {
			instance.setSelectedBoxes();
			instance.clearSelectAllBtns();
		}
		return panel;
	}

	public static synchronized BPSelectionPanel getInstance() {
		if (instance == null) {
			instance = new BPSelectionPanel();
		}
		return instance;
	}

	private BPSelectionPanel() {
		JSplitPane bottomSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, getLine1(), getLine2());
		bottomSplitPane.setDividerLocation(0.5);
		this.add(bottomSplitPane);

		setSelectedBoxes();
	}

	private JPanel getLine2() {
		noOfBestPractices = 0;
		JPanel line2 = new JPanel();
		line2.add(getGroupedBPPanel(Category.HTML), BorderLayout.NORTH);
		line2.add(getGroupedBPPanel(Category.SECURITY), BorderLayout.NORTH);
		line2.add(getGroupedBPPanel(Category.VIDEO), BorderLayout.NORTH);
		line2.add(getGroupedBPPanel(Category.OTHER), BorderLayout.NORTH);
		line2.setPreferredSize(new Dimension(350,90 + 23 * noOfBestPractices));
		return line2;
	}

	private JPanel getLine1() {
		noOfBestPractices = 0;
		JPanel line1 = new JPanel();
		line1.add(getGroupedBPPanel(Category.FILE), BorderLayout.NORTH);
		line1.add(getGroupedBPPanel(Category.CONNECTIONS), BorderLayout.NORTH);
		line1.setPreferredSize(new Dimension(350,205 + 23 * noOfBestPractices));
		return line1;
	}

	private Component getGroupedBPPanel(Category category) {
		List<BestPracticeType> bpItems = BestPracticeType.getByCategory(category);
		JPanel panel = new JPanel();
		noOfBestPractices = noOfBestPractices+bpItems.size();
		Dimension sectionDimention = new Dimension(350, 45 + 23 * bpItems.size());
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.setPreferredSize(sectionDimention);
		panel.setMinimumSize(sectionDimention);
		panel.setMaximumSize(sectionDimention);
		final JCheckBox toggleAll = new JCheckBox("Select All");
		toggleAll.setForeground(Color.BLUE);
		toggleAll.addActionListener((e) -> setSelectedBoxes(bpItems, toggleAll.isSelected()));
		panel.add(toggleAll);
		selectAllList.add(toggleAll);
		for (BestPracticeType item : bpItems) {
			JCheckBox checkBox = new JCheckBox(item.getDescription());
			panel.add(checkBox);
			map.put(item.name(), checkBox);
		}
		TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(),
				category.getDescription());
		border.setTitleColor(Color.BLUE);
		panel.setBorder(border);
		return panel;
	}

	public List<BestPracticeType> getCheckedBP() {
		List<BestPracticeType> selected = new ArrayList<>();
		for (Entry<String, JCheckBox> entry : map.entrySet()) {
			if (entry.getValue().isSelected()) {
				selected.add(BestPracticeType.valueOf(entry.getKey()));
			}
		}
		return selected;
	}

	private void clearSelectAllBtns() {
		selectAllList.stream().forEach((a) -> a.setSelected(false));
	}
	
	private void setSelectedBoxes() {
		List<BestPracticeType> bpList = retrieveBestPractices();
		for (JCheckBox val : map.values()) {
			val.setSelected(false);
		}
		setSelectedBoxes(bpList, true);
	}

	private void setSelectedBoxes(List<BestPracticeType> selected, boolean state) {
		for (BestPracticeType type : selected) {
			map.get(type.name()).setSelected(state);
		}
	}
}