package com.att.aro.core.videoanalysis.csi;

import com.att.aro.core.videoanalysis.csi.pojo.CSIManifestAndState;

public interface ICSIDataHelper {
	public CSIManifestAndState readData(String tracePath);
	public void saveData(String path, CSIManifestAndState CSIData);
	public boolean doesCSIFileExist(String path);
}
