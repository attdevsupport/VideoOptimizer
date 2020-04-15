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
package com.att.aro.core.fileio.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.util.Util;

/**
 * Helper class for reading file. Return result as String, array of string or
 * byte array
 * Date: April 18, 2014
 *
 */
public class FileManagerImpl implements IFileManager {

	private static final Logger LOGGER = LogManager.getLogger(FileManagerImpl.class.getName());
	
	@Autowired
	private IExternalProcessRunner extrunner;

	@Override
	public String[] readAllLine(String filepath) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filepath));
		String[] arrlist = readAllLine(reader);
		reader.close();
		return arrlist;
	}

	public String[] readAllLine(BufferedReader reader) {
		List<String> list = new ArrayList<String>();
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				list.add(line);
			}
		} catch (IOException e) {
			LOGGER.error("error reading data from BufferedReader", e);
		}
		String[] arrlist = list.toArray(new String[list.size()]);

		return arrlist;
	}

	@Override
	public String readAllData(String filepath) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filepath));
		String dString = readAllData(reader);
		reader.close();
		return dString;
	}

	public String readAllData(BufferedReader reader) {
		StringBuilder temp = new StringBuilder();
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				temp.append(line);
			}
		} catch (IOException e) {
			LOGGER.error("error reading data from BufferedReader", e);
		}

		return temp.toString();
	}

	@Override
	public boolean fileExist(String path) {
		File file = new File(path);
		return file.exists();
	}

	@Override
	public boolean isFile(String filepath) {
		File file = new File(filepath);
		return file.isFile();
	}

	@Override
	public File createFile(String filepath) {
		return new File(filepath);
	}

	@Override
	public File createFile(String parent, String child) {
		return new File(parent, child);
	}

	@Override
	public void mkDir(String path) {
		mkDir(new File(path));
	}

	public void mkDir(File dirinfo) {
		if (!dirinfo.exists()) {
			dirinfo.mkdirs();
		}
	}

	@Override
	public boolean directoryExist(String directoryPath) {
		File dir = new File(directoryPath);
		return (dir.exists() && dir.isDirectory());
	}

	@Override
	public boolean directoryExistAndNotEmpty(String directoryPath) {
		return directoryExistAndNotEmpty(createFile(directoryPath));
	}

	public boolean directoryExistAndNotEmpty(File directory) {
		if (directory.exists() && directory.isDirectory()) {
			String[] list = directory.list();
			return list != null && list.length > 0;
		}
		return false;
	}
	
	@Override
	public boolean deleteFolderContents(String folderPath) {

		String[] files = list(folderPath, null);
		boolean delResult = true;
		for (String file : files) {
			String filepath = folderPath + Util.FILE_SEPARATOR + file;
			if (directoryExistAndNotEmpty(filepath)) {
				deleteFolderContents(filepath);
			}
			boolean tempResult = deleteFile(filepath);
			delResult = delResult && tempResult;
			LOGGER.debug("delete :" + file + (tempResult ? " deleted" : " failed"));
		}
		return delResult;
	}

	
	@Override
	public boolean directoryDeleteInnerFiles(String directoryPath) {
		if ((Util.isWindowsOS() && ("C:\\".equals(directoryPath) || "C:".equals(directoryPath)))
				|| "/".equals(directoryPath)) {
			LOGGER.error("Illegal attempt to delete files in " + directoryPath);
			return false;
		}
		try {
			File directory = new File(directoryPath);
			if (!directory.exists()) {
				return false;
			}
			FileUtils.cleanDirectory(directory);
		} catch (IOException ex) {
			return false;
		}
		return true;
	}
	
	@Override
	public String[] findFiles(String path, final String fileName) {

		if (path==null || fileName ==null) {
			return new String[] {};
		}
		String name1 = StringUtils.substringBeforeLast(fileName, ".");
		String extension1 = StringUtils.substringAfterLast(fileName, ".");

		String[] files = list(path, new FilenameFilter() {

			private boolean fileRes;

			@Override
			public boolean accept(File dir, String file) {
				String prefix = StringUtils.substringBeforeLast(file, ".");
				String extension = StringUtils.substringAfterLast(file, ".");
				fileRes = (!name1.isEmpty()) ? prefix.startsWith(name1) : true;
				fileRes = fileRes && (!extension1.isEmpty()) ? extension.equals(extension1) : fileRes;
				return fileRes;
			}
		});
		Arrays.sort(files);
		return files;
	}
	
	@Override
	public String[] findFilesByExtention(String localVidsFolder, final String extention) {
		String[] files = list(localVidsFolder, new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(extention);
			}
		});
		return files;
	}

	@Override
	public String[] list(String directoryPath, FilenameFilter filter) {
		File dir = new File(directoryPath);
		if (dir.exists() && dir.isDirectory()) {
			return dir.list(filter);
		}
		return new String[0];
	}

	@Override
	public long getLastModified(String filepath) {
		File file = new File(filepath);
		return file.lastModified();
	}
	
	public long getCreatedTime(String filePath) throws IOException {
		File file = new File(filePath);
		FileTime fileTime = (FileTime) Files.getAttribute(file.toPath(), "creationTime", LinkOption.NOFOLLOW_LINKS);
		if (fileTime != null) {
			return fileTime.to(TimeUnit.SECONDS);
		}
		return 0;
	}

	@Override
	public String getDirectory(String filepath) {
		File file = new File(filepath);
		if (file.exists()) {
			if (file.isDirectory()) {
				return filepath;
			} else {
				return file.getParent();
			}
		}
		return null;
	}

	@Override
	public boolean fileDirExist(String filepath) {
		File targetFile = new File(filepath);
		File parent = targetFile.getParentFile();
		if (parent.exists()) {
			return true;
		}
		return false;
	}

	@Override
	public InputStream getFileInputStream(String filepath) throws FileNotFoundException {
		return new FileInputStream(filepath);
	}

	@Override
	public InputStream getFileInputStream(File file) throws FileNotFoundException {
		return new FileInputStream(file);
	}

	@Override
	public OutputStream getFileOutputStream(String filepath) throws FileNotFoundException {
		return new FileOutputStream(filepath);
	}

	/**
	 * flush and close the OutputStream
	 * 
	 * @param outputStream
	 * @throws IOException
	 */
	@Override
	public void closeFile(FileOutputStream fileOutputStream) throws IOException {
		fileOutputStream.flush();
		fileOutputStream.close();
	}

	/**
	 * Write an input stream to a given location.
	 */
	@Override
	public void saveFile(InputStream iStream, String location) throws IOException {

		this.saveFile(iStream, getFileOutputStream(location));
	}

	private void saveFile(InputStream iStream, OutputStream oStream) throws IOException {

		try {
			byte[] buffer = new byte[4096];
			int length;
			while ((length = iStream.read(buffer)) != -1) {
				oStream.write(buffer, 0, length);
			}

		} finally {
			if (oStream != null) {
				oStream.close();
			}
		}
	}

	@Override
	public boolean deleteFile(String path) {
		boolean success = false;
		if(fileExist(path)){
			success = createFile(path).delete();
		}
		return success;
	}

	/**
	 * rename a File with a newName
	 * 
	 * @param origFileName
	 *            a File
	 * @param newName
	 *            a String containing a new name
	 * @return
	 */
	@Override
	public boolean renameFile(File origFileName, String newName) {
		String path = origFileName.getParent();
		File renameFile = createFile(path, newName);
		
		return (!renameFile.exists() && origFileName.renameTo(renameFile));
	}

	@Override
	public File deAlias(File tracePath) {
		if (tracePath == null) {
			return tracePath;
		}
		if (Util.isMacOS()) {
			String cmd ="if [ -f \"%path%\" -a ! -L \"%path%\" ]; then\n"
					  + " item_name=`basename \"%path%\"`\n"
					  + " item_parent=`dirname \"%path%\"`\n"
					  + " item_parent=\"`cd \\\"${item_parent}\\\" 2>/dev/null && pwd || echo \\\"${item_parent}\\\"`\"\n"
					  + " item_path=\"${item_parent}/${item_name}\"\n"
					  + " line_1='tell application \"Finder\"'"
					  + " line_2='set theItem to (POSIX file \"'${item_path}'\") as alias'\n"
					  + " line_3='if the kind of theItem is \"alias\" then'\n"
					  + " line_4='get the posix path of (original item of theItem as text)'\n"
					  + " line_5='end if'\n"
					  + " line_6='end tell'\n"
					  + " orig=`osascript -e \"$line_1\" -e \"$line_2\" -e \"$line_3\" -e \"$line_4\" -e \"$line_5\" -e \"$line_6\"`\n"
					  + " echo \"$orig\"\n"
					  + "fi\n";
			cmd = cmd.replaceAll("%path%", tracePath.getAbsolutePath());
			String results = extrunner.executeCmd(cmd);
			if (StringUtils.isEmpty(results) || results.contains("execution error:")) {
				LOGGER.error("shell error:" + results);
				return tracePath;
			}
			results = results.replaceAll("\\s*$", "");
			return new File(results);
		} else if (Util.isWindowsOS()) {
			Path oPath;
			try {
				oPath = Files.readSymbolicLink(tracePath.toPath());
				tracePath = oPath.toFile();
				return tracePath;
			} catch (IOException e) {
				return tracePath;
			}

		}  else {
			return tracePath;
		}
	}
}