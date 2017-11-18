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
