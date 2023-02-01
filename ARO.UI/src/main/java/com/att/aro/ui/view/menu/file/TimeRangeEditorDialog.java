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
package com.att.aro.ui.view.menu.file;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jfree.ui.tabbedui.VerticalLayout;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.CollectionUtils;

import com.att.aro.core.AROConfig;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.impl.ExternalProcessRunnerImpl;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.fileio.impl.FileManagerImpl;
import com.att.aro.core.packetanalysis.pojo.TimeRange;
import com.att.aro.core.packetanalysis.pojo.TimeRange.TimeRangeType;
import com.att.aro.core.peripheral.ITimeRangeReadWrite;
import com.att.aro.core.peripheral.impl.TimeRangeReadWrite;
import com.att.aro.core.peripheral.pojo.TraceTimeRange;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;
import com.att.aro.core.util.VideoUtils;
import com.att.aro.core.video.pojo.Orientation;
import com.att.aro.core.videoanalysis.videoframe.FrameReceiver;
import com.att.aro.core.videoanalysis.videoframe.FrameRequest;
import com.att.aro.core.videoanalysis.videoframe.FrameRequest.JobType;
import com.att.aro.core.videoanalysis.videoframe.FrameStatus;
import com.att.aro.core.videoanalysis.videoframe.VideoFrameExtractor;
import com.att.aro.ui.commonui.ImagePanel;
import com.att.aro.ui.utils.ResourceBundleHelper;

import lombok.Getter;
import lombok.Setter;

public class TimeRangeEditorDialog extends JDialog implements FrameReceiver{
	private static final int IMAGE_HEIGHT = 346; // "normal" device X Y dimensions
	private static final Logger LOG = LogManager.getLogger(TimeRangeEditorDialog.class.getName());
	private static final long serialVersionUID = 1L;

	private int frameImagePanelLandscapeWidth = 380;
	private int frameImagePanelPortraitWidth = 240;
	private static final int SLIDER_SENSITIVITY = 25;

	private ApplicationContext context = new AnnotationConfigApplicationContext(AROConfig.class);
	private IFileManager fileManager = context.getBean(FileManagerImpl.class);
	
	private VideoFrameExtractor videoFrameExtractor = context.getBean("videoFrameExtractor", VideoFrameExtractor.class);
	private ITimeRangeReadWrite timeRangeReadWrite = context.getBean("timeRangeReadWrite", TimeRangeReadWrite.class);
	private IExternalProcessRunner externalProcessRunner = context.getBean(ExternalProcessRunnerImpl.class);
	private File traceFolder;
	
	@Getter
	private TraceTimeRange traceTimeRange;
	
	@Getter
	private TimeRange timeRange;
	private TimeRange timeRangeBU;
	
	private double traceTimeDuration;
	private double deviceVideoDuration;
	private double initialDeviceVideoOffset;
	private int deviceVideoWidth;
	private int deviceVideoHeight;
	private Orientation deviceOrientation;
	private double deviceVideoRatio;
	private int fWidth;
	private int fHeight;
	private double extractedFPS = 30;
	private Double deviceVideoNbFrames;

	private TreeMap<Double, BufferedImage> frameMap = new TreeMap<>();
	private FrameImagePanel startImagePanel;
	private FrameImagePanel endImagePanel;
	
	private JTextField configNameField = new JTextField();
	private JComboBox<TimeRangeType> autoLaunchComboBox = new JComboBox<TimeRangeType>();
	private JTextField startAnalysisTextField = new JTextField();
	private JTextField endAnalysisTextField = new JTextField();
	private JSlider startSlider = new JSlider();
	private JSlider endSlider = new JSlider();
	private JPanel editorJPanel;
	private JPanel chooserJPanel;
	private Dimension editorDimension;
	private Dimension chooserDimension;
	@Getter
	@Setter
	private boolean continueWithAnalyze = false;
	private JPanel editButtonPanel;
	private int dialogHeight;
	private int dialogWidth;
	private JDialog splash;
	private JTextField sliderAlert;
	private FontRenderContext frc;

	private double getStartAnalysis() {
		return StringParse.stringToDouble(startAnalysisTextField.getText(), 0);
	}
	
	private double getEndAnalysis() {
		return StringParse.stringToDouble(endAnalysisTextField.getText(), 0);
	}

