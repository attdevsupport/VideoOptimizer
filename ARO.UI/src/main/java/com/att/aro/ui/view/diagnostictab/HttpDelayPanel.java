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
package com.att.aro.ui.view.diagnostictab;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.att.aro.core.packetanalysis.IHttpRequestResponseHelper;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.model.diagnostic.HttpDelayTableModel;

import lombok.Data;

/**
 * Initializes a new instance of the HttpDelayPanel class under TCP/UDP table.
 */
@Data
public class HttpDelayPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private JScrollPane scrollPane;
	private DataTable<HttpRequestResponseInfo> httpDelayTable;
	private HttpDelayTableModel httpDelayTableModel = new HttpDelayTableModel();

	public HttpDelayTableModel getHttpDelayTableModel() {
		return httpDelayTableModel;
	}

	private IHttpRequestResponseHelper httpHelper = ContextAware.getAROConfigContext()
			.getBean(IHttpRequestResponseHelper.class);


	/**
	 * The default constructor that initializes a new instance of the
	 * HttpDelayPanel class.
	 * 
	 * @param tab
	 */
	public HttpDelayPanel() {
		setLayout(new BorderLayout());
		add(getScrollPane(), BorderLayout.CENTER);

	}

	/**
	 * Returns the ScrollPane that contains the RequestResponse table.
	 */
	private JScrollPane getScrollPane() {
		if (scrollPane == null) {
			scrollPane = new JScrollPane(getHttpDelayTable());
		}
		return scrollPane;
	}

	/**
	 * Initializes and returns the the DataTable that contains HTTP delay informations.
	 */
	public DataTable<HttpRequestResponseInfo> getHttpDelayTable() {
		if (httpDelayTable == null) {
			httpDelayTable = new DataTable<HttpRequestResponseInfo>(httpDelayTableModel);
			httpDelayTable.setName("httpDelayTable");
			httpDelayTable.setAutoCreateRowSorter(true);
			httpDelayTable.setGridColor(Color.LIGHT_GRAY);
			TableRowSorter<TableModel> sorter = new TableRowSorter<>(httpDelayTable.getModel());
			httpDelayTable.setRowSorter(sorter);
		}
		return httpDelayTable;
	}

	

	public void updateTable(Session session) {
		httpDelayTableModel.refresh(session);
	}

}