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
package com.att.aro.ui.model.diagnostic;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.CheckBoxRenderer;
import com.att.aro.ui.model.DataTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class TCPUDPFlowsTableModel extends DataTableModel<Session> {
    private static final long serialVersionUID = 3691872179254714817L;

    public static final int TIME_COL = 0;
    public static final int CHECKBOX_COL = 1;
    public static final int DOMAIN_COL = 2;
    public static final int LOCALPORT_COL = 3;
    public static final int REMOTEIP_COL = 4;
    public static final int REMOTEPORT_COL = 5;
    public static final int BYTE_COUNT_COL = 6;
    public static final int PACKETCOUNT_COL = 7;
    public static final int PROTOCOL_COL = 8;
    public static final int SYN_COL = 9;
    public static final int SYN_ACK_COL = 10;
    public static final int NW_LATENCY_COL = 11;

    private static final String[] COLUMNNAMES = { 
    		ResourceBundleHelper.getMessageString("tcp.time"), 		         //  0
    		"", // Empty column name for checkbox selection                  //  1
            ResourceBundleHelper.getMessageString("tcp.domain"), 	         //  2
            ResourceBundleHelper.getMessageString("tcp.local"),              //  3
            ResourceBundleHelper.getMessageString("tcp.remote"), 	         //  4
            ResourceBundleHelper.getMessageString("tcp.remoteport"),         //  5
            ResourceBundleHelper.getMessageString("tcp.bytecount"),          //  6
            ResourceBundleHelper.getMessageString("tcp.packetcount"),        //  7
            ResourceBundleHelper.getMessageString("tcp.protocol"), 	         //  8
            ResourceBundleHelper.getMessageString("tcp.syn"),                //  9
            ResourceBundleHelper.getMessageString("tcp.synack"), 	         //  10
            ResourceBundleHelper.getMessageString("tcp.latency") };          //  11
    
    private Set<Session> highlighted = new HashSet<Session>();
    private TableColumnModel cols;

    private Map<String, Session> sessionMap;
    private Map<String, Boolean> checkboxMap;// side load and track...

	private FontRenderContext fontRenderContext;
	private Font font;
	private int maxDomainColWidth;
	private int maxRemoteIpColWidth;
	private int maxByteCountColWidth;

    public Map<String, Session> getSessionMap() {
        return sessionMap;
    }

    public Map<String, Boolean> getCheckboxMap() {
        return checkboxMap;
    }

    private static TCPUDPFlowsTableModel tcpudpFlowsTableModel;
    
	public static TCPUDPFlowsTableModel getInstance() {
		if (tcpudpFlowsTableModel == null) {
			tcpudpFlowsTableModel = new TCPUDPFlowsTableModel();
		}
		return tcpudpFlowsTableModel;
	}
    
    /**
     * Initializes a new instance of the BurstAnalysisTableModel class.
     */
    public TCPUDPFlowsTableModel() {
        super(COLUMNNAMES);
        sessionMap = new HashMap<String, Session>();
        checkboxMap = new HashMap<String, Boolean>();
		fontRenderContext = new FontRenderContext(new AffineTransform(), true, true);
        this.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent arg0) {
                if (arg0.getType() == TableModelEvent.INSERT || arg0.getType() == TableModelEvent.DELETE) {
                    highlighted.clear();
                }
            }
        });
    }

	public void refresh(AROTraceData aroTraceData, Font font) {
		this.font = font;
		maxDomainColWidth = 0;
		maxRemoteIpColWidth = 0;
		maxByteCountColWidth = 0;

		List<Session> sessionlist = aroTraceData.getAnalyzerResult().getSessionlist();
		setData(sessionlist);
		getCheckboxMap().clear();
		for (Session session : sessionlist) {
			String sessionKey = getSessionKey(session);
			sessionMap.put(sessionKey, session);
			checkboxMap.put(sessionKey, new Boolean(true));
		}
	}

    /**
     * Temporary Fix
     * 
     * ARO is using start time to check duplicate, but some sessions might have same
     * start time. Remote IP, remote port and local port can guarantee a unique
     * session.
     * 
     * Better to change equals function in Session in 6.1 release
     * 
     * @param session
     * @return
     */
    public String getSessionKey(Session session) {
        return session.getRemoteIP().getHostAddress() + "$" + session.getRemotePort() + "$" + session.getLocalPort();
    }

    /**
     * Returns a class representing the specified column. This method is primarily
     * used to sort numeric columns.
     * 
     * @param columnIndex
     *            The index of the specified column.
     * 
     * @return A class representing the specified column.
     * 
     * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case TIME_COL:
                return Double.class;
                
            case CHECKBOX_COL:
                return Boolean.class;
                
            case REMOTEPORT_COL:
            case BYTE_COUNT_COL:
            case PACKETCOUNT_COL:
                return Integer.class;
                
            case REMOTEIP_COL:
            case DOMAIN_COL:
                return String.class;
            default:
                return super.getColumnClass(columnIndex);
        }
    }

    public boolean isCellEditable(int row, int col) {
        return col == CHECKBOX_COL;
    }

    /**
     * Returns a TableColumnModel that is based on the default table column model
     * for the DataTableModel class. The TableColumnModel returned by this method
     * has the same number of columns in the same order and structure as the table
     * column model in the DataTableModel. When a DataTable object is created, this
     * method is used to create the TableColumnModel if one is not specified. This
     * method may be overridden in order to provide customizations to the default
     * column model, such as providing a default column width and/or adding column
     * renderers and editors.
     * 
     * @return A TableColumnModel object.
     */
    @Override
    public TableColumnModel createDefaultTableColumnModel() {
        if (cols == null) {
            cols = super.createDefaultTableColumnModel();
            DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer();

            setTableColumnParams(TIME_COL        ,  60,  60,  60, false, new NumberFormatRenderer(new DecimalFormat("0.000"))); // 0                 
            setTableColumnParams(CHECKBOX_COL    ,  35,  35,  35, false, new CheckBoxRenderer()); // 1                                           
            setTableColumnParams(DOMAIN_COL      , 200, 250,null,  true, dtcr); // 2                     
            setTableColumnParams(LOCALPORT_COL   ,  60, 100, 100,  true, dtcr); // 3                                              
            setTableColumnParams(REMOTEIP_COL    , 200, 250,null,  true, dtcr); // 4                                            
            setTableColumnParams(REMOTEPORT_COL  ,  80, 130, 100,  true, dtcr); // 5                                             
            setTableColumnParams(BYTE_COUNT_COL  ,  70,  70, 130,  true, dtcr); // 6   
            setTableColumnParams(PACKETCOUNT_COL ,  83, 150, 120,  true, dtcr); // 7  
            setTableColumnParams(PROTOCOL_COL    ,  55, 140,  70,  true, dtcr); // 8  
            setTableColumnParams(SYN_COL         ,   0,   0,   0, false, dtcr); // 9  
            setTableColumnParams(SYN_ACK_COL     ,   0,   0,   0, false, dtcr); // 10 
            setTableColumnParams(NW_LATENCY_COL  ,  55,  85,  95, false, dtcr); // 11 
        }
        return cols;
    }
    
	public void setTableColumnParams(int column, Integer min, Integer preferred, Integer max, Boolean resizable, TableCellRenderer tableCellRenderer) {
		TableColumn appCol = cols.getColumn(column);

		if (min != null) {
			appCol.setMinWidth(min);
		}
		if (preferred != null) {
			appCol.setPreferredWidth(preferred);
		}
		if (max != null) {
			appCol.setMaxWidth(max);
		}
		if (resizable != null) {
			appCol.setResizable(resizable);
		}
		if (tableCellRenderer != null) {
			appCol.setCellRenderer(tableCellRenderer);
		}
	}
	
	/**
	 * Resize the column, by detecting witdh of supplied text content. If detected width is greater than previous then apply to min, preferred, and max. 
	 * The minimum width will only be temporarily change before being restored to the original minimum.
	 * 
	 * @param columIndex
	 * @param text
	 * @param maxWidth
	 * @return the new maximum or the current maximum.
	 */
	private int resizeColumnBasedOnContent(int columIndex, String text, int maxWidth) {
		int textWidth;
		if ((textWidth = (int) (font.getStringBounds(text, fontRenderContext).getWidth())) > maxWidth) {
			int min = cols.getColumn(columIndex).getMinWidth();
			cols.getColumn(columIndex).setMinWidth(maxWidth);
			cols.getColumn(columIndex).setPreferredWidth(maxWidth);
			cols.getColumn(columIndex).setMinWidth(min);
			maxWidth = textWidth;
		}
		return maxWidth;
	}
	
    /**
     * This is the one method that must be implemented by subclasses. This method
     * defines how the data object managed by this table model is mapped to its
     * columns when displayed in a row of the table. The getValueAt() method uses
     * this method to retrieve table cell data.
     * 
     * Also sets min, preferred and max widths on selected columns (DOMAIN_COL, REMOTEIP_COL, BYTE_COUNT_COL)
     * 
     * @param item
     *            A object containing the column information. columnIndex The index
     *            of the specified column.
     * 
     * @return An object containing the table column value.
     */
    @Override
    protected Object getColumnValue(Session item, int columnIndex) {
	
        switch (columnIndex) {
            case TIME_COL:
                if (item.isUdpOnly()) {
                    return item.getUdpPackets().get(0).getTimeStamp();
                } else {
                    return item.getTcpPackets().get(0).getTimeStamp();
                }
            case CHECKBOX_COL:
                String sessionKey = getSessionKey(item);
                return getCheckboxMap().get(sessionKey);
                
            case DOMAIN_COL:
            	maxDomainColWidth = resizeColumnBasedOnContent(columnIndex, item.getDomainName(), maxDomainColWidth);
                return item.getDomainName();

            case LOCALPORT_COL:
                return item.getLocalPort();

            case REMOTEIP_COL:
            	maxRemoteIpColWidth = resizeColumnBasedOnContent(columnIndex, item.getRemoteIP().getHostAddress(), maxRemoteIpColWidth);
                return item.getRemoteIP().getHostAddress();

            case REMOTEPORT_COL:
                return item.getRemotePort();

            case BYTE_COUNT_COL:
            	maxByteCountColWidth = resizeColumnBasedOnContent(columnIndex, Long.toString(item.getBytesTransferred()), maxByteCountColWidth);
                return item.getBytesTransferred();

            case PACKETCOUNT_COL:
                if (item.isUdpOnly()) {
                    return item.getUdpPackets().size();
                } else {
                    return item.getTcpPackets().size();
                }
            case PROTOCOL_COL:
                if (item.isUdpOnly()) {
                    if (53 == item.getLocalPort() || 53 == item.getRemotePort()) {
                        return ResourceBundleHelper.getMessageString("tcp.dns");
                    } else if (443 == item.getLocalPort() || 443 == item.getRemotePort() || 80 == item.getLocalPort() || 80 == item.getRemotePort()) {
                        return ResourceBundleHelper.getMessageString("tcp.quic");
                    } else {
                        return ResourceBundleHelper.getMessageString("tcp.udp");
                    }
                } else {
                    return ResourceBundleHelper.getMessageString("tcp.tcp");
                }
            case NW_LATENCY_COL:
                if (!item.isUdpOnly() && !item.getSynAckPackets().isEmpty() && !item.getSynPackets().isEmpty()) {
                	return item.getLatency()== -1 ? "N/A" : (item.getLatency() > 0 ? Util.formatDoubleToMicro(item.getLatency()) : "0.0");
					 
                } else {
                    return "N/A";
                }
            case SYN_COL: {
                if (!item.isUdpOnly() && !item.getSynAckPackets().isEmpty() && !item.getSynPackets().isEmpty()) {
                    return item.getSynTime();
                } else {
                    return 0.0d;
                }
            }
            case SYN_ACK_COL: {
                if (!item.isUdpOnly() && !item.getSynAckPackets().isEmpty() && !item.getSynPackets().isEmpty()) {
                    return item.getSynAckTime();
                } else {
                    return 0.0d;
                }
            }

            default:
                return null;
        }
    }

    /**
     * update the datamodel with data
     */
    public void setValueAt(Object value, int row, int col) {
        if (col == CHECKBOX_COL) {
            Session item = getValueAt(row);
            if (value instanceof Boolean) {
                boolean checkBoxValue = (Boolean) value;
                String sessionKey = getSessionKey(item);
                if (sessionMap.containsKey(sessionKey) && checkboxMap.containsKey(sessionKey)) {
                    sessionMap.put(sessionKey, item);
                    checkboxMap.put(sessionKey, checkBoxValue);
                }
            }
            fireTableCellUpdated(row, col);
        }

    }

}