	public TimeRangeEditorDialog(File traceFolder, boolean analyzeOnExit, JDialog splash) throws Exception {
		LOG.debug("TimeRangeDialog :" + traceFolder.toString());
		this.splash = splash;
		this.traceFolder = traceFolder;
		autoLaunchComboBox.addItem(TimeRangeType.DEFAULT);
		autoLaunchComboBox.addItem(TimeRangeType.MANUAL);
		autoLaunchComboBox.getSize();
		autoLaunchComboBox.setPreferredSize(new Dimension(getStringFontWidth(autoLaunchComboBox.getFont(), TimeRangeType.DEFAULT.toString()), autoLaunchComboBox.getSize().height));
		autoLaunchComboBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				TimeRange originalDefault;
				if (TimeRangeType.DEFAULT.equals(autoLaunchComboBox.getSelectedItem())
					&& (originalDefault = locateEntryWithAutoLaunch()) != null) {
					// the actual swap of DEFAULT will happen in saveTraceTimeRange()
					LOG.debug(String.format("%s will be changed from DEFAULT to MANUAL", originalDefault.toString()));
				}
			}
		});
		
		setContinueWithAnalyze(analyzeOnExit);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				destroy();
				super.windowClosing(e);
			}
		});

		initialize();
		
		editorDimension = calculateEditorDimension();
		
		pack();
		setResizable(true);
		Runnable doMore = () -> {
			try {
				doMore();
			} catch (Exception e1) {
				LOG.error(e1);
			}
		};
		new Thread(doMore,"doTheRest").start();
	}

	public Dimension calculateEditorDimension() {
		//determine size for the editor
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		int minLandscapeH = 438;
		int minLandscapeW = 893;
		int minPortraitH = 585;
		int minPortraitW = 893;
		if (Util.isWindowsOS()) {
			minLandscapeH = 430;
			minLandscapeW = 831;
			minPortraitH = 573;
			minPortraitW = 813;
		} else if (Util.isMacOS()) {
			minLandscapeH = 398;
			minLandscapeW = 815;
			minPortraitH = 539;
			minPortraitW = 751;
		}
			
		if (deviceOrientation == Orientation.LANDSCAPE) {
			// max based on screen size
			dialogHeight = (int) (screenSize.height / 3.000);
			dialogWidth = (int) (dialogHeight * 2.065);
			// enforce a minimum size
			dialogHeight = dialogHeight < minLandscapeH ? minLandscapeH : dialogHeight;
			dialogWidth = dialogWidth < minLandscapeW ? minLandscapeW : dialogWidth;
		} else {
			// max based on screen size
			dialogHeight = (int) (screenSize.height / 2.330);
			dialogWidth = (int) (dialogHeight * 1.398);
			// enforce a minimum size
			dialogHeight = dialogHeight < minPortraitH ? minPortraitH : dialogHeight;
			dialogWidth = dialogWidth < minPortraitW ? minPortraitW : dialogWidth;
		}

		LOG.debug(deviceOrientation);
		LOG.debug("Computer Screen Size:" + screenSize);
		return new Dimension(dialogWidth, dialogHeight);
	}

	public void doMore() throws Exception {

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		if ((traceTimeRange = loadTraceTimeRange()) != null) {
			chooser(false);
			chooserDimension = getSize();
		} else {
			createEntry();
			editorDimension = new Dimension(getSize());
			editorDimension.setSize(editorDimension.width, editorDimension.height + startImagePanel.getImageHeight() - IMAGE_HEIGHT);
			setMinimumSize(editorDimension);
		}
		if (splash != null) {
			splash.dispose();
			splash = null;
		}
	}

	private void chooser(Boolean needUpdate) {
		setTitle("Time Range Chooser");
		if (needUpdate) {
			chooserJPanel = null;
		}
		if (editorJPanel != null && editorJPanel.isVisible()) {
			editorJPanel.setVisible(false);
		}

		if (chooserJPanel == null) {
			add(chooserJPanel = createChooserDialog());
			chooserJPanel.setSize(new Dimension(830, Util.getAdjustedHeight(386)));
			chooserDimension = chooserJPanel.getSize();
		}
		chooserJPanel.setVisible(true);
		setPreferredSize(chooserDimension);
		pack();
	}

	void createEntry() {
		configNameField.setText("");
		autoLaunchComboBox.setSelectedItem(TimeRangeType.MANUAL);
		setTitle("Time Range Setup");
		if (editorJPanel == null) {
			add(editorJPanel = createEditorDialog());
		} 
		if (editorDimension != null) {
			setPreferredSize(editorDimension);
		}
		editorJPanel.setVisible(true);
		pack();
	}
	
	public void edit(TimeRange timeRange) {
		timeRangeBU = timeRange;
		if (chooserJPanel != null && chooserJPanel.isVisible()) {
			chooserJPanel.setVisible(false);
		}
		setTitle("Time Range Editor");
		if (editorJPanel == null) {
			add(editorJPanel = createEditorDialog());
		}
		loadFields(timeRange); 
		if (editorDimension != null) {
			setPreferredSize(editorDimension);
		}
		editorJPanel.setVisible(true);
		pack();
		
		startSlider.setValue((int) (getStartAnalysis() * SLIDER_SENSITIVITY));
		endSlider.setValue((int) (getEndAnalysis() * SLIDER_SENSITIVITY));
		
	}

	public void cancel() {
		timeRange = null;
		setContinueWithAnalyze(false);
	}
	
	public void delete(TimeRange timeRange) {

		if (traceTimeRange != null && !CollectionUtils.isEmpty(traceTimeRange.getTimeRangeList())) {
			traceTimeRange.getTimeRangeList().remove(timeRange);
			try {
				timeRangeReadWrite.save(traceFolder, traceTimeRange);
			} catch (Exception e) {
				LOG.error("Failed to save time-range.json, ", e);
			}
		}
		if (traceTimeRange == null || traceTimeRange.getTimeRangeList().isEmpty()) {
			createEntry();
		} else {
			chooser(true);
		}
	}

	private JPanel createChooserDialog() {
		JPanel chooserPanel = new TimeRangeChooser(this, traceTimeRange);
		return chooserPanel;
	}
	
	// dialog
	private JPanel createEditorDialog() {
		if (editorJPanel == null) {
			editorJPanel = new JPanel();
			editorJPanel.setLayout(new VerticalLayout());
			editorJPanel.add(createContentPanel());
		}
		editorJPanel.setVisible(true);
		return editorJPanel;
	}

	private Component createContentPanel() {
		JPanel contentPane = new JPanel(new VerticalLayout());
		contentPane.add(getFrameViews());
		contentPane.add(buildConfigGroup());
		contentPane.add(createEditButtonPane());
		return contentPane;
	}
	
	/**
	 * Count entries set to TimeRangeType.DEFAULT
	 * 
	 * @return count
	 */
	protected long countEntriesWithAutoLaunch(String excludeItem) {
		if (traceTimeRange == null || configNameField == null || configNameField.getText() == null) {
			return 0;
		}
		return traceTimeRange.getTimeRangeList().stream()
				.filter(a -> a.getTimeRangeType().equals(TimeRangeType.DEFAULT))
				.filter(p -> !p.getTitle().equals(excludeItem))
				.count();
	}
	
	protected TimeRange locateEntryWithAutoLaunch() {
		if (traceTimeRange == null || configNameField == null || configNameField.getText() == null) {
			return null;
		}
		Optional<TimeRange> foundEntry = traceTimeRange.getTimeRangeList().stream()
				.filter(a -> a.getTimeRangeType().equals(TimeRangeType.DEFAULT))
				.filter(p -> !p.getTitle().equals(configNameField.getText()))
				.findFirst();
		return foundEntry.isPresent() ? foundEntry.get() : null;
	}

	private boolean collisionTest(String title) {
		if (traceTimeRange == null) {
			return false;
		}
		return traceTimeRange.getTimeRangeList().stream()
				.filter(p -> p.getTitle().equals(title)).findFirst().isPresent();
	}

	private TimeRange locateTimeRange(String title) {
		if (traceTimeRange != null && !CollectionUtils.isEmpty(traceTimeRange.getTimeRangeList())) {
			Optional<TimeRange> trSection = traceTimeRange.getTimeRangeList().stream()
					.filter(p -> p.getTitle().equals(configNameField.getText())).findFirst();
			if (trSection.isPresent()) {
				return trSection.get();
			}
		}
		return null;
	}

	private TraceTimeRange loadTraceTimeRange() {
		return timeRangeReadWrite.readData(traceFolder);
	}
	
	private void loadFields(TimeRange timeRange) {
		configNameField           .setText(timeRange.getTitle());
		startAnalysisTextField    .setText(String.format("% 8.03f", timeRange.getBeginTime()));
		endAnalysisTextField      .setText(String.format("% 8.03f", timeRange.getEndTime()));
		autoLaunchComboBox        .setSelectedItem(timeRange.getTimeRangeType());
	}
	
	private TimeRange saveTraceTimeRange() {
		if (traceTimeRange == null) {
			traceTimeRange = new TraceTimeRange();
		}
		if (traceTimeRange.getTimeRangeList().isEmpty()) {
			timeRange = new TimeRange("FULL", TimeRangeType.DEFAULT, 0, traceTimeDuration);
			traceTimeRange.getTimeRangeList().add(timeRange);
		} 
		LOG.debug("Save trace range: " + traceTimeRange);
		TimeRange timeRange = null;
		
		if (autoLaunchComboBox.getSelectedItem().equals(TimeRangeType.DEFAULT)) {
			TimeRange prevDefaultTimeRange = locateEntryWithAutoLaunch();
			if (prevDefaultTimeRange!=null && !prevDefaultTimeRange.getTitle().equals(configNameField.getText())) {
				prevDefaultTimeRange.setTimeRangeType(TimeRangeType.MANUAL);
			}
		}
		int actionCode;
		if (timeRangeBU != null && !timeRangeBU.getTitle().equals(configNameField.getText())) {
			actionCode = ask();
			if (actionCode != 0) { // 1 = No, 2 = Cancel
				if (timeRangeBU != null && !timeRangeBU.getTitle().equals(configNameField.getText())) {
					configNameField.setText(timeRangeBU.getTitle());
					configNameField.grabFocus();
					timeRangeBU = null;
					return null;
				}
			}
		}
		try {
			timeRange = locateTimeRange(configNameField.getText());
			timeRange = loadTimeRange(timeRange);
			timeRangeReadWrite.save(traceFolder, traceTimeRange);

		} catch (Exception e) {
			LOG.error("Failed to save TimeRange data:", e);
		}
		timeRangeBU = null;
		return timeRange;
	}

	private TimeRange loadTimeRange(TimeRange timeRange) {
		if (timeRange == null) {
			timeRange = new TimeRange();
		}
		timeRange.setTitle(configNameField.getText());
		timeRange.setBeginTime(Double.valueOf(startAnalysisTextField.getText()));
		timeRange.setEndTime(Double.valueOf(endAnalysisTextField.getText()));
		timeRange.setTimeRangeType((TimeRangeType) autoLaunchComboBox.getSelectedItem());
		return timeRange;
	}
	
	private boolean validateInput() {
		return (!StringUtils.isEmpty(configNameField.getText())
				&& !StringUtils.isEmpty(startAnalysisTextField.getText())
				&& !StringUtils.isEmpty(endAnalysisTextField.getText())
				&& autoLaunchComboBox.getSelectedItem() != null
				&& validateTimeSpan() > 0);
	}
	
	private double validateTimeSpan() {
		TimeRange temp = null;
		temp = loadTimeRange(temp);
		return (temp.getEndTime() - temp.getBeginTime());
	}

	// actions
	protected void analyze(TimeRange timeRangeObj) {
		if (timeRangeObj == null || !StringUtils.isEmpty(timeRangeObj.getTitle())) {
			this.timeRange = timeRangeObj;
		} else if (StringUtils.isEmpty(timeRangeObj.getTitle())) {
			timeRangeObj.setTitle("Manual");
			this.timeRange = timeRangeObj;
		}
		LOG.debug("analyze  " + timeRangeObj);
		setContinueWithAnalyze(true);
	}
	
	// top of  Dialog
	private Component getFrameViews() {
		JPanel framePanel = new JPanel(new FlowLayout());
		Image image = (new ImageIcon(getClass().getResource(ResourceBundleHelper.getImageString("ImageBasePath") 
				+ ResourceBundleHelper.getImageString("Image.blackScreen"))))
				.getImage();
		// image dimension is Dimension(360, 640)
		startImagePanel = new FrameImagePanel(image, deviceOrientation);
		endImagePanel = new FrameImagePanel(image, deviceOrientation);
		
		framePanel.add(createFramePanel(startImagePanel, startAnalysisTextField, "Start analysis:", 0, startSlider));
		framePanel.add(createFramePanel(endImagePanel, endAnalysisTextField, "End analysis:", deviceVideoDuration, endSlider));

		startSlider.addChangeListener(createTimeRangeSliderListener(startImagePanel, startAnalysisTextField, endSlider, startSlider, true));
		endSlider.addChangeListener(createTimeRangeSliderListener(endImagePanel, endAnalysisTextField, startSlider, endSlider ,false));
		
		return framePanel;
	}
	
	/**
	 * Builds a Panel with
	 *   a image frame for images
	 *   and below that a Slider and time in seconds display
	 * 
	 * FocusListener requires two JTextFields being used, one of which must be named startAnalysisTextField
	 * 
	 * @param imagePanel
	 * @param textField
	 * @param textFieldLabel
	 * @param position
	 * @return JPanel
	 */
	private JPanel createFramePanel(FrameImagePanel imagePanel, JTextField textField, String textFieldLabel, double position, JSlider slider) {
		JPanel panel = new JPanel(new VerticalLayout());

		imagePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		JPanel tsp = new JPanel(new FlowLayout());
		int max = (int) Math.ceil(deviceVideoDuration) * SLIDER_SENSITIVITY;
		int posIndex = (int) Math.ceil(position) * SLIDER_SENSITIVITY;
		tsp.add(configureSlider(slider, JSlider.HORIZONTAL, 0, max , posIndex));
		tsp.add(wrapAndLabelComponent(textField, textFieldLabel, 100));
		setTimeJTextField(textField, position);
		textField.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				Double value = StringParse.stringToDouble(textField.getText(), 0);
				if (textField == startAnalysisTextField) {
					Double endTime = getEndAnalysis();
					value = value > endTime ? endTime : value;
				} else {
					Double startTime = getStartAnalysis();
					value = value < startTime ? startTime : value;
				}
				setTimeJTextField(textField, value);
				slider.setValue((int) (value * SLIDER_SENSITIVITY));
			}
			
			@Override public void focusGained(FocusEvent e) {}
		});
		panel.add(imagePanel);
		panel.add(tsp);
		return panel;
	}

	private ChangeListener createTimeRangeSliderListener(FrameImagePanel imagePanel, JTextField timeField, JSlider sliderAssociate, JSlider thisSlider, boolean placement) {
		return new ChangeListener() {
			JSlider oSlider = sliderAssociate;
			boolean high = placement;
			
			@Override
			public void stateChanged(ChangeEvent e) {
				int value = ((JSlider) e.getSource()).getValue();
				if (!high) {
					if (value < oSlider.getValue()) {
						setSliderAlert(oSlider.getValue()-value);
					} else {
						clearSliderAlert();
					}
				} else {
					if (value > oSlider.getValue()) {
						setSliderAlert(value - oSlider.getValue());
					} else {
						clearSliderAlert();
					}
				}
				imagePanel.setSeconds(value);
				imagePanel.setImage(requestFrame(imagePanel.getSeconds()));
				setTimeJTextField(timeField, imagePanel.getSeconds());
				timeField.revalidate();
			}

		};
	}
	
	private void clearSliderAlert() {
		sliderAlert.setVisible(false);
		sliderAlert.setText("");
	}

	private void setSliderAlert(int secondsOfOverlap) {
		sliderAlert.setVisible(true);
		sliderAlert.setText("WARNING overlap");
		this.validate();
		pack();
	}

	public void show(double timeStamp, Double key, Double priorKey, double priorGap, double frameNumber) {
		LOG.debug(String.format("\ntimeStamp: %.3f, key: %.3f, frameNumber: %.3f, priorKey: %.3f, priorGap: %.3f", timeStamp, key, frameNumber, priorKey, priorGap));
	}

	private double calcFrameToSeconds(double frameNumber) {
		return (frameNumber / extractedFPS);
	}

	public void setTimeJTextField(JTextField timeField, double timestamp) {
		timeField.setText(String.format("% 8.03f", timestamp));
	}

	/**
	 * Configure a slider object
	 * 
	 * @param slider
	 * @param orientation
	 * @param minValue
	 * @param maxValue
	 * @param value
	 * @return
	 */
	public JSlider configureSlider(JSlider slider, int orientation, int minValue, int maxValue, int value) {
		BoundedRangeModel sliderModel = slider.getModel();
		sliderModel.setMinimum(minValue);
		sliderModel.setMaximum(maxValue);
		sliderModel.setValue(value);
		slider.setOrientation(orientation);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		return slider;
	}

	private JPanel buildConfigGroup() {
		JPanel framePanel = new JPanel(new FlowLayout());
		configNameField.setSize(new Dimension(200, 20));

		framePanel.add(wrapAndLabelComponent((sliderAlert = new JTextField("")), "", 200));
		sliderAlert.setVisible(false);
		sliderAlert.setDisabledTextColor(Color.RED);
		sliderAlert.setFont(new Font("Dialog", Font.BOLD, 14));
		sliderAlert.setEditable(false);
		sliderAlert.setBackground(Color.LIGHT_GRAY);
		framePanel.add(wrapAndLabelComponent(configNameField, "Configuration name:", 200));
		framePanel.add(wrapAndLabelComponent(autoLaunchComboBox, "Auto Launch:", 110));
		return framePanel;
	}

	private JPanel wrapAndLabelComponent(Component component, String fieldLabel, int width) {
		JPanel panel = new JPanel(new BorderLayout());
		component.setPreferredSize(new Dimension(width, Util.getAdjustedHeight((int) component.getPreferredSize().getHeight())));
		panel.add(new JLabel(fieldLabel), BorderLayout.WEST);
		panel.add(component, BorderLayout.EAST);
		return panel;
	}

	/**
	 * 0 = Yes
	 * 1 = No
	 * 2 = Cancel
	 * 
	 * @return
	 */
	protected int ask() {
		int answer = JOptionPane.showConfirmDialog((Component) this,
				"Replace " + configNameField.getText() + "?");
		return answer;
	}

	protected void userErrorMessage(String message) {
		JOptionPane.showMessageDialog((Component) this, message);
	}

	class FrameImagePanel extends JPanel {

		private static final long serialVersionUID = 1L;
		Image image;
		private int imageWidth = frameImagePanelLandscapeWidth;
		private int imageHeight = (int)(imageWidth * deviceVideoRatio);
		
		private double seconds;
		private Dimension dimn;
		private ImagePanel imagePanel;
		private Orientation orientation;
		
		@Override
		public String toString() {
			StringBuilder out = new StringBuilder("FrameImagePanel");
			if (dimn != null) {
				out.append(String.format("\n Dimensions: w=%d, h=%d", dimn.width, dimn.height));
				out.append(String.format("\n Orientation: %s", orientation));
				out.append(String.format("\n seconds: %.0f", seconds));
			}
			return out.toString();
		}
		
		public FrameImagePanel(Image image, Orientation orientation) {
			configure(image, orientation);
		}
		
		public void configure(Image image, Orientation orientation) {
			this.orientation = orientation;
			imagePanel = new ImagePanel(image);
			if (orientation.equals(Orientation.LANDSCAPE)) {
				imageWidth = frameImagePanelLandscapeWidth;
				imageHeight = (int) (imageWidth * deviceVideoRatio);
				dimn = new Dimension((int) (frameImagePanelLandscapeWidth), (int) (frameImagePanelLandscapeWidth * deviceVideoRatio));
			} else {
				imageWidth = frameImagePanelPortraitWidth;
				imageHeight = (int) (frameImagePanelPortraitWidth * deviceVideoRatio);
				dimn = new Dimension((int) (frameImagePanelPortraitWidth), (int) (frameImagePanelPortraitWidth * deviceVideoRatio));
			}
			
			imagePanel.setPreferredSize(dimn);
			imagePanel.setMinimumSize(dimn);
			imagePanel.setMaximumSize(dimn);
			imagePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			add(imagePanel);
		}

		public int getImageHeight() {
			return imagePanel.getHeight();
		}
		
		public void setImage(BufferedImage bImage) {
			if (bImage != null) {
				if (isLandscape()) {
					imagePanel.setImage(bImage.getScaledInstance(imageWidth, imageHeight, Image.SCALE_DEFAULT));
				} else {
					imagePanel.setImage(bImage.getScaledInstance((int) (dimn.height/deviceVideoRatio), (int) (dimn.height), Image.SCALE_DEFAULT));
				}
				repaint();
			}
		}

		private boolean isLandscape() {
			return orientation.equals(Orientation.LANDSCAPE);
		}

		public double getSeconds() {
			return seconds;
		}

		public void setSeconds(int value) {
			seconds = (double)value / SLIDER_SENSITIVITY;
		}

		public void updateFrame() {
			Double key = seconds >= initialDeviceVideoOffset ? ((seconds - initialDeviceVideoOffset) * extractedFPS) : 0;
			Double priorKey = getFloorKey(key, 0D);
			Double nextKey = getCeilingKey(key, -1D);
			Double closestKey;

			closestKey = (key - priorKey < nextKey - key) ? priorKey : nextKey;
			BufferedImage tempImage = frameMap.get(closestKey);
			if (tempImage != null) {
				setImage(tempImage);
			}
		}
	}

	/**
	 * retrieve a frame based on seconds * deviceFrameRate
	 * 
	 * @param videoEvent
	 * @return
	 */
	private BufferedImage requestFrame(double timeStamp) {
		BufferedImage frame = null;
		if (!CollectionUtils.isEmpty(frameMap)) {

			Double key = timeStamp >= initialDeviceVideoOffset ? ((timeStamp - initialDeviceVideoOffset) * extractedFPS) : 0;
			Double priorKey = getFloorKey(key, 0D);
			Double nextKey = getCeilingKey(key, -1D);
			Double closeKey;

			double priorGap = 100;
			double nextGap = 100;
			double frameNumber = -1;
			if (priorKey == null || (priorGap = key - priorKey) > 10) {
				show(timeStamp, key, priorKey, priorGap, frameNumber);
				frameNumber = (key - (priorGap > 99 ? 100 : priorGap));
				collectFrames(new FrameRequest(JobType.COLLECT_FRAMES, calcFrameToSeconds(frameNumber), (int) (nextGap > 100 ? 100 : nextGap), key, this));
			}
			if (nextKey == null || (nextGap = nextKey - key) > 10) {
				collectFrames(new FrameRequest(JobType.COLLECT_FRAMES
						, timeStamp
						, (int) (nextGap > 100 ? 100 : nextGap)	// number of frames being requested
						, key
						, this
				));
			}

			priorKey = getFloorKey(key, 0D);
			nextKey = getCeilingKey(key, -1D);
			closeKey = (priorGap < nextGap) ? priorKey : nextKey;

			frame = frameMap.get(closeKey);
		}
		return frame;
	}
	
	@Override
	public void receiveResults(Class<?> sender, FrameStatus result) {
		LOG.info(String.format("%s :%s, results:%s", sender.getSimpleName(), result.isSuccess() ? "received" : "nothing", result.getAddedCount()));

		if (result.isSuccess() || result.getAddedCount() > 0) {
			if (result.getFrameRequest().getTargetFrame() != null) {
				// checks if need to Recover from missing frames
				FrameRequest frameRequest = result.getFrameRequest();
				int frameOffset;
				if (Math.abs((frameOffset = frameRequest.getTargetFrame().intValue() - result.getFirstFrame())) > 1) {
					Double frameKey = Math.floor(frameRequest.getTargetFrame());
					Double ceiling = frameMap.ceilingKey(Math.floor(frameRequest.getTargetFrame()));
					Double floor = frameMap.floorKey(Math.floor(frameRequest.getTargetFrame()));
					if (!frameKey.equals(ceiling) && !frameKey.equals(floor)) {
						frameOffset = frameRequest.getTargetFrame().intValue() - result.getFirstFrame();
						frameRequest.setStartTimeStamp(result.getFrameRequest().getStartTimeStamp() - frameOffset / extractedFPS);
						frameRequest.setTryCount(frameRequest.getTryCount() + 1);
						collectFrames(frameRequest);
					}
				}
			}
			if (startImagePanel != null) {
				startImagePanel.updateFrame();
				if (endImagePanel != null) {
					endImagePanel.updateFrame();
				}
			}

		} else {
			LOG.error("failed frame request:" + result);
		}
	}

	/**
	 * Get the ceiling key, with provisions for a defaultKey. the defaultKey has a magic value of -1D which causes lastKey to be sent if ceilingKey is null
	 * 
	 * @param key
	 * @param defaultKey
	 * @return ceilingKey, or defaultKey or lastKey()
	 */
	public Double getCeilingKey(Double key, Double defaultKey) {

		Double foundKey = frameMap.ceilingKey(key);
		if (foundKey == null) {
			foundKey = defaultKey == -1D ? frameMap.lastKey() : defaultKey;
		}
		return foundKey;
	}

	public Double getFloorKey(Double key, Double defaultKey) {
		Double foundKey = frameMap.floorKey(key);
		if (foundKey == null) {
			return defaultKey;
		}
		return foundKey;
	}
	
	// button panel & ActionListener
	private Component createEditButtonPane() {
		if (editButtonPanel == null) {
			editButtonPanel = new JPanel(new BorderLayout());
			editButtonPanel.setPreferredSize(new Dimension(Util.getAdjustedWidth(600), Util.getAdjustedHeight(40)));

			JPanel btnPanel = new JPanel();
			btnPanel.setLayout(new FlowLayout());

			// build the 'Save' JButton
			btnPanel.add(buildButton("Save"));
			btnPanel.add(buildButton("Save & Analyze"));
			btnPanel.add(buildButton("Analyze"));
			btnPanel.add(buildButton("Cancel"));
			btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

			editButtonPanel.add(btnPanel, BorderLayout.EAST);
		}
		return editButtonPanel;
	}

	private JButton buildButton(String buttonName) {
		JButton button = new JButton(buttonName);
		button.setFont(new Font("Dialog", Font.BOLD, 14));
		button.setPreferredSize(new Dimension(Util.getAdjustedWidth(140),Util.getAdjustedHeight(20)));
		button.addActionListener(createButtonListener());
		return button;
	}

	public ActionListener createButtonListener() {
		return new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				String button = e.getActionCommand();
				LOG.debug("Selected: " + button);
				
				switch (button) {
				case "Save":
					editorDimension = getSize(); // Landscape [width=856,height=425], Portrait 
					if (validateInput()) {
						cleanup();
						saveTraceTimeRange();
						chooser(true);
					} else {
						explainWhyCannotSave();
					}
					break;
				case "Save & Analyze":
					if (validateInput()) {
						cleanup();
						analyze(saveTraceTimeRange());
						destroy();
					} else {
						explainWhyCannotSave();
					}
					break;
				case "Analyze":
					if (!StringUtils.isEmpty(startAnalysisTextField.getText())
							&& !StringUtils.isEmpty(endAnalysisTextField.getText()) && validateTimeSpan() > 0) {
						timeRange = loadTimeRange(null);
						timeRange.setTimeRangeType(TimeRangeType.MANUAL);
						analyze(timeRange);
					} else {
						explainWhyCannotUse();
						break;
					}
					destroy();
					break;
				case "Cancel":
					LOG.debug("Editor Size:" + getSize());
					if (chooserJPanel != null && traceTimeRange != null
							&& !traceTimeRange.getTimeRangeList().isEmpty()) {
						chooser(false);
					} else {
						cancel();
						destroy();
					}
					break;
				default:
					destroy();
					break;
				}
			}


		};
	}

	private void explainWhyCannotSave() {
		if (configNameField.getText().isEmpty()) {
			userErrorMessage("Invalid: cannot be saved without a name");
		} 
		explainWhyCannotUse();
	}
	
	private void explainWhyCannotUse() {
		double validation = validateTimeSpan();
		if (validation == 0) {
			userErrorMessage("Invalid: Time range is zero, there can be nothing to analyze");
		} else if (validation < 0) {
			userErrorMessage("Invalid: Time range is negative, start must be before the end");
		} else {
			userErrorMessage("Invalid settings");
		}
	}
				
	void destroy() {
		cleanup();
		dispose();
		System.gc();
	}

	public void cleanup() {
		if (videoFrameExtractor != null) {
			LOG.debug("shutdown videoFrameExtractor");
			videoFrameExtractor.shutdown();
		}
	}

	private boolean initialize() throws Exception{
		if (traceFolder.exists()) {
			File videoFrameFolder = fileManager.createFile(traceFolder, "tempVideoFrameFolder");
			fileManager.mkDir(videoFrameFolder);
			FileUtils.cleanDirectory(videoFrameFolder);
			loadVideoData(videoFrameFolder);
			return true;
		}
		return false;

	}
	
	/**
	 * Determine width needed for a given font and string
	 * 
	 * @param font
	 * @param string
	 * @return
	 */
	private int getStringFontWidth(Font font, String string) {
		if (frc == null) {
			AffineTransform affinetransform = new AffineTransform();
			frc = new FontRenderContext(affinetransform, true, true);
		}
		return (int) (font.getStringBounds(string, frc).getWidth());
	}

	/**
	 * extract video data via ffprobe 
	 * 
	 * @param deviceVideoPath
	 * @return ffprobe results
	 * @throws Exception
	 */
	private String extractDeviceVideoInfo(File deviceVideoPath) throws Exception {
		String cmd = String.format("%s -i \"%s\" %s", Util.getFFPROBE(), deviceVideoPath, " -v quiet -show_entries stream=height,width,nb_frames,duration,codec_name");

		String results = externalProcessRunner.executeCmd(cmd, true, true);
		results = results.replaceAll("[\n\r]", " ").replaceAll("  ", " ").replaceAll("] ", "]").replaceAll(" \\[", "\\[");
		
		if (!results.contains("STREAM")) {
			throw new Exception("Error executing ffprobe <" + cmd + ">" + results);
		}
		return results;
	}
	
	/**
	 * <pre>
	 * Locate the video with the longest duration, video.mov or video.mp4.
	 * Extract duration, width & height
	 * Calculate deviceOrientation, deviceVideoRatio
	 * Assign frameImagePanelPanelPortrait value
	 * 
	 * @param videoFrameFolder
	 * @throws Exception
	 */
	private void loadVideoData(File videoFrameFolder) throws Exception {
		File movVideoPath = null;
		File mp4VideoPath = null;		
		
		String[] videoFiles = VideoUtils.validateFolder(traceFolder, VideoUtils.VIDEO, VideoUtils.VIDEO_EXTENTIONS).get(VideoUtils.VIDEO);
		if (videoFiles == null || videoFiles.length == 0) {
			splash.dispose();
			throw new Exception("No Video Files found");
		}
		if (videoFiles.length == 1) {
			mp4VideoPath = fileManager.createFile(traceFolder, videoFiles[0]);
		} else if (videoFiles.length == 2) {
			movVideoPath = fileManager.createFile(traceFolder, videoFiles[0]);
			mp4VideoPath = fileManager.createFile(traceFolder, videoFiles[1]);
		} else {
			String message = "Wrong Video File count" + videoFiles.length;
			splash.dispose();
			throw new Exception(message);
		}
		
		File deviceVideoPath = null;
		Double durationMOV = 0D;
		String resultsMOV = "";
		Double durationMP4 = 0D;
		String resultsMP4 = "";
		
		if (mp4VideoPath.exists()) {
			resultsMP4 = extractDeviceVideoInfo(mp4VideoPath);
			durationMP4 = StringParse.findLabeledDoubleFromString("duration=", " ", resultsMP4);
		}
		if (movVideoPath != null && movVideoPath.exists()) {
			resultsMOV = extractDeviceVideoInfo(movVideoPath);
			durationMOV = StringParse.findLabeledDoubleFromString("duration=", " ", resultsMOV);
		}

		String results;
		
		if (durationMOV - 30 > durationMP4) {
			results = resultsMOV;
			deviceVideoPath = movVideoPath;
			frameImagePanelPortraitWidth = 165;
		} else {
			results = resultsMP4;
			deviceVideoPath = mp4VideoPath;
			frameImagePanelPortraitWidth = 195;
		}
		
		if (deviceVideoPath == null || !deviceVideoPath.exists() || results.isEmpty()) {
			throw new Exception("No Device Video file");
		}
		
		String streamSection;
		String fieldString = "[STREAM]";
		String delimiter = "\\[/STREAM\\]";
		results = results.replaceAll(fieldString + " ", fieldString).replaceAll(" " + delimiter, delimiter);

		while (StringUtils.isNotEmpty(streamSection = StringParse.findLabeledDataFromString(fieldString, delimiter, results))) {
			results = results.substring(fieldString.length() * 2 + 1 + streamSection.length());
			Double height = StringParse.findLabeledDoubleFromString("height=", " ", streamSection);
			if (height == null) { // only video has dimensions such as height & width
				continue;
			} else {
				Double width = StringParse.findLabeledDoubleFromString("width=", " ", streamSection);
				if (width == null) {
					continue;
				}
				
				deviceVideoDuration = StringParse.findLabeledDoubleFromString("duration=", " ", streamSection);
				initialDeviceVideoOffset = 0;
				
				// we have a video stream
				deviceVideoWidth = width.intValue();
				deviceVideoHeight = height.intValue();
				
				if (height < width) {
					deviceOrientation = Orientation.LANDSCAPE;
					deviceVideoRatio = height / width;
				} else {
					deviceOrientation = Orientation.PORTRAIT;
					deviceVideoRatio = width / height;
				}
				deviceVideoRatio = height / width;
				
				fHeight = height.intValue();
				fWidth = (int) ((double) fHeight / deviceVideoRatio);
				
			}
			deviceVideoNbFrames = StringParse.findLabeledDoubleFromString("nb_frames=", " ", streamSection);
			Double duration = StringParse.findLabeledDoubleFromString("duration=", " ", streamSection);
			if (duration != null) {
				deviceVideoDuration = Math.max(deviceVideoDuration, duration);
			}
		}

		try {
			videoFrameExtractor.initialize(videoFrameFolder.toString(), deviceVideoPath.toString(), frameMap, fWidth, fHeight);
			extractedFPS = calculateFPS(30);
			preloadFrames(100, this);
		} catch (Exception e) {
			LOG.error("VideoFrameExtractor Exception:" + e.getMessage());
		}

		Double[] timeFileValues = getTraceTime();
		traceTimeDuration = (timeFileValues[1] - timeFileValues[0]) / 1000;
		double videoStartTime = getVideoTime();
		initialDeviceVideoOffset = (videoStartTime - timeFileValues[0]) / 1000;

		LOG.info("timeDuration :" + traceTimeDuration);
		LOG.info("videoDuration :" + deviceVideoDuration);
		LOG.info("videoOffset :" + initialDeviceVideoOffset);
		LOG.info(String.format("h:w = %d:%d", deviceVideoHeight, deviceVideoWidth));
		
		traceTimeDuration = Math.max(traceTimeDuration, deviceVideoDuration);
		LOG.info("duration:" + traceTimeDuration);
		LOG.info("nb_frames:" + deviceVideoNbFrames);
		

	}
	
	private double getVideoTime() {
		String[] timeArray;
		double vTime0 = 0;
		File timeFile = fileManager.createFile(traceFolder, "video_time");
		try {
			timeArray = fileManager.readAllLine(timeFile.toString());
			if (timeArray != null && timeArray.length > 0) {
				if (timeArray[0].contains(" ")) {
					vTime0 = StringParse.stringToDouble(StringUtils.substringBefore(timeArray[0], " "), 0);
				} else {
					vTime0 = StringParse.stringToDouble(timeArray[0], 0);
				}
			}
		} catch (IOException e) {
			LOG.error("Failed to obtain video time.", e);
		}

		return vTime0 * 1000;
	}
	
	private Double[] getTraceTime() {
		String[] timeArray;
		Double[] time0 = {0D,0D};
		File timeFile = fileManager.createFile(traceFolder, "time");

		try {
			timeArray = fileManager.readAllLine(timeFile.toString());
			if (timeArray != null && timeArray.length > 0) {
				time0[0] = Double.valueOf(timeArray[1]) * 1000;
				time0[1] = Double.valueOf(timeArray[3]) * 1000;
			}

		} catch (IOException e) {
			LOG.error("Failed to obtain trace time.", e);
		}

		return time0;
	}

	/**
	 * Calculate output frames per second
	 * 
	 * @param frameMap
	 * @return fps 
	 */
	private double calculateFPS(double defaultValue) {
		if (deviceVideoDuration > 0) {
			collectFrames(new FrameRequest(FrameRequest.JobType.COLLECT_FRAMES, 0, 1, 0D, null));
			collectFrames(new FrameRequest(FrameRequest.JobType.COLLECT_FRAMES, deviceVideoDuration / 2, 1, null, null));

			while (frameMap.size() < 2) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					LOG.info("Interrupted :" + e.getMessage());
				}
			}
			Double frame;
			if ((frame = frameMap.lastEntry().getKey()) > 0) {
				return frame / (deviceVideoDuration / 2);
			}
		}
		return defaultValue;
	}
	
	/**
	 * Collect frames from startTime for a given count of frames. Frames are added to a TreeMap<Double, BufferedImage> frameMap
	 * 
	 * Note: If a startTime is calculated to retrieve a specific frame this can fail if the ffmpeg fails to extract one or more frames depending on the state of the
	 * video file. If accuracy is important, the results should be examined, and adjustments should be made to handle missing frames. Usually this results in
	 * pulling frames beyond the target.
	 * 
	 * @param startTime
	 * @param frameCount
	 * @param resultSubscriber
	 *                             to receive results
	 */
	private void collectFrames(FrameRequest frameRequest) {
		try {
			videoFrameExtractor.addJob(frameRequest);
		} catch (Exception e) {
			LOG.error("VideoFrameExtractor Exception:" + e.getMessage());
			return;
		}
	}

	/**
	 * Extracts 1 frame every frameSkip count. Results depend on the video. Do not depend on the frame skip to be an exact amount.
	 * 
	 * @param frameSkip
	 * @param frameReceiver 
	 * @throws Exception
	 */
	public void preloadFrames(int frameSkip, FrameReceiver frameReceiver) throws Exception {
		videoFrameExtractor.addJob(new FrameRequest(JobType.PRELOAD, 0, frameSkip, null, frameReceiver));
	}
}
