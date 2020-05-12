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
package com.att.aro.core.fileio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Reading file as a whole string, string array of line, byte array etc.
 * Date: April 18, 2014
 */
public interface IFileManager {
	/**
	 * read file line by line and return an array of string
	 * 
	 * @param filepath
	 *            full path of file to read
	 * @return array of String of line
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	String[] readAllLine(String filepath) throws IOException;

	boolean isFile(String filepath);

	boolean fileExist(String path);

	boolean directoryExist(String directoryPath);

	boolean directoryExistAndNotEmpty(String directoryPath);

	boolean directoryDeleteInnerFiles(String directoryPath);
	boolean deleteFolderContents(String folderPath);
	boolean fileDirExist(String filepath);

	String[] list(String directoryPath, FilenameFilter filter);

	long getLastModified(String filepath);
	
	/**
	 * Given a file path, returns file created time in seconds
	 * @param filePath - full path of the file 
	 * @return file created time in seconds
	 */
	long getCreatedTime(String filePath) throws IOException;

	String getDirectory(String filepath);

	File createFile(String filepath);

	File createFile(String parent, String child);

	/**
	 * Creates a directory
	 * 
	 * @param path
	 */
	void mkDir(String path);

	/**
	 * flush and close the OutputStream
	 * 
	 * @param outputStream
	 * @throws IOException
	 */
	void closeFile(FileOutputStream fileOutputStream) throws IOException;

	InputStream getFileInputStream(String filepath) throws FileNotFoundException;

	InputStream getFileInputStream(File file) throws FileNotFoundException;

	OutputStream getFileOutputStream(String filepath) throws FileNotFoundException;

	/**
	 * Finishes writing the output stream, then closes output stream
	 * 
	 * @param iStream
	 * @param location
	 * @throws IOException
	 */
	void saveFile(InputStream iStream, String location) throws IOException;

	/**
	 * Delete a file
	 * 
	 * @param path
	 * @return
	 */
	boolean deleteFile(String path);

	/**
	 * Rename a file
	 * 
	 * @param origFileName
	 * @param newName
	 * @return
	 */
	boolean renameFile(File origFileName, String newName);

	/**
	 * Returns a String array of files/folders within a folder
	 * 
	 * @param folderPath
	 * @param extention
	 * @return
	 */
	String[] findFilesByExtention(String folderPath, String extention);

	String readAllData(String filepath) throws IOException;
	String readAllData(BufferedReader reader) throws IOException;
	
	/**
	 * Find the real path from an alias, if it is an alias
	 * 
	 * @param tracePath
	 * @return validated real path
	 */
	File deAlias(File tracePath);
	
	String[] findFiles(String localVidsFolder, String fileName);

}
