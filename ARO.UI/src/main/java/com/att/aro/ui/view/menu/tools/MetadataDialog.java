package com.att.aro.ui.view.menu.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
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
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.tracemetadata.impl.MetaDataHelper;
import com.att.aro.core.tracemetadata.pojo.MetaDataModel;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.EnableEscKeyCloseDialog;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.SharedAttributesProcesses;

public class MetadataDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LogManager.getLogger(MetadataDialog.class);

	private MetaDataHelper metadataHelper = ContextAware.getAROConfigContext().getBean(MetaDataHelper.class);

	private MetaDataModel metadataModel;

	private JButton okButton;
	private JPanel buttonPanel;
	private JPanel metadataPanel;
	private JPanel jButtonGrid;
	private JPanel jContentPane;
	private final JMenuItem callerMenuItem;
	private final SharedAttributesProcesses parent;
	private EnableEscKeyCloseDialog enableEscKeyCloseDialog;

	private Label descriptionLabel;
	private Label traceTypeLabel;
	private Label targetedAppLabel;
	private Label applicationProducerLabel;
	private Label targetAppVerLabel;
	
	private JTextField descriptionField;
	private JTextField traceTypeField;
	private JTextField targetedAppField;
	private JTextField applicationProducerField;
	private JTextField targetAppVerField;
	private TraceDirectoryResult result;
	private String tracePath = "";

	public MetadataDialog(SharedAttributesProcesses parent, JMenuItem callerMenuItem) {
		super(parent.getFrame());
		this.parent = parent;
		this.callerMenuItem = callerMenuItem;
		callerMenuItem.setEnabled(false);
		try {
			result = getTraceDirectoryResult();
			tracePath = getTracePath();
			if(result!=null && result.getMetaData()!=null) {
				metadataModel= result.getMetaData();
			} else {
				metadataModel = metadataHelper.loadMetaData(tracePath);
			}
			
			init();
			this.setVisible(true);
		} catch (Exception e) {
			LOGGER.error("Exception while reading metadata json", e);
			dispose();
			// MetadataDialog.this.dispose();
		}
	}

	private void init() {
		this.setContentPane(getJContentPane());
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setTitle("Metadata");
		this.setModal(true);
		enableEscKeyCloseDialog = new EnableEscKeyCloseDialog(getRootPane(), this, false);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowDeactivated(WindowEvent event) {
				if (enableEscKeyCloseDialog.consumeEscPressed()) {
					dispose();
				}
			}
		});
		pack();
		setLocationRelativeTo(parent.getFrame());
		getRootPane().setDefaultButton(okButton);
	}

	@Override
	public void dispose() {
		callerMenuItem.setEnabled(true);
		super.dispose();
	}

	private JComponent getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setPreferredSize(new Dimension(550, 350));
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getMetadataPanel(), BorderLayout.CENTER);
			jContentPane.add(getButtonPanel(), BorderLayout.PAGE_END);
		}
		return jContentPane;
	}

	private JPanel getMetadataPanel() {

		if (metadataPanel == null) {

			int index = 0;

			metadataPanel = new JPanel(new GridBagLayout());

			descriptionLabel = new Label(ResourceBundleHelper.getMessageString("metadata.field.description"));
			traceTypeLabel = new Label(ResourceBundleHelper.getMessageString("metadata.field.traceType"));
			targetedAppLabel = new Label(ResourceBundleHelper.getMessageString("metadata.field.targetedApp"));
			applicationProducerLabel = new Label(
					ResourceBundleHelper.getMessageString("metadata.field.applicationProducer"));
			targetAppVerLabel = new Label(ResourceBundleHelper.getMessageString("metadata.field.targetAppVer"));
		
			descriptionField = getValueTextField("setDescription", metadataModel.getDescription(), 20);
			traceTypeField = getValueTextField("setTraceType", metadataModel.getTraceType(), 20);
			targetedAppField = getValueTextField("setTargetedApp", metadataModel.getTargetedApp(), 20);
			applicationProducerField = getValueTextField("setApplicationProducer",
					metadataModel.getApplicationProducer(), 20);
			targetAppVerField = getValueTextField("setTargetAppVer", metadataModel.getTargetAppVer(), 20);
			
			index = addLine(descriptionLabel, descriptionField, index, metadataPanel);
			index = addLine(traceTypeLabel, traceTypeField, index, metadataPanel);
			index = addLine(targetedAppLabel, targetedAppField, index, metadataPanel);
			index = addLine(applicationProducerLabel, applicationProducerField, index, metadataPanel);
			index = addLine(targetAppVerLabel, targetAppVerField, index, metadataPanel);
		}
		metadataPanel.setBorder(BorderFactory.createEmptyBorder(8, 80, 10, 10));
		return metadataPanel;
	}

	private int addLine(Label label, JTextField edit, int idx, JPanel panel) {
		panel.add(label, new GridBagConstraints(0, idx, 1, 1, 0.2, 0.2, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
		panel.add(edit, new GridBagConstraints(1, idx, 1, 1, 1.0, 0.2, GridBagConstraints.LINE_START,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		return ++idx;
	}

	private JTextField getValueTextField(String setterMethodName, String fieldText, int fieldSize) {
		JTextField value = new JTextField();

		value.setText(fieldText);
		value.setName(setterMethodName);
		value.setColumns(fieldSize);

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
					Method method = Class.forName("com.att.aro.core.tracemetadata.pojo.MetaDataModel")
							.getMethod(value.getName(), String.class);
					method.invoke(metadataModel, document.getText(0, document.getLength()));
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
			jButtonGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			jButtonGrid.setLayout(gridLayout);
			jButtonGrid.add(okButton = getButton("Save & Close", (ActionEvent arg) -> saveAndClose()));
			jButtonGrid.add(getButton("Cancel", (ActionEvent arg) -> dispose()));
		}
		return jButtonGrid;
	}

	private JButton getButton(String text, ActionListener al) {
		JButton button = new JButton();
		button.setText(text);
		button.addActionListener(al);
		return button;
	}

	private void saveAndClose() {
		try {
			if (result != null) {
				result.setMetaData(metadataHelper.getMetaData());
				metadataHelper.saveJSON(tracePath);
			}
			dispose();
			((MainFrame) parent).refreshBestPracticesTab();
		} catch (Exception e) {
			LOGGER.error("Exception Thrown for Save and Close. Couldnt Save Metadata JSON", e);
		}
	}

	private String getTracePath() {		
		if (result != null) {
			tracePath = result.getTraceDirectory();
		}
		return tracePath;
	}

	private TraceDirectoryResult getTraceDirectoryResult() {
		TraceDirectoryResult result = null;
		if (((MainFrame) parent).getController().getCurrentTraceInitialAnalyzerResult() != null && ((MainFrame) parent)
				.getController().getCurrentTraceInitialAnalyzerResult().getTraceresult() != null) {
			result = (TraceDirectoryResult) ((MainFrame) parent).getController().getCurrentTraceInitialAnalyzerResult()
					.getTraceresult();
		}
		return result;
	}

}
