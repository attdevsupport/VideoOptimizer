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
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.att.aro.core.bestpractice.pojo.BestPracticeType;
import com.att.aro.core.bestpractice.pojo.BestPracticeType.Category;
import com.att.aro.core.settings.SettingsUtil;;

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
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		JPanel line1 = new JPanel();
		JPanel line2 = new JPanel();
		this.add(line1);
		this.add(line2);
		line1.setLayout(new BoxLayout(line1, BoxLayout.PAGE_AXIS));
		line2.setLayout(new BoxLayout(line2, BoxLayout.PAGE_AXIS));
		line1.add(getGroupedBPPanel(Category.VIDEO), BorderLayout.NORTH);
		line1.add(getGroupedBPPanel(Category.FILE), BorderLayout.NORTH);
		line1.add(getGroupedBPPanel(Category.OTHER), BorderLayout.NORTH);
		line2.add(getGroupedBPPanel(Category.HTML), BorderLayout.NORTH);
		line2.add(getGroupedBPPanel(Category.CONNECTIONS), BorderLayout.NORTH);
		line2.add(getGroupedBPPanel(Category.SECURITY), BorderLayout.NORTH);
		setSelectedBoxes();
	}

	private Component getGroupedBPPanel(Category category) {
		// TODO remember to change  allBpItems to bpItems when adding VIDEO_CONCURRENT_SESSION to BPTab
		List<BestPracticeType> allBpItems = BestPracticeType.getByCategory(category);
//		List<BestPracticeType> bpItems = BestPracticeType.getByCategory(category); // original

		// TODO remember to remove this when added to BPTab 
		List<BestPracticeType> bpItems = allBpItems.stream().filter((i) -> (i != BestPracticeType.VIDEO_CONCURRENT_SESSION)).collect(Collectors.toList());
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.setPreferredSize(new Dimension(350, 45 + 23 * bpItems.size()));
		panel.setMinimumSize(new Dimension(350, 45 + 23 * bpItems.size()));
		panel.setMaximumSize(new Dimension(350, 45 + 23 * bpItems.size()));
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
		//TODO REMOVE THIS BLOCK AFTER 1.2 RELEASE
		List<BestPracticeType> selectableVBP = BestPracticeType.getByCategory(BestPracticeType.Category.VIDEO);
		selectableVBP.remove(BestPracticeType.VIDEO_CONCURRENT_SESSION);
		if (CollectionUtils.containsAny(selected, selectableVBP)) {
			selected.add(BestPracticeType.VIDEO_CONCURRENT_SESSION);
		} else {
			selected.remove(BestPracticeType.VIDEO_CONCURRENT_SESSION);
		}
		//END OF HACK
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
			// TODO remember to remove this when added to BPTab 
			if(type == BestPracticeType.VIDEO_CONCURRENT_SESSION) continue;
			map.get(type.name()).setSelected(state);
		}
	}
}