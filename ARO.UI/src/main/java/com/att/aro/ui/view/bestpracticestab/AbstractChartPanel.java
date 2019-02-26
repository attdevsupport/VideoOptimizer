package com.att.aro.ui.view.bestpracticestab;

import java.util.Observer;

import javax.swing.JPanel;

import com.att.aro.core.pojo.AROTraceData;

public abstract class AbstractChartPanel extends JPanel implements Observer{

	private static final long serialVersionUID = 1L;

	public abstract void refresh(AROTraceData aModel);

}