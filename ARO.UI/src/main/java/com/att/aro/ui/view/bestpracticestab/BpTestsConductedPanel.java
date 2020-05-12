/*
 *  Copyright 2015 AT&T
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
package com.att.aro.ui.view.bestpracticestab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.commonui.TabPanelJScrollPane;
import com.att.aro.ui.commonui.UIComponent;
import com.att.aro.ui.utils.ResourceBundleHelper;

/**
 *         <p>
 *         Displays two column report of the tests with icons that indicate pass, fail, caution, or not run.
 *         </p>
 * 
 *
 *
 */
public class BpTestsConductedPanel extends AbstractBpPanel {

	private static final long serialVersionUID = 1L;

	private static final Insets    TESTS_CONDUCTED_INSETS = new Insets(10, 8, 10, 10);

	private JLabel testFillerHeaderLabel;

	private List<AbstractBestPracticeResult> bpResults;
	
	private TabPanelJScrollPane bpTab;
	
	private MouseAdapter mouseAdapter;

	BpTestsConductedPanel(TabPanelJScrollPane bestPracticesTab) {
		super();

		dataPanel = null;
		testFillerHeaderLabel = new JLabel();
		this.bpTab = bestPracticesTab;
		
		// when user click the mouse, capture the event and "jump" to corresponding session
		this.mouseAdapter = new MouseAdapter() {
			
			@Override
			public void mousePressed(MouseEvent e) {
				Map<String, Point> titleToLocation = null;
				if (bpTab instanceof BestPracticesTab) {
					titleToLocation = ((BestPracticesTab) bpTab).getTitleToLocation();
				}

				if (titleToLocation != null) {
					Component comp = e.getComponent();
					
					if (comp instanceof JLabel) {
						JLabel clickedLabel = (JLabel) comp;
						String temp = clickedLabel.getText();
						String title = temp.substring(temp.indexOf(":") + 1).trim();
						
						if (titleToLocation.containsKey(title)) {
							Point point = titleToLocation.get(title);
							
							int x = (int) point.getX();
							int y = (int) (point.getY() - bpTab.getViewport().getViewPosition().getY());
							Point adjusted = new Point(x, y);
							bpTab.getHorizontalScrollBar().setValue(0);
							bpTab.getViewport().scrollRectToVisible(new Rectangle(adjusted));
						}
					}
				}
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				Component comp = e.getComponent();
				
				if (comp instanceof JLabel) {
					JLabel enteredLabel = (JLabel) comp;
					enteredLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
				}
			}
		};

		add(layoutDataPanel(), BorderLayout.CENTER);
	}

	@Override
	public JPanel layoutDataPanel() {

		final double weightX = 0.0;

		if (dataPanel == null) {
			dataPanel = new JPanel(new GridBagLayout());
			dataPanel.setBackground(Color.WHITE);

			Insets insets = new Insets(2, 2, 2, 2);
			int idx = 0;

			JLabel testConductedHeaderLabel = new JLabel(ResourceBundleHelper.getMessageString("bestPractices.header.testsConducted"));
			testConductedHeaderLabel.setBackground(Color.WHITE);
			testConductedHeaderLabel.setFont(HEADER_FONT);
			dataPanel.add(testConductedHeaderLabel, new GridBagConstraints(
					0, idx
					, 3, 1
					, 0.0, 0.0
					, GridBagConstraints.WEST
					, GridBagConstraints.NONE
					, new Insets(0, 0, 0, 0)
					, 0, 0));
			
			addLabelLine(testFillerHeaderLabel, " ", ++idx, 2, weightX, insets, TEXT_FONT);

			if (bpResults != null && !bpResults.isEmpty()){
				addTestsConductedSummary();
			} else {
				addNullSummary();
			}

		}
		return dataPanel;
	}

	@Override
	public void refresh(AROTraceData model) {

		bpResults = model.getBestPracticeResults();

		removeAll();
		dataPanel = null;
		testFillerHeaderLabel = new JLabel();

		add(layoutDataPanel(), BorderLayout.CENTER);

	}

