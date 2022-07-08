/*
 *  Copyright 2022 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.ui.view.videotab;


import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.att.aro.ui.model.listener.VideoStreamMenuItemListener;
import com.att.aro.ui.utils.ResourceBundleHelper;


public class ExportManifestDialog extends JPanel {
    private static final long serialVersionUID = 1L;


    public ExportManifestDialog(VideoManifestPanel videoManifestPanel) {

        setLayout(new FlowLayout());
        setName(ExportManifestDialog.class.getName());
        JButton exportBtn = new JButton(ResourceBundleHelper.getMessageString("videoTab.export"));

        exportBtn.addActionListener(new VideoStreamMenuItemListener(videoManifestPanel.getSegmentTableList()));
        add(exportBtn);
    }
}
