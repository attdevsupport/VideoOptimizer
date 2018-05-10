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