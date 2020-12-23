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

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.packetanalysis.pojo.PacketInfo;
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

    private static final String HOSTPORTSEPERATOR = ResourceBundleHelper.getMessageString("tcp.hostPortSeparator");

    private static final Logger LOGGER = LogManager.getLogger(TCPUDPFlowsTableModel.class.getName());

    private static final String[] COLUMNNAMES = { ResourceBundleHelper.getMessageString("tcp.time"), "", // Empty column name for checkbox selection
            ResourceBundleHelper.getMessageString("tcp.domain"), ResourceBundleHelper.getMessageString("tcp.local"),
            ResourceBundleHelper.getMessageString("tcp.remote"), ResourceBundleHelper.getMessageString("tcp.remoteport"),
            ResourceBundleHelper.getMessageString("tcp.bytecount"), ResourceBundleHelper.getMessageString("tcp.packetcount"),
            ResourceBundleHelper.getMessageString("tcp.protocol"), ResourceBundleHelper.getMessageString("tcp.syn"),
            ResourceBundleHelper.getMessageString("tcp.synack"), ResourceBundleHelper.getMessageString("tcp.latency") };
    private Set<Session> highlighted = new HashSet<Session>();
    private TableColumnModel cols;

    private Map<String, Session> sessionMap;
    private Map<String, Boolean> checkboxMap;// side load and track...
    private double synTime = 0.0;
    private double synAckTime = 0.0;

    public Map<String, Session> getSessionMap() {
        return sessionMap;
    }

    public Map<String, Boolean> getCheckboxMap() {
        return checkboxMap;
    }

    /**
     * Initializes a new instance of the BurstAnalysisTableModel class.
     */
    public TCPUDPFlowsTableModel() {
        super(COLUMNNAMES);
        sessionMap = new HashMap<String, Session>();
        checkboxMap = new HashMap<String, Boolean>();
        this.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent arg0) {
                if (arg0.getType() == TableModelEvent.INSERT || arg0.getType() == TableModelEvent.DELETE) {
                    highlighted.clear();
                }
            }
        });

    }

    public void refresh(AROTraceData aroTraceData) {
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
            TableColumn col = cols.getColumn(TIME_COL);
            col.setCellRenderer(new NumberFormatRenderer(new DecimalFormat("0.000")));
            col.setMaxWidth(60);

            // Adding checkbox to column greg story
            TableColumn appCol = cols.getColumn(CHECKBOX_COL);
            appCol.setCellRenderer(new CheckBoxRenderer());
            appCol.setMaxWidth(35);

            appCol = cols.getColumn(DOMAIN_COL);
            DefaultTableCellRenderer defaultTableCR = new DefaultTableCellRenderer();
            appCol.setCellRenderer(defaultTableCR);
            appCol.setPreferredWidth(130);

            appCol = cols.getColumn(LOCALPORT_COL);
            appCol.setPreferredWidth(60);

            // Re-sizing the column
            appCol = cols.getColumn(BYTE_COUNT_COL);
            appCol.setMaxWidth(70);
            appCol.setPreferredWidth(70);

            appCol = cols.getColumn(PACKETCOUNT_COL);
            appCol.setMaxWidth(75);

            appCol = cols.getColumn(PROTOCOL_COL);
            appCol.setMaxWidth(85);
            appCol.setPreferredWidth(85);

            appCol = cols.getColumn(NW_LATENCY_COL);
            appCol.setMaxWidth(85);
            appCol.setPreferredWidth(85);

            // Adding background data
            appCol = cols.getColumn(SYN_COL);
            appCol.setMaxWidth(0);
            appCol.setMinWidth(0);
            appCol.setPreferredWidth(0);

            appCol = cols.getColumn(SYN_ACK_COL);
            appCol.setMaxWidth(0);
            appCol.setMinWidth(0);
            appCol.setPreferredWidth(0);

        }
        return cols;
    }

    /**
     * This is the one method that must be implemented by subclasses. This method
     * defines how the data object managed by this table model is mapped to its
     * columns when displayed in a row of the table. The getValueAt() method uses
     * this method to retrieve table cell data.
     * 
     * @param item
     *            A object containing the column information. columnIndex The index
     *            of the specified column.
     * 
     * @return An object containing the table column value.
     */
    @Override
    protected Object getColumnValue(Session item, int columnIndex) {

    	String latency = getLatency(item);
    	
        switch (columnIndex) {
            case TIME_COL:
                if (item.isUdpOnly()) {
                    return item.getUdpPackets().get(0).getTimeStamp();
                } else {
                    return item.getPackets().get(0).getTimeStamp();
                }
            case CHECKBOX_COL:
                String sessionKey = getSessionKey(item);
                return getCheckboxMap().get(sessionKey);
            case DOMAIN_COL:

                return item.getDomainName();

            case LOCALPORT_COL:

                return ResourceBundleHelper.getMessageString("tcp.localhost") + HOSTPORTSEPERATOR + item.getLocalPort();

            case REMOTEIP_COL:

                return item.getRemoteIP().getHostAddress();

            case REMOTEPORT_COL:
                return item.getRemotePort();

            case BYTE_COUNT_COL:
                return item.getBytesTransferred();

            case PACKETCOUNT_COL:
                if (item.isUdpOnly()) {
                    return item.getUdpPackets().size();
                } else {
                    return item.getPackets().size();
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
					return latency;
                } else {
                    return "N/A";
                }
            case SYN_COL: {
                if (!item.isUdpOnly() && !item.getSynAckPackets().isEmpty() && !item.getSynPackets().isEmpty()) {
                    return synTime;
                } else {
                    return 0.0d;
                }
            }
            case SYN_ACK_COL: {
                if (!item.isUdpOnly() && !item.getSynAckPackets().isEmpty() && !item.getSynPackets().isEmpty()) {
                    return synAckTime;
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

    /*
     * Returns calculated Latency value
     * 
     * Packet Sequence : SYN - SYNACK Latency = First SYNACK time - First SYN time
     * 
     * Packet Sequence: SYN1 -SYN2 - SYNACK Latency = SYNACK time - SYN2 time
     * 
     * Packet Sequence: SYN1 - SYNACK - SYN2 Latency = SYNACK time - SYN1 time
     * 
     */

    private String getLatency(Session session) {
        double latencyValue = 0.0;
        String latency = "N/A";
        calculateSynAckTimestamp(session.getSynAckPackets());
        if (synAckTime > 0.0) {
            calculateSynTimestamp(session.getSynPackets(), synAckTime);
            if (synAckTime >= synTime) {
                latencyValue = synAckTime - synTime;
                latency = latencyValue > 0 ? Util.formatDoubleToMicro(latencyValue) : "0.0";
            } else {
                LOGGER.debug("Negative latency value : " + session.getSessionKey());
            }
        }
        return latency;
    }

    /*
     * Returns the timestamp of the packet with last SYN before the SYNACK
     */
    private void calculateSynTimestamp(TreeMap<Double, PacketInfo> synPackets, double syncAckTime) {
        if (synPackets.containsKey(syncAckTime)) {
            synTime = syncAckTime;
        } else {
            Entry<Double, PacketInfo> synEntry = synPackets.lowerEntry(syncAckTime);
            if (synEntry != null) {
                synTime = synEntry.getKey();
            } else {
                LOGGER.debug("Packet info error : No SYN's found before the SYNACK - " + syncAckTime);
            }
        }
    }

    /*
     * Returns the timestamp of the first packet with the SYNACK flag
     */
    private void calculateSynAckTimestamp(TreeMap<Double, PacketInfo> synAckPackets) {
        for (PacketInfo packetInfo : synAckPackets.values()) {
            synAckTime = packetInfo.getTimeStamp();
            break;
        }
    }

}