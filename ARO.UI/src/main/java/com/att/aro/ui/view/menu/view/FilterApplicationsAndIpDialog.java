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

package com.att.aro.ui.view.menu.view;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.packetanalysis.impl.PacketAnalyzerImpl;
import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.core.packetanalysis.pojo.ApplicationSelection;
import com.att.aro.core.packetanalysis.pojo.IPAddressSelection;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.mvc.IAROView;
import com.att.aro.ui.commonui.EnableEscKeyCloseDialog;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.model.DataTablePopupMenu;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;

/**
 * Represents the Filter Applications Dialog that appears when the user chooses
 * the Select Applications/IPs menu item under the View menu. This dialog prompts
 * the user to select applications and IP addresses that are found in the
 * trace data.
 * 
 *
 */
public class FilterApplicationsAndIpDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LogManager.getLogger(FilterApplicationsAndIpDialog.class);	
	private JPanel jButtonPanel = null;
	private JPanel jButtonGrid = null;
	private JButton okButton = null;
	private JButton cancelButton = null;
	private JPanel mainPanel = null;
	private DataTable<ApplicationSelection> jApplicationsTable = null;
	private DataTable<FilterIpAddressesTableModel.AppIPAddressSelection> jIpAddressTable = null;
	private FilterApplicationsTableModel jApplicationsTableModel;
	private FilterIpAddressesTableModel jIpAddressesTableModel;
 	private IAROView parent;
 	
 	private PacketAnalyzerResult currentTraceResult;
 	private PacketAnalyzerResult initialTraceResult;
 	private AnalysisFilter initialFilter; 
 	private boolean ipv4Selection;
 	private boolean ipv6Selection;
 	private boolean udpSelection;
 	private boolean dnsSelection;
 	
	public PacketAnalyzerResult getCurrentPktAnalyzerResult() {
		return currentTraceResult;
	}

	public void setCurrentPktAnalyzerResult(PacketAnalyzerResult currentTraceResult) {
		this.currentTraceResult = currentTraceResult;
	}

	public PacketAnalyzerResult getInitialPktAnalyzerResult() {
		return initialTraceResult;
	}

	public void setInitialPktAnalyzerResult(PacketAnalyzerResult initialTraceResult) {
		this.initialTraceResult = initialTraceResult;
	}
	
	private enum DialogItem {
		filter_title,
		Button_ok,
		Button_cancel
	}

	/**
	 * Initializes a new instance of the FilterApplicationsAndIpDialog class using
	 * the specified instance of the ApplicationResourceOptimizer as the owner.
	 * 
	 * @param parent
	 *            The top level instance (that implements SharedAttributesProcesses).
	 */
	public FilterApplicationsAndIpDialog(IAROView parent) {
		this.parent = parent;
		initialize();
	}

	/**
	 * Initializes the dialog.
	 */
	private void initialize() {
		PacketAnalyzerResult currentTraceResult = ((MainFrame)parent).getController().getTheModel().getAnalyzerResult();
		PacketAnalyzerResult initialTraceResult = ((MainFrame)parent).getController().getCurrentTraceInitialAnalyzerResult();
		
		if (currentTraceResult==null){
			LOGGER.error("Trace result error! " );
			MessageDialogFactory.getInstance().showErrorDialog(FilterApplicationsAndIpDialog.this,"wrong.."); 
		}else{
			setCurrentPktAnalyzerResult(currentTraceResult);
			setInitialPktAnalyzerResult(initialTraceResult);
			
			// Save a copy of the filter before user makes any changes to the selection
			cloneFilter();
					
			this.jIpAddressesTableModel = new FilterIpAddressesTableModel(currentTraceResult.getFilter());			
			this.jApplicationsTableModel = new FilterApplicationsTableModel(currentTraceResult.getFilter());
			this.jApplicationsTableModel.addTableModelListener(new TableModelListener() {

				@Override
				public void tableChanged(TableModelEvent e) {
					if (e.getColumn() == FilterApplicationsTableModel.SELECT_COL || e.getColumn() == FilterApplicationsTableModel.COLOR_COL) {
						for (int row = e.getFirstRow(); row <= e.getLastRow(); ++row) {
							if (row >= 0 && row < jApplicationsTableModel.getRowCount()) {
								ApplicationSelection as = jApplicationsTableModel.getValueAt(row);
								String appName = as.getAppName();
								for (FilterIpAddressesTableModel.AppIPAddressSelection is : jIpAddressesTableModel.getData()) {
									if (appName == is.getAppName() || (appName != null && appName.equals(is.getAppName()))) {
										if ((as.isSelected() || !is.getIpSelection().isSelected()) && e.getColumn() == FilterApplicationsTableModel.COLOR_COL) {
											is.getIpSelection().setColor(as.getColor());
										}
										if (e.getColumn() == FilterApplicationsTableModel.SELECT_COL) {
											is.getIpSelection().setSelected(as.isSelected());
										}
									}
								}
							}
						}
						jIpAddressesTableModel.fireTableDataChanged();
					}
				}
				
			});
			this.jIpAddressesTableModel.addTableModelListener(new TableModelListener() {

				@Override
				public void tableChanged(TableModelEvent e) {
					if (e.getColumn() == FilterIpAddressesTableModel.SELECT_COL) {
						for (int row = e.getFirstRow(); row <= e.getLastRow(); ++row) {
							if (row >= 0 && row < jIpAddressesTableModel.getRowCount()) {
								FilterIpAddressesTableModel.AppIPAddressSelection ipSel = jIpAddressesTableModel.getValueAt(row);
								String appName = ipSel.getAppName();
								boolean b = ipSel.getIpSelection().isSelected();
								if (b) {
									for (FilterIpAddressesTableModel.AppIPAddressSelection is : jIpAddressesTableModel.getData()) {
										if (appName == is.getAppName() || (appName != null && appName.equals(is.getAppName()))) {
											b &= is.getIpSelection().isSelected();
										}
									}
								}
								
								for (ApplicationSelection as : jApplicationsTableModel.getData()) {
									if (appName != null && appName.equals(as.getAppName())) {
										as.setSelected(b);
										break;
									}
								}
								jApplicationsTableModel.fireTableDataChanged();
							}
						}
					}
				}
				
			});
		}

		this.setSize(600, 420);
		this.setModal(true);
		this.setTitle(ResourceBundleHelper.getMessageString(DialogItem.filter_title));
		this.setLayout(new BorderLayout());
		this.add(getMainPanel(), BorderLayout.CENTER);
		this.setLocationRelativeTo(getOwner());
		new EnableEscKeyCloseDialog(getRootPane(), this);
	}

	/*
	 * Make a copy of the filter before selection changes 
	 * made update the filter. We can then revert the 
	 * updated filter using this copy when user clicks
	 * the cancel button.
	 */
	private void cloneFilter() {
	
		AnalysisFilter filter = getCurrentPktAnalyzerResult().getFilter();
		Collection<ApplicationSelection> appSel = filter.getApplicationSelections();
		HashMap<String, ApplicationSelection> applications = new HashMap<String, ApplicationSelection>(appSel.size());
	
		for (ApplicationSelection aSel: appSel) {
			ApplicationSelection clonedAP = new ApplicationSelection(aSel);
			applications.put(clonedAP.getAppName(), clonedAP);		
		}
				
		initialFilter = new AnalysisFilter(applications, filter.getTimeRange(), filter.getDomainNames());	
		initialFilter.setIpv4Sel(filter.isIpv4Sel());
		initialFilter.setIpv6Sel(filter.isIpv6Sel());
		initialFilter.setUdpSel(filter.isUdpSel());
	}

	/**
	 * Initializes ButtonPanel
	 */
	private JPanel getJButtonPanel() {
		if (jButtonPanel == null) {
			jButtonPanel = new JPanel(new BorderLayout());
			jButtonPanel.add(getJButtonGrid(), BorderLayout.EAST);
			this.addWindowListener(getWindowListener());
		}
		return jButtonPanel;
	}
	
	private WindowListener getWindowListener() {
		return (new WindowListener() {
			
			@Override
			public void windowOpened(WindowEvent e) {
				// Auto-generated method 				
			}
			
			@Override
			public void windowIconified(WindowEvent e) {
				// Auto-generated method 		
			}
			
			@Override
			public void windowDeiconified(WindowEvent e) {
				// Auto-generated method 
			}
			
			@Override
			public void windowDeactivated(WindowEvent e) {
				// Auto-generated method 
			}
			
			@Override
			public void windowClosing(WindowEvent e) {
				closeWindow();
			}
			
			@Override
			public void windowClosed(WindowEvent e) {
			}
			
			@Override
			public void windowActivated(WindowEvent e) {
				// Auto-generated method 
			}
		});	
	}

	/**
	 * Initializes and returns the gird containing the ok and cancel buttons.
	 */
	private JPanel getJButtonGrid() {
		if (jButtonGrid == null) {
			GridLayout gridLayout = new GridLayout();
			gridLayout.setRows(1);
			gridLayout.setHgap(10);
			jButtonGrid = new JPanel(gridLayout);
			jButtonGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10,
					10));
			jButtonGrid.add(getOkButton(), null);
			jButtonGrid.add(getCancelButton(), null);
		}
		return jButtonGrid;
	}

	/**
	 * Initializes and returns the ok Button.
	 */
	private JButton getOkButton() {
		if (okButton == null) {
			okButton = new JButton();
			okButton.setText(ResourceBundleHelper.getMessageString(DialogItem.Button_ok));
			okButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					if(getCurrentPktAnalyzerResult().getFilter()!=null){
						AnalysisFilter filter = getCurrentPktAnalyzerResult().getFilter();
						Map<InetAddress, String> domainNames= filter.getDomainNames();
						Map<String, ApplicationSelection> appSelections = new HashMap<String, ApplicationSelection>(filter.getAppSelections().size());
						for (ApplicationSelection sel : filter.getAppSelections().values()) {
							if(domainNames != null){ //Greg Story Add domain names map to Application Selection
								sel.setDomainNames(domainNames);
							}
							appSelections.put(sel.getAppName(), new ApplicationSelection(sel));
						}
						filter.setIpv4Sel(ipv4Selection);
						filter.setIpv6Sel(ipv6Selection);
						filter.setUdpSel(udpSelection);
						filter.setDnsSelection(dnsSelection);
			
						if (!selectionReturnsData()) {
							
							MessageDialogFactory.getInstance().showErrorDialog(
									FilterApplicationsAndIpDialog.this, 
									ResourceBundleHelper.getMessageString("filter.noResultData.error"));
						
						} else {

							((MainFrame)parent).updateFilter(filter);
							dispose();
						
						}
					}
				}
			});
		}
		return okButton;
	}

	
	/*
	 * Compares user selection to trace data to determine 
	 * if all the trace data will be filtered out if user
	 * selection is applied. 
	 */
	private boolean selectionReturnsData() {
		
		AnalysisFilter filter = getCurrentPktAnalyzerResult().getFilter();

		if (!filter.isIpv4Sel() && !filter.isIpv6Sel() && !filter.isUdpSel()) {
			return false;
		}
		
		Collection<ApplicationSelection> appSel = filter.getApplicationSelections();		
		List<IPAddressSelection> ipSel = new ArrayList<IPAddressSelection>(); 
		List<IPAddressSelection> ipSelChecked = new ArrayList<IPAddressSelection>();
		
		for (ApplicationSelection aSel: appSel) {			
			ipSel.addAll(aSel.getIPAddressSelections());
		}
		
		for (IPAddressSelection iSel: ipSel) {
			if (iSel.isSelected())  {
				ipSelChecked.add(iSel);
			}
		}
		
		if (ipSelChecked.size() == 0) {
			return false;
		}
		
		List<PacketInfo> packetsInfo = getInitialPktAnalyzerResult().getTraceresult().getAllpackets();
		PacketAnalyzerImpl pktAnalyzer = (PacketAnalyzerImpl) ((MainFrame) parent).getController().getAROService().getAnalyzer();
					
		if (pktAnalyzer.filterPackets(filter, packetsInfo).size() > 0) {				
			return true;
		}

		return false;
	}
	
	/**
	 * Initializes and returns the cancel Button.
	 */
	private JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton();
			cancelButton.setText(ResourceBundleHelper.getMessageString(DialogItem.Button_cancel));
			cancelButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {						
				    closeWindow();
	
	
				}

			});
		}
		return cancelButton;
	}
	
	public void closeWindow() {
		/*
	     * Bring filter/selection back to what it was when the dialog was launched
	     */
		((MainFrame) parent).getController().getTheModel().getAnalyzerResult().setFilter(initialFilter);
		FilterApplicationsAndIpDialog.this.dispose();
	}

	/**
	 * Initializes mainPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getMainPanel() {
		if (mainPanel == null) {
			mainPanel = new JPanel();
			mainPanel.setLayout(new BorderLayout());
			mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			mainPanel.add(getJButtonPanel(), BorderLayout.SOUTH);
			mainPanel.add(getSelectionPanel(), BorderLayout.CENTER);
		}
		return mainPanel;
	}

	/**
	 * Initializes and returns the Panel that contains the applications and ip
	 * address selection tables.
	 */
	private JPanel getSelectionPanel() {

		JPanel selectionPanel = new JPanel(new GridLayout(2, 1));
		
		//selectionPanel.add(getPacketSelectionPanel());
		selectionPanel.add(getApplicationSelectionsPanel());
		selectionPanel.add(getIpAddressSelectionsPanel());

		return selectionPanel;
	}

	private JPanel getPacketSelectionPanel(){
		JPanel checkBoxSelPanel = new JPanel();
		final JCheckBox chkIpv4 = new JCheckBox("IPV4");
	    final JCheckBox chkIpv6 = new JCheckBox("IPV6");
	    final JCheckBox chkUdp = new JCheckBox("UDP");
	    final JCheckBox chkDns = new JCheckBox("DNS");

/*	    chkIpv4.setMnemonic(KeyEvent.VK_I);
	    chkIpv6.setMnemonic(KeyEvent.VK_P);
	    chkUdp.setMnemonic(KeyEvent.VK_U);
	    LOGGER.info("Filter Info : " +traceresult.getFilter());
	    if(traceresult.getFilter() != null){
	    	LOGGER.info("Flag1 : " +traceresult.getFilter().isIpv4Sel());
	    	LOGGER.info("Flag2 : " +traceresult.getFilter().isIpv6Sel());
	    	LOGGER.info("Flag3 : " +traceresult.getFilter().isUdpSel());
	    } 
*/
	    chkIpv4.setSelected(ipv4Selection = currentTraceResult.getFilter().isIpv4Sel());
		chkIpv6.setSelected(ipv6Selection = currentTraceResult.getFilter().isIpv6Sel());
		chkUdp.setSelected(udpSelection = currentTraceResult.getFilter().isUdpSel());
		chkDns.setSelected(dnsSelection = currentTraceResult.getFilter().isDnsSelection());
				
		chkIpv4.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent aEvent) {
				JCheckBox cb = (JCheckBox) aEvent.getSource();
				ipv4Selection = cb.isSelected();				
			}
		});

		chkIpv6.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent aEvent) {
				JCheckBox cb = (JCheckBox) aEvent.getSource();
				ipv6Selection = cb.isSelected();
			}
		});
		
		chkUdp.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent aEvent) {
				JCheckBox cb = (JCheckBox) aEvent.getSource();
				udpSelection = cb.isSelected();
			}
		});
		
		chkDns.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent aEvent) {
				JCheckBox cb = (JCheckBox)aEvent.getSource();
				dnsSelection = cb.isSelected();				
			}
		});
		
	    checkBoxSelPanel.add(chkIpv4);
	    checkBoxSelPanel.add(chkIpv6);
	    checkBoxSelPanel.add(chkUdp);
	    checkBoxSelPanel.add(chkDns);
	    checkBoxSelPanel.setSize(50, 20);
		return checkBoxSelPanel;
	}
	
	/**
	 * Initializes panel that contains the the list of applications.
	 */
	private JPanel getApplicationSelectionsPanel() {

		JPanel applicationSelectionPanel = new JPanel(new BorderLayout());

		JScrollPane applicationTableScrollPane = new JScrollPane(
				getJApplicationsTable());
		applicationSelectionPanel.add(applicationTableScrollPane,
				BorderLayout.CENTER);
		applicationSelectionPanel.setBorder(BorderFactory.createEmptyBorder(5,
				5, 5, 5));
		applicationSelectionPanel.add(getPacketSelectionPanel(), BorderLayout.SOUTH);

		return applicationSelectionPanel;

	}

	/**
	 * Initializes panel that contains the the list of IP addresses.
	 */
	private JPanel getIpAddressSelectionsPanel() {

		JPanel ipAddressSelectionPanel = new JPanel(new BorderLayout());

		JScrollPane ipAddressTableScrollPane = new JScrollPane(
				getJIpAddressesTable());
		ipAddressSelectionPanel.add(ipAddressTableScrollPane,
				BorderLayout.CENTER);
		ipAddressSelectionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5,
				5, 5));

