package com.att.aro.ui.view.menu.datacollector;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.att.aro.core.peripheral.pojo.AttenuatorModel;
import com.att.aro.ui.commonui.DataCollectorSelectNStartDialog;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class AttnrRadioGroupPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	private String attnrSlider;
	private String attnrLoadFile;
	private String attnrNone;

	private JRadioButton attnrSliderRBtn;
	private JRadioButton rbAtnrLoadFile;
	private JRadioButton rbAtnrNone;
	private ButtonGroup radioAttnGroup;
	private AttnrPanel parentPanel;
	private DataCollectorSelectNStartDialog startDialog;
	private AttnrThroughputThrottlePanel attnrTTPanel;
	private AttnrLoadProfilePanel attnrLoadPanel;
	private AttenuatorModel miniAtnr;

	public AttnrRadioGroupPanel(AttnrPanel jp, AttenuatorModel miniAtnr, DataCollectorSelectNStartDialog startDialog) {

		setLayout(new FlowLayout());
		this.parentPanel = jp;
		this.startDialog = startDialog;
		this.miniAtnr = miniAtnr;
		attnrSlider = ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.slider");
		attnrLoadFile = ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.loadfile");
		attnrNone = ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.none");

		attnrSliderRBtn = new JRadioButton(attnrSlider);
		rbAtnrLoadFile = new JRadioButton(attnrLoadFile);
		rbAtnrNone = new JRadioButton(attnrNone);

		radioAttnGroup = new ButtonGroup();
		radioAttnGroup.add(attnrSliderRBtn);
		radioAttnGroup.add(rbAtnrLoadFile);
		radioAttnGroup.add(rbAtnrNone);

		attnrSliderRBtn.addActionListener(this);
		rbAtnrLoadFile.addActionListener(this);
		rbAtnrNone.addActionListener(this);
		rbAtnrNone.setSelected(true);

		add(rbAtnrNone);

		add(attnrSliderRBtn);

		add(rbAtnrLoadFile);
	}

	@Override
	public void actionPerformed(ActionEvent ac) {

		if (attnrSlider.equals(ac.getActionCommand())) {
			parentPanel.getAttnrHolder().remove(getLoadProfilePanel());
			getLoadProfilePanel().resetComponent();
			parentPanel.getAttnrHolder().add(getThroughputPanel(),BorderLayout.CENTER);
			parentPanel.getAttnrHolder().revalidate();
			parentPanel.getAttnrHolder().repaint();

			miniAtnr.setConstantThrottle(true);
			miniAtnr.setFreeThrottle(false);
			miniAtnr.setLoadProfile(false);
			startDialog.resizeLarge();

		} else if (attnrLoadFile.equals(ac.getActionCommand())) {
			parentPanel.getAttnrHolder().remove(getThroughputPanel());
			getThroughputPanel().resetComponent();
			parentPanel.getAttnrHolder().add(getLoadProfilePanel());
			parentPanel.getAttnrHolder().revalidate();
			parentPanel.getAttnrHolder().repaint();

			miniAtnr.setConstantThrottle(false);
			miniAtnr.setFreeThrottle(false);
			miniAtnr.setLoadProfile(true);
			startDialog.resizeMedium();

		} else if (attnrNone.equals(ac.getActionCommand())) {
			reset();
		}
	}

	public AttnrLoadProfilePanel getLoadProfilePanel() {
		if (attnrLoadPanel == null) {
			attnrLoadPanel = new AttnrLoadProfilePanel(miniAtnr);
		}
		return attnrLoadPanel;
	}

	public AttnrThroughputThrottlePanel getThroughputPanel(){
		if(attnrTTPanel == null){
			attnrTTPanel = new AttnrThroughputThrottlePanel(miniAtnr);
		}
		return attnrTTPanel;
	}

	public void enableComponents(Container container, boolean enable) {
		Component[] components = container.getComponents();
		for (Component component : components) {
			component.setEnabled(enable);
			if (component instanceof Container) {
				enableComponents((Container) component, enable);
			}
		}
	}
	
	public void reset() {
		parentPanel.getAttnrHolder().remove(getThroughputPanel());
		getThroughputPanel().resetComponent();		
		parentPanel.getAttnrHolder().remove(getLoadProfilePanel());
		getLoadProfilePanel().resetComponent();
		parentPanel.getAttnrHolder().revalidate();
		parentPanel.getAttnrHolder().repaint();

		miniAtnr.setConstantThrottle(false);
		miniAtnr.setFreeThrottle(true);
		miniAtnr.setLoadProfile(false);
		startDialog.resizeMedium();
		rbAtnrNone.setSelected(true);
	}
}
