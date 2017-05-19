package com.att.aro.ui.view.videotab;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JEditorPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.TableCellRenderer;

class WordWrapRenderer extends JTextArea implements TableCellRenderer{	
		JEditorPane editor;
		int rowSelected=-1;
		
		public WordWrapRenderer(){
			setLineWrap(true);
			setWrapStyleWord(true);
		}
		
		public WordWrapRenderer(int rowSelected){
			this();
			this.rowSelected = rowSelected;
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			String html = value.toString();
			setText(html);
			if (row == rowSelected) {
				this.setForeground(Color.gray);
			} else {
				this.setForeground(Color.blue);
			}
			setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
			if (table.getRowHeight(row) != getPreferredSize().height) {
				table.setRowHeight(row, getPreferredSize().height);
			}
			return this;
		}

	}