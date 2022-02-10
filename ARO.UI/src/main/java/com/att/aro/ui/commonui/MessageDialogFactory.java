/*
 *  Copyright 2015, 2021 AT&T
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
package com.att.aro.ui.commonui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.apache.commons.lang.StringUtils;
import org.jfree.ui.tabbedui.VerticalLayout;
import org.jfree.util.Log;
import org.springframework.context.ApplicationContext;

import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.tracemetadata.IMetaDataHelper;
import com.att.aro.core.tracemetadata.impl.MetaDataHelper;
import com.att.aro.core.tracemetadata.pojo.MetaDataModel;
import com.att.aro.core.util.Util;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.view.images.Images;

/**
 * A factory class for displaying common message dialogs used by the ARO Data Analyzer.
 */
public class MessageDialogFactory extends JOptionPane{
	public MessageDialogFactory() {
	}
	private static final long serialVersionUID = 1L;

	private static MessageDialogFactory msgDialogInstance;
	private static byte[] msgDialogInstanceCriticalRegion = new byte[0];
	
	public static MessageDialogFactory getInstance(){
		synchronized(msgDialogInstanceCriticalRegion) {
			if (msgDialogInstance == null) {
				msgDialogInstance = new MessageDialogFactory();
			}
		}
		
		return msgDialogInstance;
	}
	
	
	/**
	 * Displays a dialog that is used for reporting unexpected exceptions to the
	 * user. The error dialog is associated with the specified parent window,
	 * and contains the specified exception. Unexpected exceptions can be I/O
	 * exceptions or other checked exceptions that can be handled locally.
	 * 
	 * @param parentComponent
	 *            The parent window to associate with this dialog.
	 * @param throwable
	 *            The exception that should be thrown for this error.
	 */
	public void showUnexpectedExceptionDialog(Component parentComponent, Throwable throwable) {
		String msg = throwable.getLocalizedMessage();
		if (msg != null && msg.length() > 200) {
			msg = ResourceBundleHelper.getMessageString("Error.defaultMsg");
		}
		showMessageDialog(
				parentComponent,
				MessageFormat.format(ResourceBundleHelper.getMessageString("Error.unexpected"), throwable.getClass().getName(),
						msg), ResourceBundleHelper.getMessageString("error.title"), ERROR_MESSAGE);
	}
	
	public void showPlainDialog(Component parentComponent,String msg,String title) {
		showMessageDialog(parentComponent,msg, title, PLAIN_MESSAGE);
	}
	
	public void showInformationDialog(Component parent, String message, String title) {
				showMessageDialog(parent, message, title, INFORMATION_MESSAGE);
	}
	
	/**
	 * Displays an error dialog with the specified title. The error dialog is
	 * associated with the specified parent window, and contains the specified
	 * message.
	 * 
	 * @param window
	 *            The parent window to associate with this dialog.
	 * @param message
	 *            The message to be displayed in the dialog.
	 * @param title
	 *            The dialog title.
	 */
	public void showErrorDialog(Window window, String message, String title) {
		showMessageDialog(window, message, title, ERROR_MESSAGE);
	}

	/**
	 * Displays an error dialog using the default title. The error dialog is
	 * associated with the specified parent window, and contains the specified
	 * message.
	 * 
	 * @param window
	 *            The parent window to associate with this dialog.
	 * @param message
	 *            The message to be displayed in the dialog.
	 */
	public void showErrorDialog(Window window, String message) {

		if (window != null) {
			window.setFocusable(true);
		}

		if (window !=null && Util.isMacOS()) {
			window.setAlwaysOnTop(true);
		}

		showMessageDialog(window, message, ResourceBundleHelper.getMessageString("error.title"), ERROR_MESSAGE);

		if (window != null && Util.isMacOS()) {
			window.setAlwaysOnTop(false);
		}

	}

	/**
	 * Displays a confirmation dialog using the default title. The confirmation
	 * dialog is associated with the specified parent window, contains the
	 * specified message, and uses the specified optionType.
	 * 
	 * @param parentComponent
	 *            The parent window to associate with this dialog.
	 * @param message
	 *            The message to be displayed in the dialog.
	 * @param optionType
	 *            An int that identifies the dialog option type.
	 */
	public int showConfirmDialog(Component parentComponent, String message, int optionType) {
		Object[] options = { ResourceBundleHelper.getMessageString("jdialog.option.yes"), ResourceBundleHelper.getMessageString("jdialog.option.no") };
		return JOptionPane.showOptionDialog(parentComponent, message,
				ResourceBundleHelper.getMessageString("confirm.title"), optionType, JOptionPane.QUESTION_MESSAGE, null,
				options, options[0]);
	}

