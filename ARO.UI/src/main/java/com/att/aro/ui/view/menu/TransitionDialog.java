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
package com.att.aro.ui.view.menu;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import com.att.aro.core.util.Util;

import lombok.Getter;
import lombok.Setter;

/**
 * This dialog helps pull the focus from UI elements, such as menuitems, and provide the user with a message that something is happening in the background.
 * The dialog will start with APPLICATION_MODAL and after a 200 ms (2000 ms for Windows), will transition to MODELESS. 
 * This transition requires that the dialog vanish for a brief moment, so there will be a blink
 *
 */
@Getter
@Setter
public class TransitionDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private JTextArea textArea;
	private int maxWidth;
	private String altTitle;
	private AffineTransform affinetransform;
	private FontRenderContext frc;
	
	private int getStringFontWidth(Font font, String line) {
		if (frc == null) {
			AffineTransform affinetransform = new AffineTransform();
			frc = new FontRenderContext(affinetransform, true, true);
		}
		return (int) (font.getStringBounds(line, frc).getWidth());
	}

	public TransitionDialog(Frame frame, String... message) {
		super(frame, "Message", true);

		if (message == null || message.length == 0) {
			message = new String[] { "Busy preparing data" };
		}

		altTitle = message.length > 1 ? message[1] : message[0];

		Font font = new Font("Dialog", Font.PLAIN, 18);
		textArea = new JTextArea(message.length, 1);
		textArea.setFont(font);
		for (String line : message) {

			int lineWidth = getStringFontWidth(font, line);
			if (lineWidth > maxWidth) {
				maxWidth = lineWidth;
			}
			textArea.append(line + "\n");
		}
		textArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		textArea.setEditable(false);
		add(textArea);
		setPreferredSize(new Dimension(maxWidth + 20, 200));
		setAlwaysOnTop(true);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setLocation(50, 100);
		validate();

		new SwingWorker<Object, Object>() {
			@Override
			protected Object doInBackground() {
				try {
					Thread.sleep(Util.isWindowsOS() ? 2000 : 200);
				} catch (InterruptedException e) {
				}
				return null;
			}

			@Override
			protected void done() {
				// hide dialog
				setVisible(false);

				// reset modality
				setModalityType(ModalityType.MODELESS);
				setModal(false);

				// show dialog
				/* In Windows, only the Title bar will show, after going {hidden}MODELESS{visible}
				 * so placing the message content in place of title and collapse the rest of the dialog
				 */
				if (Util.isWindowsOS()) { 
					setTitle(altTitle);
					setSize(getMaxWidth() + 20, 0);
				}
				setVisible(true);
				validate();
			}
		}.execute();
		pack();
		setVisible(true);
	}

	public String getMessageLabelText() {
		return textArea.getText();
	}

}
