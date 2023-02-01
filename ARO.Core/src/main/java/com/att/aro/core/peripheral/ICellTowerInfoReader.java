package com.att.aro.core.peripheral;

import java.util.List;

import com.att.aro.core.peripheral.pojo.CellInfo;

public interface ICellTowerInfoReader {

	List<CellInfo> readData (String directory); 
}
