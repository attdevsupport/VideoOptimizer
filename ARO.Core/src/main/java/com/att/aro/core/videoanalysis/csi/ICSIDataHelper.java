package com.att.aro.core.videoanalysis.csi;

import java.io.File;

import com.att.aro.core.videoanalysis.csi.pojo.CSIManifestAndState;

public interface ICSIDataHelper {
	public CSIManifestAndState readData(String tracePath);
	public void saveData(String path, CSIManifestAndState CSIData);
	public boolean doesCSIFileExist(String path);
	public File generateManifestPath(String traceDirectory, String fileName);
}
