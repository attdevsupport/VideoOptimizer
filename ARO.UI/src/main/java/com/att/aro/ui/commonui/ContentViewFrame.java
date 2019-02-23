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
package com.att.aro.ui.commonui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.att.aro.core.packetanalysis.IHttpRequestResponseHelper;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.ui.model.diagnostic.ContentException;
import com.att.aro.ui.utils.ResourceBundleHelper;


public class ContentViewFrame extends JFrame {

	private static final long serialVersionUID = 1L;

//	private static final Logger LOGGER = LogManager.getLogger(ContentViewFrame.class.getName());

	private HttpRequestResponseInfo rrInfo;
	private Session session;
	private JScrollPane contentComponent;
	private JPanel buttonPanel;
	private JButton closeButton;

	private IHttpRequestResponseHelper httpHelper = ContextAware.getAROConfigContext().getBean(IHttpRequestResponseHelper.class);

	private String originalImage;

	private StringBuffer imageMetadata;

	/**
	 * Initializes a new instance of the ContentViewFrame class using the
	 * specified content.
	 * 
	 * @param rrInfo
	 *            - The HttpRequestResponseInfo object containing the content.
	 */
	public ContentViewFrame(Session session,HttpRequestResponseInfo rrInfo)
			throws ContentException, IOException,Exception {
		this.rrInfo = rrInfo;
		this.session = session;
		initialize();
	}
	
	public ContentViewFrame(String imgOrginal)
			throws ContentException, IOException,Exception {
		this.originalImage = imgOrginal;
		initializeContent();
	}
	
	public ContentViewFrame(StringBuffer imgMetadata)
			throws ContentException, IOException,Exception {
		this.imageMetadata = imgMetadata;
		initializeMetadataContent();
	}

	/**
	 * Returns the request/response object whose content is being viewed.
	 * 
	 * @return HttpRequestResponseInfo The request/response object associated
	 *         with this ContentViewer.
	 */
	public HttpRequestResponseInfo getRrInfo() {
		return rrInfo;
	}

	public String getOrgImageInfo() {
		return originalImage;
	}
	
	public String getImageMetadata() {
		return imageMetadata.toString();
	}
	
	/**
	 * This method initializes the frame.
	 * 
	 * @throws ContentException
	 * @throws IOException
	 */
	private void initialize() throws ContentException, IOException,Exception {
		this.setMinimumSize(new Dimension(400, 400));
		this.setTitle(ResourceBundleHelper.getMessageString("fileChooser.rrContentViewerTitle"));
		this.setLocationByPlatform(true);

		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panel.add(getContentComponent(), BorderLayout.CENTER);
		panel.add(getButtonPanel(), BorderLayout.SOUTH);
		this.setContentPane(panel);
	}
	
	private void initializeMetadataContent() throws ContentException, IOException,Exception {
		this.setMinimumSize(new Dimension(400, 400));
		this.setTitle(ResourceBundleHelper.getMessageString("fileChooser.rrContentViewerTitle"));
		this.setLocationByPlatform(true);

		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panel.add(getMetadatContentComponent(), BorderLayout.CENTER);
		panel.add(getButtonPanel(), BorderLayout.SOUTH);
		this.setContentPane(panel);
	}
	
	private void initializeContent() throws ContentException, IOException,Exception {
		this.setMinimumSize(new Dimension(400, 400));
		this.setTitle(ResourceBundleHelper.getMessageString("fileChooser.rrContentViewerTitle"));
		this.setLocationByPlatform(true);

		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panel.add(getImageContent(), BorderLayout.CENTER);
		panel.add(getButtonPanel(), BorderLayout.SOUTH);
		this.setContentPane(panel);
	}
	
	private JScrollPane getContentComponent() throws ContentException,
			IOException, Exception {
		if (contentComponent == null) {
			JComponent result = null;
			if (rrInfo.getContentType() != null && rrInfo.getContentType()
					.contains(ResourceBundleHelper.getMessageString("fileChooser.contentType.image"))) {
				ImageIcon icon = new ImageIcon(httpHelper.getContent(rrInfo, session));
				JLabel imagelJLabel = new JLabel(icon);
				imagelJLabel.setVerticalAlignment(JLabel.CENTER);
				result = imagelJLabel;
			} else {
				String contentString = httpHelper.getContentString(rrInfo, session);
				if(contentString.length() > 100*1000) {//100kb
					contentString = insertPeriodically(contentString, 1000);
				}
				JTextArea contentTxtArea = new JTextArea(contentString);
				contentTxtArea.setLineWrap(true);
				contentTxtArea.setEditable(false);
				result = contentTxtArea;
			}
			contentComponent = new JScrollPane(result);
		}
		return contentComponent;
	}

	//Ref https://stackoverflow.com/questions/537174/putting-char-into-a-java-string-for-each-n-characters
	//TODO Add new lines at the end of element instead of every x characters
	private String insertPeriodically(String text, int period) {
		String insert = "\n";
		StringBuilder builder = new StringBuilder(text.length() + insert.length() * (text.length() / period) + 1);
		int index = 0;
		String prefix = "";
		while (index < text.length()) {
			// Don't put the insert in the very first iteration.
			// This is easier than appending it *after* each substring
			builder.append(prefix);
			prefix = insert;
			builder.append(text.substring(index, Math.min(index + period, text.length())));
			index += period;
		}
		return builder.toString();
	}
	
	private JScrollPane getMetadatContentComponent() throws ContentException,
			IOException, Exception {
		if (contentComponent == null) {
			JComponent result = null;

			JTextArea contentTxtArea = new JTextArea(getImageMetadata());
			contentTxtArea.setLineWrap(true);
			contentTxtArea.setEditable(false);
			result = contentTxtArea;
			contentComponent = new JScrollPane(result);
		}
		

		return contentComponent;
	}
	private JScrollPane getImageContent() throws ContentException,
			IOException, Exception {
		if (contentComponent == null) {
			
			Path path = Paths.get(originalImage);
			

			ImageIcon icon = new ImageIcon(Files.readAllBytes(path));
			
			
			JLabel imagelJLabel = new JLabel(icon);

			imagelJLabel.setVerticalAlignment(JLabel.CENTER);
			// JComponent result = imagelJLabel;

			contentComponent = new JScrollPane(imagelJLabel);
		}
		return contentComponent;
	}

	/**
	 * @return the buttonPanel
	 */
	private JPanel getButtonPanel() {
		if (buttonPanel == null) {
			buttonPanel = new JPanel(new BorderLayout());
			JPanel panel = new JPanel(new GridLayout(1, 2, 5, 5));
			panel.add(getCloseButton());
			buttonPanel.add(panel, BorderLayout.EAST);
		}
		return buttonPanel;
	}

	/**
	 * Initializes and returns the Close button for this ContentViewer.
	 * 
	 * @return JButton The Close button.
	 */
	public JButton getCloseButton() {
		if (closeButton == null) {
			closeButton = new JButton(ResourceBundleHelper.getMessageString("Button.close"));
			closeButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					ContentViewFrame.this.dispose();
				}

			});
		}
		return closeButton;
	}

}