//		ipAddressSelectionPanel.add(getPacketSelectionPanel(), BorderLayout.SOUTH);

		return ipAddressSelectionPanel;

	}

	/**
	 * Initializes and returns the table that contains the list of applications
	 * found in the trace data.
	 */
	private JTable getJApplicationsTable() {
		if (jApplicationsTable == null) {

			// Make sure to make a copy of the current data before modifying
			jApplicationsTable = new DataTable<ApplicationSelection>(jApplicationsTableModel);
			jApplicationsTable.setAutoCreateRowSorter(true);

			DataTablePopupMenu popupMenu = (DataTablePopupMenu) jApplicationsTable.getPopup();
            popupMenu.initialize();
		}
		return jApplicationsTable;
	}

	/**
	 * Initializes and returns the table that contains the list of IP addresses
	 * found in the trace data.
	 */
	private JTable getJIpAddressesTable() {
		if (jIpAddressTable == null) {

			// Make sure to make a copy of the current data before modifying
			jIpAddressTable = new DataTable<FilterIpAddressesTableModel.AppIPAddressSelection>(jIpAddressesTableModel);
			jIpAddressTable.setAutoCreateRowSorter(true);

			DataTablePopupMenu popupMenu = (DataTablePopupMenu) jIpAddressTable.getPopup();
            popupMenu.initialize();
		}
		return jIpAddressTable;
	}

} // @jve:decl-index=0:visual-constraint="70,10"
