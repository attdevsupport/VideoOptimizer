/*
 * Copyright 2017 AT&T
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
package com.att.arocollector.privatedata;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

abstract class AbstractDeviceDataCollector {

	private BufferedWriter bufferedWriter;
	private FileOutputStream fileOutputStream;
	private String dataFilePath;
	private File dataFile;
	private static final String TAG = AbstractDeviceDataCollector.class.getSimpleName();

	protected AbstractDeviceDataCollector(String dataFilePath) {
		
		this.dataFilePath = dataFilePath;
	}
	
	abstract List<NameValuePair> getData();
	
	void collect() {
		
		List<NameValuePair> data = getData();
		
		initFile();
		writeToFile(data);
		closeFile();
	}
	
	private void initFile() {
		
		dataFile = new File(dataFilePath);
		
		Log.i(TAG, "create file:" + dataFile.getAbsolutePath());
		
		try {
			
			fileOutputStream = new FileOutputStream(dataFile, true);
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
			
		} catch (FileNotFoundException e) {

			fileOutputStream = null;
			bufferedWriter = null;
			Log.d(TAG, "initFile() FileNotFoundException:" + e.getMessage());
		}
	}
	
	private void writeToFile(List<NameValuePair> data) {

		if (bufferedWriter != null) {
		
			try {
				
				String eol = System.lineSeparator();
				StringBuilder strBuilder = new StringBuilder();
				
				for (NameValuePair dataItem: data) {

					strBuilder.append(PrivateDataCollectionConst.KEYWORD_CATEGORY); 
					strBuilder.append(PrivateDataCollectionConst.COLUMN_DELIMITER);
					strBuilder.append(dataItem.getName());
					strBuilder.append(PrivateDataCollectionConst.COLUMN_DELIMITER);
					
					/*
					 * Save value as an empty String if it is null 
					 * to avoid search problem in ARO.Core.
					 */
					String value = dataItem.getValue();
					value = value == null? "" : value;
					
					strBuilder.append(value);
					strBuilder.append(PrivateDataCollectionConst.COLUMN_DELIMITER);
					strBuilder.append(PrivateDataCollectionConst.YES_SELECTED); // yes - meaning will be searched for during analysis
					strBuilder.append(eol);
				}
				
				bufferedWriter.write(strBuilder.toString());
				bufferedWriter.flush();
			
			} catch (IOException e) {

				Log.d(TAG, "writeToFile() IOException:" + e.getMessage());
			}
		}
	}
	
	private void closeFile(){
		
		Log.i(TAG, "close file :"+ dataFile);
		
		try {
		
			bufferedWriter.close();
			fileOutputStream.close();
		
		} catch (IOException e) {
			
			Log.d(TAG, "closeFile() <"+dataFile+"> IOException :"+e.getMessage());
		}
	}

	
}
