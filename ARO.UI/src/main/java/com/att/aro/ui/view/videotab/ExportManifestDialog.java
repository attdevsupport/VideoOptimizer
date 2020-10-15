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

        exportBtn.addActionListener(new VideoStreamMenuItemListener(null, videoManifestPanel.getSegmentTableList()));
        add(exportBtn);
    }
}
