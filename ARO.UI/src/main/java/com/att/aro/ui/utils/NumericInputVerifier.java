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
package com.att.aro.ui.utils;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.Timer;

/**
 * <pre>
 * Numeric verification of JTextField inputs. 
 * Instantiate with a Maximum, Minimum, and number of significands.
 * 
 * When clicking on a Save Button you might want to recheck the validity.
 * use:
 * ((NumericInputVerifier)yourJTextField.getInputVerifier()).getResult()
 * 
 * FAQ:
 * Significands is the number of significant digits below the decimal point.
 * Math purists "do not confuse with the mantissa".
 */
public class NumericInputVerifier extends InputVerifier{
	
	private static final int DISPLAYTIMER = 3000;

	private static final Color COLOR = new Color(255, 255, 204);
	
	private double min = 0;
	private double max = 0;
	private Timer timer;
	private Popup popup;
	private int significands;
	String maxMssgFormat = null;
	String minMssgFormat = null;
	
	/**
	 * results of last verification
	 */
	private boolean result;
	private boolean tested;

	/**
	 * Verify a String input for conversion to a numeric value and within range
	 * 
	 * @param max value
	 * @param min value
	 * @param significands - number of significant digits allowed below the decimal
	 */
	public NumericInputVerifier(double max, double min, int significands) {
		this.max = max;
		this.min = min;
		this.significands = significands;
		maxMssgFormat = (new StringBuilder()).append("Maximum value is %." + String.format("%d", significands) + "f").toString();
		minMssgFormat = (new StringBuilder()).append("Minimum value is %." + String.format("%d", significands) + "f").toString();
	}

	
	@Override
	public boolean verify(JComponent input) {
		
		hidePopup();
		
		String text = ((JTextField) input).getText();
		result = false;
		tested = true;
		try {
			text = text.trim();
			// strip trailing '0' and if it is last the '.' 
			text = text.indexOf(".") < 0 ? text : text.replaceAll("0*$", "").replaceAll("\\.$", "");
			
			BigDecimal value = new BigDecimal(text);
			
			if (value.doubleValue() > max) {
				popup(input, String.format(maxMssgFormat, max));
			} else if (value.doubleValue() < min) {
				popup(input, String.format(minMssgFormat, min));
			} else if (value.scale() > significands) {
				popup(input, significands>0
						? String.format("No more than %d digits beyond decimal point", significands)
						: "Integer values only"
						);
			} else {
				result = true;
			}
			
		} catch (Exception e) {
			popup(input, String.format("Illegal value!"));
		}

		return result;
	}

	/**
	 * Popup a display offset to the right of the JComponent and slightly lower
	 * 
	 * @param component
	 * @param messageText
	 */
	private void popup(JComponent component, String messageText) {
		Point position = component.getLocationOnScreen();
		int yOffset = position.y + (int) (component.getHeight() * 0.2);
		int xOffset = position.x + (int) (component.getWidth() * 1.1);

		PopupFactory factory = PopupFactory.getSharedInstance();
		JTextField message = new JTextField(messageText);
		message.setBackground(COLOR);

		popup = factory.getPopup(component.getParent(), message, xOffset, yOffset);
		popup.show();

		ActionListener hider = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				timer.stop();
				popup.hide();
			}
		};

		// Hide popup in 3 seconds
		if (timer != null) {
			timer.stop();
		}
		timer = new Timer(DISPLAYTIMER, hider);
		timer.start();

	}

	private void hidePopup() {
		if (timer != null && timer.isRunning()) {
			timer.stop();
		}
		if (popup != null) {
			popup.hide();
		}
	}
	
	/**
	 * Returns result of last verification.
	 * 
	 * @return false only if tested and result is false, otherwise returns true
	 */
	public boolean getResult() {
		return result == tested;
	}
}
