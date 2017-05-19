package com.att.aro.ui.view.menu.tools;

import java.awt.Color;

import javax.swing.border.Border;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

class RoundedBorderPanel implements Border {
		final int radius = 25;
	    Color color;
		public RoundedBorderPanel(Color color){
			this.color = color;
		}
		@Override
		public void paintBorder(Component component, Graphics graphics, int xPoint, int yPoint, int width, int height) {		
				Color color = graphics.getColor();
				graphics.setColor(this.color);
				graphics.fillRoundRect(xPoint, yPoint, width, height, radius, radius);
				graphics.setColor(color);
			
			graphics.drawRoundRect(xPoint, yPoint, width - 1, height - 1, radius, radius);
		}

		@Override
		public Insets getBorderInsets(Component c) {
			return new Insets(5, 5, 5, 5);
		}

		@Override
		public boolean isBorderOpaque() {
			return true;
		}
	}