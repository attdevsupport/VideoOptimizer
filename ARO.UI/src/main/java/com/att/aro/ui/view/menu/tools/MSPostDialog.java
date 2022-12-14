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
package com.att.aro.ui.view.menu.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.annotation.Nonnull;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.settings.impl.SettingsImpl;
import com.att.aro.core.tracemetadata.IMetaDataHelper;
import com.att.aro.core.tracemetadata.pojo.MetaDataModel;
import com.att.aro.core.upload.Compressor;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class MSPostDialog extends PostDialog{
	private static final long serialVersionUID = 1L;

	private static final String ZIPBASE64 = "trace.zip64"; //"trace.zb64";

	private JLabel msgLabel;

	private static final Logger LOG = LogManager.getLogger(MSPostDialog.class);

	IMetaDataHelper metaDataHelper = ContextAware.getAROConfigContext().getBean(IMetaDataHelper.class);
	SettingsImpl settings = ContextAware.getAROConfigContext().getBean(SettingsImpl.class);
	
	@Nonnull
	private String traceFolder;
	private MetaDataModel metaData;

	private Label description          ;
	private Label traceType            ;
	private Label targetedApp          ;
	private Label applicationProducer  ;
	private Label targetAppVer         ;
	private Label deviceOrientation    ;
	private Label phoneMake            ;
	private Label phoneModel           ;
	private Label os                   ;
	private Label osVersion            ;
	private Label startUTC             ;
	private Label traceSource          ;
	private Label traceOwner           ;

	private String[] excludedArray = new String[] {
			".DS_Store"
			, "trace.zip"
			, "trace.zip64"
			, TraceDataConst.FileName.VIDEO_MOV_FILE
			, TraceDataConst.FileName.VIDEO_MP4_FILE
			, "video_segments"
			, "thumbnailVideo.png"
			, "frameImage.png"
			, "searchPartLandScape.png"
			, "searchPartPortrait.png"
			, ""
	};

	public MSPostDialog() {
		LOG.debug("MSPostDialog()");
		traceFolder = settings.getAttribute("TRACE_PATH");
		createDialog();
		try {
			prepareUpload();
		} catch (Exception e) {
			LOG.error("Exception :",e);
		}
		setVisible(true);
		pack();
	}

	@Override
	public void createDialog() {
		LOG.debug("createDialog()");
		try {
			initialize();
		} catch (Exception e) {
			LOG.error("Exception :",e);
			
		}
		setUndecorated(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		setResizable(false);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setTitle(resourceBundle.getString("ms.post.dialog.title"));
		int height = screenSize.height;
		int width = screenSize.width;
		setBounds(width/3, height/20, 400, 150);
		
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		add(panel);
		
		GridBagConstraints constraint = new GridBagConstraints();
		constraint.fill = GridBagConstraints.HORIZONTAL;
		constraint.gridx = 1;
		constraint.gridy = 0;
		constraint.insets = new Insets(0, 10, 0, 0);
		constraint.weightx = 1;
		
		panel.add(getMessage(constraint));

		panel.add(getMetadataPanel());
		panel.add(getStatusPane(), BorderLayout.PAGE_END);
		panel.add(getButtonPanel(), BorderLayout.PAGE_END);
		this.setModalityType(ModalityType.APPLICATION_MODAL);
		pack();
		panel.setSize(panel.getPreferredSize());
		panel.validate();
	}
	
	private void initialize() throws Exception{
		
		LOG.debug("initialize()");
		traceFolder = settings.getAttribute("TRACE_PATH");
		jsonFile = new File(traceFolder, "metaData.json");
		metaData = metaDataHelper.loadMetaData(traceFolder);
		
	}
	
	private void prepareUpload() throws Exception {
		LOG.debug("prepareUpload()");
		updateStatus("Preparing " + traceFolder + " for upload...");
		urlKey = "traceHandlerURL";
		
		preparePayload(traceFolder, ZIPBASE64);
	}
	
	private void preparePayload(String traceFolder, String fileName) throws Exception{

		LOG.debug("preparePayload()");
		Compressor comp = new Compressor();
		comp.prepare(this, traceFolder, excludedArray, fileName);
		threadexecutor.executeFuture(comp);
		configButton(startButton, "Wait", false);
		panel.validate();

	}

	public File savePayload(String data) throws Exception{
		LOG.debug("savePayload()");
		File b64zipfile = filemanager.createFile(traceFolder, ZIPBASE64 );
		BufferedWriter out = new BufferedWriter(new FileWriter(b64zipfile));
		try {
			out.write(data);
		} catch (Exception e) {
			LOG.error("Exception", e);
		} finally {
			out.close();
		}
		return b64zipfile;
	}

	/**
	 * Creates MetadataModel display
	 * Note: some fields are commented out, and are for future use.
	 * @return
	 */
	private Component getMetadataPanel() {

		LOG.debug("getMetadataPanel()");
		Label descriptionLabel          = new Label(ResourceBundleHelper.getMessageString("metadata.field.description"));
		Label traceTypeLabel            = new Label(ResourceBundleHelper.getMessageString("metadata.field.traceType"));
		Label targetedAppLabel          = new Label(ResourceBundleHelper.getMessageString("metadata.field.targetedApp"));
		Label applicationProducerLabel  = new Label(ResourceBundleHelper.getMessageString("metadata.field.applicationProducer"));
		Label targetAppVerLabel         = new Label(ResourceBundleHelper.getMessageString("metadata.field.targetAppVer"));
		Label deviceOrientationLabel    = new Label(ResourceBundleHelper.getMessageString("metadata.field.deviceOrientation"));
		Label phoneMakeLabel            = new Label(ResourceBundleHelper.getMessageString("metadata.field.phoneMake"));
		Label phoneModelLabel           = new Label(ResourceBundleHelper.getMessageString("metadata.field.phoneModel"));
		Label osLabel                   = new Label(ResourceBundleHelper.getMessageString("metadata.field.os"));
		Label osVersionLabel            = new Label(ResourceBundleHelper.getMessageString("metadata.field.osVersion"));
		Label startUTCLabel             = new Label(ResourceBundleHelper.getMessageString("metadata.field.startUTC"));
		Label traceSourceLabel          = new Label(ResourceBundleHelper.getMessageString("metadata.field.traceSource"));
		Label traceOwnerLabel           = new Label(ResourceBundleHelper.getMessageString("metadata.field.traceOwner"));
		
		description                  = new Label(   metaData.getDescription            () );
		traceType                    = new Label(   metaData.getTraceType              () );
		targetedApp                  = new Label(   metaData.getTargetedApp            () );
		applicationProducer          = new Label(   metaData.getApplicationProducer    () );
		targetAppVer                 = new Label(   metaData.getTargetAppVer           () );
		deviceOrientation            = new Label(   metaData.getDeviceOrientation().toString());
		phoneMake                    = new Label(   metaData.getPhoneMake              () );
		phoneModel                   = new Label(   metaData.getPhoneModel             () );
		os                           = new Label(   metaData.getOs                     () );
		osVersion                    = new Label(   metaData.getOsVersion              () );
		startUTC                     = new Label(   metaData.getStartUTC			   () );
		traceSource                  = new Label(   metaData.getTraceSource            () );
		traceOwner                   = new Label(   metaData.getTraceOwner             () );
	
		int idx = 0;
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setFont(new Font(panel.getFont().getName(), Font.PLAIN, 16));

		panel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));
		idx = addLine(descriptionLabel        , description         , idx, panel);
		idx = addLine(traceTypeLabel          , traceType           , idx, panel);
		idx = addLine(targetedAppLabel        , targetedApp         , idx, panel);
		idx = addLine(applicationProducerLabel, applicationProducer , idx, panel);
		idx = addLine(targetAppVerLabel       , targetAppVer        , idx, panel);
		idx = addLine(deviceOrientationLabel  , deviceOrientation   , idx, panel);
		idx = addLine(phoneMakeLabel          , phoneMake           , idx, panel);
		idx = addLine(phoneModelLabel         , phoneModel          , idx, panel);
		idx = addLine(osLabel                 , os                  , idx, panel);
		idx = addLine(osVersionLabel          , osVersion           , idx, panel);
		idx = addLine(startUTCLabel           , startUTC            , idx, panel);
		idx = addLine(traceSourceLabel        , traceSource         , idx, panel);
		idx = addLine(traceOwnerLabel         , traceOwner          , idx, panel);
		return panel;
		
	}

	private int addLine(Label label, Label edit, int idx, JPanel panel) {
		panel.add(label, new GridBagConstraints(0, idx, 1, 1, 1.0, 0.2, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 10, 3, 0), 0, 0));
		panel.add(edit,  new GridBagConstraints(2, idx, 1, 1, 1.0, 0.2, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3,  0, 3, 0), 0, 0));

		return ++idx;
	}

	private JPanel getMessage(GridBagConstraints constraint) {
		msgLabel = new JLabel(getLabelMsg());
		msgLabel.setFont(new Font("Lucida Grande", Font.BOLD, 20));
		JPanel labelPanel = new JPanel();
		labelPanel.setLayout(new GridBagLayout());
		labelPanel.add(msgLabel, constraint);
		return labelPanel;
	}

	@Override
	public String getLabelMsg() {
		return resourceBundle.getString("ms.post.dialog.message");
	}
	
	@Override
	protected void disposeDialog() {
		super.disposeDialog();
		MSPostDialog.this.dispose();
	}

}
