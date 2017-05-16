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
package com.att.aro.ui.collection;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingWorker;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.datacollector.IDataCollector;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.video.pojo.VideoOption;
import com.att.aro.mvc.AROCollectorActionEvent;
import com.att.aro.ui.commonui.AROProgressDialog;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;

/**
 * Manages launching & stopping 
 * 
 *
 *
 * @param <T>
 * @param <V>
 */
public class AROCollectorSwingWorker<T, V> extends SwingWorker<T, V>{

	@InjectLogger
	private ILogger log;
	
	private JFrame parentUI;
	private AROProgressDialog progress;
	List<PropertyChangeListener> changeListeners = null;
	private int eventId;// eventID = 1,2,3
	private String command;
	private String msg;
	List<ActionListener> actionListeners = null;
	
	IDataCollector collector;
	private IAroDevice device;
	private String traceFolderPath;

	private String udid;


	private Hashtable<String, Object> extraParams;
	private VideoOption videoCapture;
	
	/**
	 *  Stop Collector
	 * @param IAROView 
	 * @param frmApplicationResourceOptimizer
	 * @param actionListeners
	 * @param eventId
	 * @param command
	 * @param msg
	 */
	public AROCollectorSwingWorker(JFrame frmApplicationResourceOptimizer
						, List<ActionListener> actionListeners
						, int eventId
						, String command
						, String msg) {
		this.parentUI = frmApplicationResourceOptimizer;
		String message = ResourceBundleHelper.getMessageString("Message.stopcollector");
		this.progress = new AROProgressDialog(parentUI, message);
		progress.setVisible(true);
		this.msg = msg;
		this.actionListeners = actionListeners;
		this.eventId = eventId;
		this.command = command;
	}
	

	//for android attenuator
	public AROCollectorSwingWorker(JFrame frmApplicationResourceOptimizer
									, List<ActionListener> actionListeners
									, int eventId
									, String command
									, IAroDevice device
									, String trace
									, Hashtable<String, Object> extraParams
									){
		
		this.parentUI = frmApplicationResourceOptimizer;
		String msg = ResourceBundleHelper.getMessageString("Message.startcollectorOnDevice");
		this.progress = new AROProgressDialog(parentUI, msg);
		progress.setVisible(true);
//		this.msg = msg;
		this.actionListeners = actionListeners;
		this.eventId = eventId;
		this.command = command;
		this.device = device;
		this.traceFolderPath = trace;
		this.extraParams = extraParams;

	}
	/**
	 * Start Collector iOS
	 * 
	 * @param IAROView 
	 * @param frmApplicationResourceOptimizer
	 * @param actionListeners
	 * @param eventId
	 * @param command
	 * @param udid
	 * @param trace
	 * @param videoCapture
	 */
	public AROCollectorSwingWorker(JFrame frmApplicationResourceOptimizer
									, List<ActionListener> actionListeners
									, int eventId
									, String command
									, IDataCollector iOsCollector
									, String udid
									, String trace
									, VideoOption videoCapture) {
		this.parentUI = frmApplicationResourceOptimizer;
		String msg = ResourceBundleHelper.getMessageString("Message.startcollectorOnDevice");
		this.progress = new AROProgressDialog(parentUI, msg);
		progress.setVisible(true);
		this.msg = msg;
		this.actionListeners = actionListeners;
		this.eventId = eventId;
		this.command = command;
		this.collector = iOsCollector;
		this.udid = udid;
		this.traceFolderPath = trace;
		this.videoCapture = videoCapture;
	}


	@Override
	protected T doInBackground() throws Exception {
		if (actionListeners != null) {
			for (ActionListener name : actionListeners) {
				if (eventId == 1) {
					// start collector for Android ?
					name.actionPerformed(new AROCollectorActionEvent(this, eventId, command, device, traceFolderPath, extraParams));//videoCapture, delayTime));
				} else if (eventId == 2) {
					// start collector for iOS ?
					name.actionPerformed(new AROCollectorActionEvent(this, eventId, command, collector, udid, traceFolderPath, videoCapture));
				} else {
					// stop collector
					name.actionPerformed(new ActionEvent(this, eventId, command));
				}
			}
		}
		return null;
	}
	
	@Override
	protected void done(){
		changeListeners = null;
		actionListeners = null;
		progress.dispose();

		if (msg != null) {
			MessageDialogFactory.showMessageDialog(parentUI, msg);
		}

	}

}
