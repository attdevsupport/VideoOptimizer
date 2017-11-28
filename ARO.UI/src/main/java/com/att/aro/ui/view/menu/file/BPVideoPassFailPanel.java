package com.att.aro.ui.view.menu.file;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.ScrollPane;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;

/**
 * This dialog provides options to configure that the video best practices
 * should be gauged against
 * 
 * @author Dinesh
 * 
 */
public class BPVideoPassFailPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private JTable table;
	private ObjectMapper mapper;
	private static Component panel;
	private PreferenceHandlerImpl prefs;
	private VideoUsagePrefs videoUsagePrefs;
	private static BPVideoPassFailPanel instance;
	public static final Logger LOGGER = Logger.getLogger(BPVideoPassFailPanel.class.getName());
	
	private JTextField compileResultsField = new JTextField();
	StringBuilder sbError = new StringBuilder();
	
	public static synchronized Component getBPPanel() {
		panel = new ScrollPane();
		((ScrollPane) panel).add(getInstance());
		return panel;
	}

	public static synchronized BPVideoPassFailPanel getInstance() {
		if(instance==null){
			instance = new BPVideoPassFailPanel();
		}
		return instance;
	}
	
	public static synchronized void dropInstance() {
		instance = null;
		}

	private BPVideoPassFailPanel() {
		JPanel mainPanel = new JPanel();
		this.add(mainPanel);
		mainPanel.setLayout(new GridBagLayout());
		GridBagConstraints constraint = new GridBagConstraints();	
		mainPanel.add(getGridPanel(),constraint);
		compileResultsField.setEditable(false);
		if(sbError.toString().isEmpty()) {
			compileResultsField.setBackground(mainPanel.getBackground());
			compileResultsField.setForeground(Color.red);
			compileResultsField.setFont(compileResultsField.getFont().deriveFont(Font.BOLD));
			compileResultsField.setText("");
			compileResultsField.setVisible(false);
		} else {
			compileResultsField.setVisible(true);
			compileResultsField.setForeground(Color.red);
			compileResultsField.setText(String.format("ERRORS: %s" ,sbError.toString()));
		}
		constraint.anchor= GridBagConstraints.FIRST_LINE_START;
		constraint.gridy = 300;
		mainPanel.add(compileResultsField,constraint);
	}

	private Component getGridPanel() {

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(getTable());
		scrollPane.setPreferredSize(new Dimension(500, 70));
		return scrollPane;
	}

	VideoPreferenceTableModel model;

	private JTable getTable() {
		if (table == null) {
			model = new VideoPreferenceTableModel(loadPrefs());
			table = new JTable(model); 
			table.setGridColor(Color.LIGHT_GRAY);		
			table.setFocusable(false);
			table.setRowSelectionAllowed(false);
			table.setShowGrid(true);
			table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
			table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
			table.getColumnModel().getColumn(0).setPreferredWidth(225);
			table.getColumnModel().getColumn(1).setPreferredWidth(50);
			table.getColumnModel().getColumn(2).setPreferredWidth(50);
		
			table.getModel().addTableModelListener(new TableModelListener() {

				@Override
				public void tableChanged(TableModelEvent e) {
					sbError = model.getValidationError();
					if (!sbError.toString().isEmpty()) {
						compileResultsField.setVisible(true);
						compileResultsField.setForeground(Color.red);
						compileResultsField.setText(String.format("ERROR : %s", sbError.toString()));
					} else {
						compileResultsField.setText("");
						compileResultsField.setVisible(false);
					}
				}
			});

		} else {
			model.setData(loadPrefs());
			table.setModel(model);
		}
		
		return table;
	}

	private List<VideoPreferenceInfo> loadPrefs() {
		mapper = new ObjectMapper();
		prefs = PreferenceHandlerImpl.getInstance();

		String temp = prefs.getPref(VideoUsagePrefs.VIDEO_PREFERENCE);
		if (temp != null && !temp.isEmpty() && !temp.contains("null")) {
			try {
				videoUsagePrefs = mapper.readValue(temp, VideoUsagePrefs.class);
			} catch (IOException e) {
				LOGGER.error("VideoUsagePrefs failed to de-serialize :" + e.getMessage());
			}
		} else {
			try {
				videoUsagePrefs = new VideoUsagePrefs();
				temp = mapper.writeValueAsString(videoUsagePrefs);
				prefs.setPref(VideoUsagePrefs.VIDEO_PREFERENCE, temp);
			} catch (IOException e) {
				LOGGER.error("VideoUsagePrefs failed to serialize :" + e.getMessage());
			}
		}

		List<VideoPreferenceInfo> videoPreferenceList = new ArrayList<VideoPreferenceInfo>();
		VideoPreferenceInfo vp = new VideoPreferenceInfo("Startup Delay (seconds)", videoUsagePrefs.getStartUpDelayWarnVal(),
				videoUsagePrefs.getStartUpDelayFailVal());
		videoPreferenceList.add(vp);
		vp = new VideoPreferenceInfo("Stall Duration (seconds)", videoUsagePrefs.getStallDurationWarnVal(),
				videoUsagePrefs.getStallDurationFailVal());
		videoPreferenceList.add(vp);
		vp = new VideoPreferenceInfo("Segment Redundancy (%)", videoUsagePrefs.getSegmentRedundancyWarnVal(),
				videoUsagePrefs.getSegmentRedundancyFailVal());
		videoPreferenceList.add(vp);
		return videoPreferenceList;
	}	
	

	public void saveWarnFail() {
		PreferenceHandlerImpl prefs = PreferenceHandlerImpl.getInstance();
		if (model != null && model.getVideoUsagePrefs() != null) {
			String temp = "";
			try {
				temp = mapper.writeValueAsString(model.getVideoUsagePrefs());
				if (temp != null && !temp.equals("null")) {
					prefs.setPref(VideoUsagePrefs.VIDEO_PREFERENCE, temp);
				} else {
					LOGGER.error("Saving Video Preference failed : model data was null");
				}
			} catch (IOException e) {
				LOGGER.error("Saving Video Preference failed :" + e.getMessage());
			}
		}
	}
}