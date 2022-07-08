/*
 *  Copyright 2021 AT&T
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
package com.att.aro.ui.view.menu.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.tracemetadata.impl.MetaDataHelper;
import com.att.aro.core.tracemetadata.pojo.MetaDataModel;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.AroFonts;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.SharedAttributesProcesses;

import lombok.Getter;

public class MetadataDialog extends JDialog {

	private static final int DEFAULT_WIDTH = 956;

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LogManager.getLogger(MetadataDialog.class);

	private MetaDataHelper metadataHelper = ContextAware.getAROConfigContext().getBean(MetaDataHelper.class);
	@Getter
	private MetaDataModel metaDataModel;

	private JButton okButton;
	private JPanel buttonPanel;
	private JPanel metadataPanel;
	private JPanel notesPanel;
	private JPanel jButtonGrid;
	private JPanel jContentPane;
	private final JMenuItem callerMenuItem;
	private final SharedAttributesProcesses parent;
		
	private Label traceStorageLabel = new Label();
	private Label descriptionLabel = new Label();
	private Label traceTypeLabel = new Label();
	private Label targetedAppLabel = new Label();
	private Label applicationProducerLabel = new Label();
	private Label targetAppVerLabel = new Label();
	private Label simLabel = new Label();
	private Label networkLabel = new Label();
	private Label traceOwnerLabel = new Label();

	private JTextField traceStorageField = new JTextField();
	private JTextField descriptionField = new JTextField();
	private JTextField traceTypeField = new JTextField();
	private JTextField targetedAppField = new JTextField();
	private JTextField applicationProducerField = new JTextField();
	private JTextField targetAppVerField = new JTextField();
	private JTextField simField = new JTextField();
	private JTextField networkField = new JTextField();	
	private JTextField traceOwnerField = new JTextField();
	private JTextArea traceNotes = new JTextArea();
	private int traceNotesLineCount =  0;
	
	private String tracePath = "";
	private AbstractTraceResult result;

	private JScrollPane notesScrollPane;

	private JPanel notesScrollPanel;

	@Getter private Point traceNotesCaretPoint;
	@Getter private int traceNotesCaretPosition;
	
	@Getter
	private boolean cancelled = false;

	private String buTraceNotes;

	/**
	 * 
	 * @param i null value indicates leave CaretPosition to default
	 * @param parent
	 * @param callerMenuItem
	 */
	public MetadataDialog(SharedAttributesProcesses parent, JMenuItem callerMenuItem, Integer caretPosition) {
		super(parent.getFrame());
		this.parent = parent;
		this.callerMenuItem = callerMenuItem;
		if (callerMenuItem != null) {
			callerMenuItem.setEnabled(false);
		}
		try {
			extractMetaDataModel();
			init();
			if (caretPosition != null) {
				traceNotesCaretPosition = caretPosition;
				traceNotes.setCaretPosition(traceNotesCaretPosition);
			}
			buTraceNotes = metaDataModel.getTraceNotes();
			traceNotesLineCount = traceNotes.getLineCount();
			setMinimumSize(getPreferredSize());
			this.setVisible(true);
		} catch (Exception e) {
			LOGGER.error("Exception while reading metadata json", e);
			dispose();
		}
	}

	private void extractMetaDataModel() throws Exception {
		result = getAbstractTraceResult();
		tracePath = getTracePath();
		if (result != null && result.getMetaData() != null) {
			metaDataModel = result.getMetaData();
		} else {
			metaDataModel = metadataHelper.loadMetaData(tracePath);
		}
	}

	private void init() {
		this.setContentPane(getJContentPane());
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setTitle("Metadata");
		this.setModal(true);
		this.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowStateChanged(WindowEvent event) {
				pack();
				validate();
			}
			
			@Override
			public void windowClosing(WindowEvent e) {
				cancelDialog();
			}
		});
		
		adjustDimensions();
		
		revalidate();
		pack();
		setLocationRelativeTo(parent.getFrame());
		getRootPane().setDefaultButton(okButton);
	}

	private void adjustDimensions() {
		int longestRow = 0;
		int width = DEFAULT_WIDTH;

		FontMetrics fm = traceNotes.getFontMetrics(AroFonts.TEXT_FONT);
		int columnCount = 0;
		String[] rows = traceNotes.getText().split("\n");
		int textAreaRows = 1;
		if (rows.length > 0) {
			for (int row = 0; row < rows.length; row++) {
				if (columnCount < rows[row].length()) {
					columnCount = rows[row].length();
					longestRow = row;
				}
			}
			textAreaRows = StringUtils.countMatches(traceNotes.getText(), "\n");
			width = SwingUtilities.computeStringWidth(fm, rows[longestRow]) + 240;
			if (width < DEFAULT_WIDTH) {
				width = DEFAULT_WIDTH;
			}
		}
		
		int fontHeight = fm.getHeight() + (Util.isWindowsOS() ? 1 : 0);
		int height = textAreaRows * fontHeight + fontHeight;

		notesScrollPane.setPreferredSize(new Dimension(960, 400));
		notesPanel.setPreferredSize(new Dimension(width * 2, height));

		notesScrollPane.setMinimumSize(notesScrollPane.getPreferredSize());
		notesPanel.setMinimumSize(notesPanel.getPreferredSize());
		
		traceNotesLineCount = traceNotes.getLineCount();
	}
	
	private JComponent getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel(new GridBagLayout());
			jContentPane.add(getMetadataPanel(), new GridBagConstraints(0, 0, 1, 1, 0.2, 0.2, GridBagConstraints.WEST,  GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
			jContentPane.add(getNotesScrollPane(), new GridBagConstraints(0, 1, 1, 1, 1, 5, GridBagConstraints.NORTHWEST,  GridBagConstraints.NONE, new Insets(0, 20, 0, 20), 0, 0));
			jContentPane.add(getButtonPanel(), new GridBagConstraints(0, 2, 1, 1, 1, 1, GridBagConstraints.EAST,  GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		}
		return jContentPane;
	}

	private JPanel getMetadataPanel() {

		if (metadataPanel == null) {

			int index = 0;

			metadataPanel = new JPanel(new GridBagLayout());

			traceStorageField 			= getValueTextField("setTraceStorage"		, metaDataModel.getTraceStorage(), 70, AroFonts.TEXT_FONT);
			descriptionField 			= getValueTextField("setDescription"		, metaDataModel.getDescription(), 70, AroFonts.TEXT_FONT);
			traceTypeField 				= getValueTextField("setTraceType"			, metaDataModel.getTraceType(), 20, AroFonts.TEXT_FONT);
			targetedAppField 			= getValueTextField("setTargetedApp"		, metaDataModel.getTargetedApp(), 20, AroFonts.TEXT_FONT);
			applicationProducerField 	= getValueTextField("setApplicationProducer", metaDataModel.getApplicationProducer(), 20, AroFonts.TEXT_FONT);
			targetAppVerField 			= getValueTextField("setTargetAppVer"		, metaDataModel.getTargetAppVer(), 20, AroFonts.TEXT_FONT);
			simField		 			= getValueTextField("setSim"				, metaDataModel.getSim(), 20, AroFonts.TEXT_FONT);
			networkField	 			= getValueTextField("setNetWork"			, metaDataModel.getNetWork(), 20, AroFonts.TEXT_FONT);
			traceOwnerField	 			= getValueTextField("setTraceOwner"			, metaDataModel.getTraceOwner(), 20, AroFonts.TEXT_FONT);

			index = addLine(traceStorageLabel		,"bestPractices.mdata.traceStorage"		,  traceStorageField		, index, metadataPanel);
			index = addLine(descriptionLabel		,"metadata.field.description"			,  descriptionField			, index, metadataPanel);
			index = addLine(traceTypeLabel			,"metadata.field.traceType"				,  traceTypeField			, index, metadataPanel);
			index = addLine(targetedAppLabel		,"metadata.field.targetedApp"			,  targetedAppField			, index, metadataPanel);
			index = addLine(applicationProducerLabel,"metadata.field.applicationProducer"	,  applicationProducerField	, index, metadataPanel);
			index = addLine(targetAppVerLabel		,"metadata.field.targetAppVer"			,  targetAppVerField		, index, metadataPanel);
			index = addLine(simLabel 				,"metadata.field.sim"					,  simField		 			, index, metadataPanel);
			index = addLine(networkLabel			,"metadata.field.netWork"				,  networkField	 			, index, metadataPanel);			
			index = addLine(traceOwnerLabel			,"bestPractices.mdata.traceOwner"		,  traceOwnerField	 		, index, metadataPanel);
		}
		metadataPanel.setBorder(BorderFactory.createEmptyBorder(8, 80, 10, 10));
		metadataPanel.setPreferredSize(new Dimension(1000, 280));
		metadataPanel.setMinimumSize(new Dimension(1000, 280));
		return metadataPanel;
	}
	
	private JPanel getNotesScrollPane() {

		if (notesScrollPanel == null) {
			
			notesPanel = getNotesPanel();
			notesScrollPane = new JScrollPane(notesPanel);
			notesScrollPane.setMinimumSize(notesPanel.getPreferredSize());
			notesScrollPane.setPreferredSize(notesPanel.getPreferredSize());
			
			notesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			notesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			notesScrollPanel = new JPanel(new BorderLayout());
			JLabel title = new JLabel("Trace Notes");
			title.setFont(AroFonts.SUBHEADER_FONT);
			notesScrollPanel.add(title, BorderLayout.WEST);
			notesScrollPanel.add(notesScrollPane, BorderLayout.SOUTH);

			// make sure scroll is at the top
			traceNotes.setCaretPosition(0);
			return notesScrollPanel;
		}

		return notesScrollPanel;
	}
	
	private JPanel getNotesPanel() {

		if (notesPanel == null) {
			notesPanel = new JPanel(new BorderLayout());
			traceNotes = getValueTextArea("setTraceNotes", metaDataModel.getTraceNotes(), 70, AroFonts.MONOSPACED);
			traceNotes.setFont(AroFonts.MONOSPACED);
			notesPanel.add(traceNotes);
			return notesPanel;
		}

		return notesPanel;
	}

	private int addLine(Label label, String message, JTextComponent edit, int gridy, JPanel panel) {
		label.setText(ResourceBundleHelper.getMessageString(message));
		panel.add(label, new GridBagConstraints(0, gridy, 1, 1, 0.2, 0.2, GridBagConstraints.WEST,  GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		panel.add(edit,  new GridBagConstraints(1, gridy, 1, 1, 0.2, 0.2, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		return ++gridy;
	}

	private JTextArea getValueTextArea(String setterMethodName, String fieldText, int fieldSize, Font textFont) {
		JTextArea value = new JTextArea();

		value.setFont(textFont);
		value.setText(fieldText);
		value.setName(setterMethodName);
		value.setColumns(fieldSize);
		
		value.setRows(1);
		value.setLineWrap(true);
		value.setWrapStyleWord(true);
		value.setEditable(true);
		value.setTabSize(4);
		value.setBackground(Color.WHITE);
		
		return (JTextArea)docListener(value);
	}

	private JTextComponent docListener(JTextComponent value) {
		value.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				updateText(e.getDocument());
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				updateText(e.getDocument());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateText(e.getDocument());
			}

			private void updateText(Document document) {
				try {
					Method method = Class.forName("com.att.aro.core.tracemetadata.pojo.MetaDataModel").getMethod(value.getName(), String.class);
					method.invoke(metaDataModel, document.getText(0, document.getLength()));

					if (traceNotes.getLineCount() != traceNotesLineCount) {
						adjustDimensions();
					}
				} catch (NoSuchMethodException | SecurityException | ClassNotFoundException e) {
					LOGGER.error("Exception Thrown for Metadata Update. Method name not found");
				} catch (IllegalAccessException e) {
					LOGGER.error("Exception Thrown for Metadata Update. IllegalAccessException");
				} catch (IllegalArgumentException e) {
					LOGGER.error("Exception Thrown for Metadata Update. IllegalArgumentException");
				} catch (InvocationTargetException e) {
					LOGGER.error("Exception Thrown for Metadata Update. InvocationTargetException");
				} catch (BadLocationException e) {
					LOGGER.error("Exception Thrown for Metadata Update. BadLocationException");
				}
			}
		});

		return value;
	}

	private JTextField getValueTextField(String setterMethodName, String fieldText, int fieldSize, Font textFont) {
		JTextField value = new JTextField();

		value.setText(fieldText);
		value.setFont(textFont);
		value.setName(setterMethodName);
		value.setColumns(fieldSize);
		return (JTextField)docListener(value);
	}

	private JPanel getButtonPanel() {
		if (buttonPanel == null) {
			buttonPanel = new JPanel();
			buttonPanel.setLayout(new BorderLayout());
			buttonPanel.add(getJButtonGrid(), BorderLayout.LINE_END);
		}
		return buttonPanel;
	}

	private JPanel getJButtonGrid() {
		if (jButtonGrid == null) {
			GridLayout gridLayout = new GridLayout();
			gridLayout.setRows(1);
			gridLayout.setHgap(10);
			jButtonGrid = new JPanel();
			jButtonGrid.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 8));
			jButtonGrid.setLayout(gridLayout);
			jButtonGrid.add(okButton = getButton("Save & Close", (ActionEvent arg) -> saveAndClose()));
			jButtonGrid.add(getButton("Cancel", (ActionEvent arg) -> cancelDialog()));
		}
		return jButtonGrid;
	}

	private JButton getButton(String text, ActionListener al) {
		JButton button = new JButton();
		button.setText(text);
		button.addActionListener(al);
		return button;
	}

	@Override
	public void dispose() {
		if (callerMenuItem != null) {
			callerMenuItem.setEnabled(true);
		}
		super.dispose();
	}

	private void cancelDialog() {
		cancelled = true;
		metaDataModel.setTraceNotes(buTraceNotes);
		buTraceNotes = null;
		dispose();
	}
	
	private void saveAndClose() {
		try {
			if (result != null) {
				result.setMetaData(metadataHelper.getMetaData());
				metadataHelper.saveJSON(tracePath);
				this.traceNotesCaretPoint = traceNotes.getCaret().getMagicCaretPosition();
				this.traceNotesCaretPosition = traceNotes.getCaretPosition();
			}
			dispose();
			((MainFrame) parent).refreshBestPracticesTab();
		} catch (Exception e) {
			LOGGER.error("Exception Thrown for Save and Close. Couldnt Save Metadata JSON", e);
		}
	}

	private String getTracePath() {
		return result != null ? result.getTraceDirectory() : null;
	}

	private AbstractTraceResult getAbstractTraceResult() {
		AbstractTraceResult result = null;
		if (((MainFrame) parent).getController().getCurrentTraceInitialAnalyzerResult() != null 
		 && ((MainFrame) parent).getController().getCurrentTraceInitialAnalyzerResult().getTraceresult() != null) {
			result = ((MainFrame) parent).getController().getCurrentTraceInitialAnalyzerResult().getTraceresult();
		}
		return result;
	}

}
