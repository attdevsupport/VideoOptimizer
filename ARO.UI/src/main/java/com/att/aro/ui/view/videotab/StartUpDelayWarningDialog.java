package com.att.aro.ui.view.videotab;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.codehaus.jackson.map.ObjectMapper;

import com.att.aro.core.packetanalysis.IVideoUsageAnalysis;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.att.aro.mvc.IAROView;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.EnableEscKeyCloseDialog;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.diagnostictab.GraphPanel;

public class StartUpDelayWarningDialog extends JDialog {

	private static final long serialVersionUID = -3506390644939515475L;

	private IAROView parent;
	private int width = 240;
	private int height = 120;
	private JPanel contentPanel;
	private JPanel jDialogPanel;
	GraphPanel graphPanel = null;
	private IVideoUsageAnalysis videoUsage = ContextAware.getAROConfigContext().getBean(IVideoUsageAnalysis.class);
	private VideoUsagePrefs videoUsagePrefs;
	IARODiagnosticsOverviewRoute diagnosticRoute;
	private PacketAnalyzerResult currentTraceResult;
	private JLabel warningLabel;

	private static ResourceBundle resourceBundle = ResourceBundleHelper.getDefaultBundle();

	public StartUpDelayWarningDialog(IAROView parent, IARODiagnosticsOverviewRoute diagnosticRoute) {
		this.parent = parent;
		this.diagnosticRoute = diagnosticRoute;
		initialize();
		new EnableEscKeyCloseDialog(getRootPane(), this);
	}

	private void initialize() {
		PacketAnalyzerResult currentTraceResult = ((MainFrame) parent).getController().getTheModel()
				.getAnalyzerResult();
		if(Util.isWindowsOS())
			height = height + 30;
		setCurrentPktAnalyzerResult(currentTraceResult);
		this.setMinimumSize(new Dimension(width, height));
		this.setMaximumSize(new Dimension(width, height));
		this.setResizable(false);
		this.setTitle(resourceBundle.getString("startupdelay.warning.dialog.title"));
		this.setLocationRelativeTo(getOwner());
		this.setContentPane(getJDialogPanel());
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowLostFocus(WindowEvent e) {
				dispose();
			}
			
			@Override
			public void windowGainedFocus(WindowEvent e) {
				
			}
		});
		if (null != ((MainFrame) parent).getDiagnosticTab()) {
			graphPanel = ((MainFrame) parent).getDiagnosticTab().getGraphPanel();
		}
	}

	private JPanel getJDialogPanel() {
		if (jDialogPanel == null) {
			jDialogPanel = new JPanel();
			jDialogPanel.setLayout(new BorderLayout());
			jDialogPanel.add(getLayoutPanel(), BorderLayout.NORTH);
			pack();
		}
		return jDialogPanel;
	}

	private JPanel getLayoutPanel() {
		if (contentPanel == null) {
			contentPanel = new JPanel();
			contentPanel.setPreferredSize(new Dimension(width, height));
			warningLabel = new JLabel();
			contentPanel.add(warningLabel);
			contentPanel.add(getDontShowCheckBox());
			contentPanel.add(getStartUpDelayButton());
			contentPanel.add(getOkButton());
		}
		return contentPanel;
	}

	public void setWarningMessage(String warningMessage) {
		warningLabel.setText(warningMessage);
	}

	private JCheckBox getDontShowCheckBox() {
		JCheckBox dontShowCheckBox = new JCheckBox(resourceBundle.getString("startupdelay.warning.dialog.dontshow"));
		dontShowCheckBox.setSelected(false);
		dontShowCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AbstractButton abstractButton = (AbstractButton) e.getSource();
				setStartUpDelayReminder(!(abstractButton.getModel().isSelected()));
			}
		});
		return dontShowCheckBox;
	}

	private JButton getStartUpDelayButton() {
		JButton jb = new JButton(resourceBundle.getString("startupdelay.warning.dialog.startupdelay"));
		jb.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (null != graphPanel) {
					StartUpDelayWarningDialog.this.dispose();
					diagnosticRoute.launchSliderDialogFromDiagnosticTab();
				}
			}
		});
		return jb;
	}

	private JButton getOkButton() {
		JButton jb = new JButton(resourceBundle.getString("startupdelay.warning.dialog.ok"));
		jb.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				StartUpDelayWarningDialog.this.dispose();
			}
		});
		return jb;
	}

	public PacketAnalyzerResult getCurrentPktAnalyzerResult() {
		return currentTraceResult;
	}

	public void setCurrentPktAnalyzerResult(PacketAnalyzerResult currentTraceResult) {
		this.currentTraceResult = currentTraceResult;
	}

	public boolean setStartUpDelayReminder(boolean selection) {
		videoUsagePrefs = videoUsage.getVideoUsagePrefs();
		videoUsagePrefs.setStartupDelayReminder(selection);

		ObjectMapper mapper = new ObjectMapper();
		String temp;
		try {
			temp = mapper.writeValueAsString(videoUsagePrefs);
		} catch (IOException e) {
			return false;
		}
		videoUsage.getPrefs().setPref(VideoUsagePrefs.VIDEO_PREFERENCE, temp);
		return true;
	}
}