	private void addTestsConductedSummary() {
		int bpRunCt = 0;
		if (bpResults != null) {
			for (AbstractBestPracticeResult bestPracticeResult : bpResults) {
				if (bestPracticeResult.getResultType() != BPResultType.NONE) {
					bpRunCt++;
				}
			}
			int gridY1 = 1;
			int column = 0;
			int rows = bpRunCt / 2 + 1;
			for (AbstractBestPracticeResult bestPracticeResult : bpResults) {
				if (bestPracticeResult.getResultType() == BPResultType.NONE) {
					continue;
				}
				if (gridY1 > rows) {
					gridY1 = 1;
					column = 2;
				}

				bestPracticeResult.getOverviewTitle();
				bestPracticeResult.getResultText();
				ImageIcon icon = loadImageIcon(bestPracticeResult);
				addGridCell(gridY1++, column, icon, bestPracticeResult.getOverviewTitle());
			}
		}
		if (bpRunCt == 0) {
			addGridCell(1, 0, null, "No tests conducted");
		}
	}
	
	private void addNullSummary() {
		String[] bpSections = {	                        //  in the original Analyzer order
				   "textFileCompression"                //  #bp 01   File Download: Text File Compression
				  ,"caching.duplicateContent"           //  #bp 02   File Download: Duplicate Content
				  ,"caching.usingCache"                 //  #bp 03   File Download: Cache Control
				  ,"caching.cacheControl"               //  #bp 04   File Download: Content Expiration
				/*
				 * ,"caching.prefetching" // #bp 05 File Download: Content Pre-fetching
				 */
				  ,"combinejscss"                       //  #bp 06   File Download: Combine JS and CSS Requests
				  ,"imageSize"                          //  #bp 07   File Download: Resize Images for Mobile
				  ,"imageMetadata"						// Image: Image Metadata
				  ,"imageCompression"					// Image: Image Compression
				  ,"imageFormat"						// Image: Image Format
				  ,"uiComparator"						// Image: Image Comparator
				  ,"minification"                       //  #bp 08   File Download: Minify CSS, JS, JSON and HTML
				  ,"spriteimages"                       //  #bp 09   File Download: Use CSS Sprites for Images
				  ,"connections.connectionOpening"      //  #bp 10   Connections: Connection Opening
				  ,"connections.unnecssaryConn"         //  #bp 11   Connections: Unnecessary Connections - Multiple Simultaneous Connections
				  ,"connections.simultaneous"  			//  #bp      Connections: Multiple Connections to One Endpoint
				  ,"connections.multiSimultaneous"  	//  #bp      Connections: Multiple Connections to many Servers
				  ,"connections.periodic"               //  #bp 12   Connections: Inefficient Connections - Periodic Transfers
				  ,"connections.screenRotation"         //  #bp 13   Connections: Inefficient Connections - Screen Rotation
				  ,"connections.connClosing"            //  #bp 14   Connections: Inefficient Connections - Connection Closing Problems
				  ,"connections.http4xx5xx"             //  #bp 16   Connections: 400, 500 HTTP Status Response Codes
				  ,"connections.http3xx"                //  #bp 17   Connections: 301, 302 HTTP Status Response Codes
				  ,"3rd.party.scripts"         	        //  #bp 18   Connections: 3rd Party Scripts
				  ,"html.asyncload"                     //  #bp 19   HTML: Asynchronous Load of JavaScript in HTML
				  ,"html.httpUsage"                     //  #bp 20   HTML: HTTP 1.0 Usage
				  ,"html.fileorder"                     //  #bp 11   HTML: File Order
				  ,"empty.url"                          //  #bp 12   HTML: Empty Source and Link Attributes
				  ,"html.displaynoneincss"              //  #bp 14   HTML: "display:none" in CSS
				  
				  ,"security.httpsUsage"				// 	#bp 15	 Security: HTTPS Usage
				  ,"security.transmissionPrivateData"	//  #bp 16	 Security: Transmission of Personal Information
				  ,"security.unsecureSSLVersion"		// 	#bp 17	 Security: Unsecure SSL Versions
				  ,"security.weakCipher"				//  #bp 18	 Security: Weak Cipher
				  ,"security.forwardSecrecy"			//  #bp 19	 Security: Forward Secrecy
				  
				  ,"videoStall"
				  ,"startUpDelay"
				  ,"bufferOccupancy"
				  ,"networkComparison"
				  ,"tcpConnection"
				  ,"segmentSize"
				  ,"segmentPacing"
				  ,"videoRedundancy"
				  ,"videoConcurrentSession"
				  ,"videoVariableBitrate"
				  ,"videoResolutionQuality"
				  ,"adaptiveBitrateLadder"
				  ,"audioStream"
				  
				  ,"other.accessingPeripherals"         //  #bp    Other: Accessing Peripheral Applications
			//	  ,"#smallrequest"                      //  #bp    File Download: Minimize Number of Small Requests
			};

		ImageIcon icon = UIComponent.getInstance().getIconByKey("Image.naGray");
		
		if (bpSections != null) {
			int gridY2 = 1;
			int column = 0;
			int rows = bpSections.length / 2 + 1;
			for (String bestPracticeName : bpSections) {
				if (gridY2 > rows) {
					gridY2 = 1;
					column = 2;
				}
				
				// System.out.println("bp name :"+bestPracticeName);
				String text = null;
				try {
					text  = ResourceBundleHelper.getMessageString(bestPracticeName+".title");
				} catch (Exception e) {
					try {
						text = "ERROR: missing codes :" + ResourceBundleHelper.getMessageString(bestPracticeName);
					} catch (Exception e1) {
						text = "ERROR: missing codes :" + bestPracticeName;
					}
				}
				addGridCell(gridY2++, column, icon, text);
			}
		}
	}

