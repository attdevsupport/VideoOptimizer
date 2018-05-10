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
package com.att.aro.ui.view;

import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.att.aro.core.ILogger;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class ConfirmationDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	JPanel panel;
	JButton okBtn;

	@InjectLogger
	private static ILogger log;

	static ResourceBundle resourceBundle = ResourceBundleHelper.getDefaultBundle();

	public void createDialog() {

	}

	public String getLabelMsg() {
		return null;
	}

}