	public int showStopDialog(Component parentComponent, String message, String title, int optionType) {
		Object[] options = { ResourceBundleHelper.getMessageString("jdialog.option.stop")};
		return JOptionPane.showOptionDialog(parentComponent
					, message
					, title
					, optionType
					, JOptionPane.QUESTION_MESSAGE
					, null
					, options
					, options[0]);
	}

	/**
	 * Displays a confirmation dialog for exporting data from a table. The dialog
	 * uses the default title, and is associated with the specified parent window.
	 * 
	 * @param parentComponent
	 *            The parent window to associate with this dialog.
	 */
	public int showExportConfirmDialog(Component parentComponent) {
		Object[] options = { ResourceBundleHelper.getMessageString("Button.open"), ResourceBundleHelper.getMessageString("Button.ok") };
		return JOptionPane.showOptionDialog(parentComponent, ResourceBundleHelper.getMessageString("table.export.success"),
				ResourceBundleHelper.getMessageString("confirm.title"), JOptionPane.YES_OPTION,
				JOptionPane.OK_CANCEL_OPTION, null, options, options[1]);
	}
	
	/**
	 * Dialog at after stopping a trace, giving options to:
	 *  [Open] - Open the trace
	 *  [OK]
	 * 
	 * @param parent
	 * @param path
	 * @param metaDataModel
	 * @param videoStatus
	 * @param traceDuration
	 * @return
	 */
	@SuppressWarnings("null")
	public boolean showTraceSummary( Component parent
									, String path
									, MetaDataModel metaDataModel
									, boolean videoStatus
									, String traceDuration){
		
		boolean approveOpenTrace = false;
		ApplicationContext context = SpringContextUtil.getInstance().getContext();
		IMetaDataHelper metaDataHelper = context.getBean("metaDataHelper", MetaDataHelper.class);
		
		int HEADER_DATA_SPACING = 10;
		

		JPanel spacePanel = new JPanel();
		spacePanel.setPreferredSize(new Dimension(this.getWidth(), HEADER_DATA_SPACING));

		JTextField pathTextField = null;		
		JTextField dataTextField = null;		
		JTextField videoTextField = null;		
		JTextField durationTextField = null;	

		JTextField targetedAppTextField = null;
		JTextField videoNameTextField = null; 
		JTextField urlTextField = null; 	
		JTextField netWorkTextField = null;
		JTextField traceNotesTextField = null;

		// Section: Trace Summary
		JPanel summaryTitlePanel = new JPanel(new BorderLayout());
		JLabel summaryLabel = new JLabel(ResourceBundleHelper.getMessageString("collector.summary"));
		summaryLabel.setFont(AroFonts.TEXT_FONT);
		summaryTitlePanel.add(summaryLabel, BorderLayout.CENTER);
		
		JPanel summaryLabeledValue = new JPanel(new GridLayout(4, 2, 0, 5));
		pathTextField     = generateEntries(summaryLabeledValue, AroFonts.TEXT_FONT, "collector.path", 		false, path);
		dataTextField     = generateEntries(summaryLabeledValue, AroFonts.TEXT_FONT, "collector.data", 		false, ResourceBundleHelper.getMessageString("collector.dataValue"));
		videoTextField    = generateEntries(summaryLabeledValue, AroFonts.TEXT_FONT, "collector.video", 		false, videoStatus ? "Yes" : "No");
		durationTextField = generateEntries(summaryLabeledValue, AroFonts.TEXT_FONT, "collector.duration", 	false, traceDuration);

		JPanel summaryPanel = new JPanel(new VerticalLayout());
		summaryPanel.add(summaryTitlePanel);
		summaryPanel.add(spacePanel);
		summaryPanel.add(summaryLabeledValue);
		// =========================================================

		// Section: MetaData		
		JPanel metaDataTitlePanel = new JPanel(new BorderLayout());
		JLabel metaDataLabel = new JLabel(ResourceBundleHelper.getMessageString("collector.metaData"));
		metaDataLabel.setFont(AroFonts.TEXT_FONT);
		metaDataTitlePanel.add(metaDataLabel, BorderLayout.CENTER);

		JPanel metaLabeledValue = new JPanel(new GridLayout(5, 2, 0, 5));
		targetedAppTextField  = generateEntries(metaLabeledValue, AroFonts.TEXT_FONT, "metadata.field.targetedApp"	, true, metaDataModel.getTargetedApp()  );
		videoNameTextField    = generateEntries(metaLabeledValue, AroFonts.TEXT_FONT, "metadata.field.videoName"	, true, metaDataModel.getVideoName()    );
		urlTextField          = generateEntries(metaLabeledValue, AroFonts.TEXT_FONT, "metadata.field.url"			, true, metaDataModel.getURL()          );
		netWorkTextField      = generateEntries(metaLabeledValue, AroFonts.TEXT_FONT, "metadata.field.netWork"		, true, metaDataModel.getNetWork()      );
		traceNotesTextField   = generateEntries(metaLabeledValue, AroFonts.TEXT_FONT, "metadata.field.traceNotes"	, true, metaDataModel.getTraceNotes()   );

		JPanel metaPanel = new JPanel(new VerticalLayout());
		metaPanel.add(metaDataTitlePanel);   
		metaPanel.add(spacePanel);
		metaPanel.add(metaLabeledValue);
		// =========================================================

		JPanel traceSummaryPanel = new JPanel(new BorderLayout());
		traceSummaryPanel.add(summaryPanel, BorderLayout.NORTH);
		traceSummaryPanel.add(spacePanel, BorderLayout.CENTER);
		traceSummaryPanel.add(metaPanel, BorderLayout.SOUTH);
		
		String[] options = new String[] { "OK", "Open" };
		int opt = MessageDialogFactory.showOptionDialog(parent, traceSummaryPanel, "Confirm", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
		approveOpenTrace = (opt == 1);
		
		try {
			metaDataModel.setTraceDuration(Double.parseDouble(durationTextField.getText()));
		} catch (NumberFormatException e) {
			Log.error("NumberFormatException: ", e);
			metaDataModel.setTraceDuration(0.0);
		}
		
		metaDataModel.setTargetedApp 	(targetedAppTextField	.getText());
		metaDataModel.setVideoName   	(videoNameTextField		.getText());
		metaDataModel.setURL         	(urlTextField			.getText());
		metaDataModel.setNetWork     	(netWorkTextField		.getText());
		metaDataModel.setTraceNotes  	(traceNotesTextField	.getText());
		try {
			metaDataHelper.saveJSON(path, metaDataModel);
		} catch (Exception e) {
			Log.error("Failed to save metadata.json", e);
		}
		return approveOpenTrace;
	}
	
	private JTextField generateEntries(JPanel metaDataPane, Font text_font, String mstr, boolean editable, String value) {
		if (value == null) {
			value = "";
		}
		// Label
		String sLabel = ResourceBundleHelper.getMessageString(mstr);
		JLabel label = new JLabel(sLabel);
		label.setFont(text_font);
		metaDataPane.add(label);

		// value
		JTextField textField = new JTextField(value);
		textField.setFont(text_font);
		textField.setEditable(editable);
		metaDataPane.add(textField);
		return textField;
	}
	
	/**
	 * Inform user that phone is rooted and does not have the rooted collector
	 * supports a hyperlink to the developer portal
	 * 
	 * @param options contains navigation buttons such as OK & Cancel
	 * @return user response
	 */
	public int confirmCollectionMethod(Object[] options, String message, String linkMessageParm) {
		JLabel label = new JLabel();
		Font font = label.getFont();
		String linkMessage = linkMessageParm == null ? "" : linkMessageParm;

		// create some css from the label's font
		StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
		style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
		style.append("font-size:" + font.getSize() + "pt;");

		String line2 = "";
		if (message != null) {
			line2 = "<p>" + message + "</p>";
		}
		
		// html content
		JEditorPane editPane = new JEditorPane("text/html", "<html><body style=\"" + style + "\">" //
				+ "<p>The Phone appears to have root capability, but does not have the rooted collector installed.</p>"
				+ line2
				+ linkMessage
				+ "</body></html>");

		// handle link events
		editPane.addHyperlinkListener(new HyperlinkListener() {

			@Override
			public void hyperlinkUpdate(HyperlinkEvent hlEvent) {
				if (hlEvent.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
					// ProcessHandler.launchUrl(e.getURL().toString()); // roll your own link launcher or use Desktop if J6+

					try {
						Desktop.getDesktop().browse(new URI("https://developer.att.com/application-resource-optimizer/get-aro/download"));
					} catch (IOException ioExp) {
						ioExp.printStackTrace();
					} catch (URISyntaxException uriExp) {
						uriExp.printStackTrace();
					}

				}
			}
		});
		editPane.setEditable(false);
		editPane.setBackground(label.getBackground());

		JOptionPane pane = new JOptionPane(editPane
				, JOptionPane.CANCEL_OPTION
				, JOptionPane.OK_CANCEL_OPTION
				, Images.ICON.getIcon()
				, options
				, options[0]);
		JDialog dialog = pane.createDialog("Message");
		dialog.setModal(true);
		dialog.setModalityType(ModalityType.APPLICATION_MODAL);
		dialog.setVisible(true);
		
		int response = 0;

		Object selectedValue = pane.getValue();

		if (selectedValue == null) {
			response = 1;
		}
		
		for (int counter = 0, maxCounter = options.length; counter < maxCounter; counter++) {
			if (options[counter].equals(selectedValue)){

				response = counter;
				break;
			}
		}

		return Math.abs(response);
	}

}