	/**
	 * locate and load an ImageIcon based on AbstractBestPracticeResult
	 * 
	 * @param bestPracticeResult
	 * @return ImageIcon corresponding to PASS, FAIL, WARNING, SELF_TEST
	 */
	private ImageIcon loadImageIcon(AbstractBestPracticeResult bestPracticeResult) {
		BPResultType resType = bestPracticeResult.getResultType(); // PASS, FAIL or WARNING
		String imageName = "Image.naGray";
		if (resType.equals(BPResultType.PASS)) {
			imageName = "Image.bpPassDark";
		} else if (resType.equals(BPResultType.FAIL)) {
			imageName = "Image.bpFailDark";
		} else if (resType.equals(BPResultType.WARNING)) {
			imageName = "Image.bpWarningDark";
		} else if (resType.equals(BPResultType.SELF_TEST)) {
			imageName = "Image.bpManual";
		} else if (resType.equals(BPResultType.CONFIG_REQUIRED)) {
			imageName = "Image.bpConfig";
		} else if (resType.equals(BPResultType.NO_DATA)) {
			imageName = "Image.bpNoData";
		}
		
		return UIComponent.getInstance().getIconByKey(imageName);
	}

	/**
	 * Gets the ImageIcon object that is associated with the current enumeration
	 * entry.
	 * 
	 * @return The ImageIcon object that is associated with the current key.
	 */
	private void addGridCell(int gridY, int column, ImageIcon icon, String string) {
		JLabel iconLabel = new JLabel(icon);
		JLabel titleLabel = new JLabel(string);
		
		titleLabel.addMouseListener(mouseAdapter);
		titleLabel.addMouseMotionListener(mouseAdapter);
		
		titleLabel.setFont(TEXT_FONT);
		dataPanel.add(iconLabel, getTestsConductedGridConstraints(column, gridY));
		dataPanel.add(titleLabel, getTestsConductedGridConstraints(column + 1, gridY));
	}

	private GridBagConstraints getTestsConductedGridConstraints(int gridX, int gridY) {
		return new GridBagConstraints(gridX, gridY, 1, 1, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.NONE, TESTS_CONDUCTED_INSETS, 0, 0);
	}

	@Override
	public int print(Graphics arg0, PageFormat arg1, int arg2) throws PrinterException {
		return 0;
	}

}
