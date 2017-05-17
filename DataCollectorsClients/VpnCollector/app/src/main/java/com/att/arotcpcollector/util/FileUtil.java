/*
 *
 *   Copyright 2017 AT&T
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package com.att.arotcpcollector.util;

import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class FileUtil {

	static OutputStreamWriter outputWriter;
	static FileOutputStream fileout;
	static int counter = 0;

	public static void init() throws FileNotFoundException {
		String logfile = "Intermediate.txt";
		logfile = logfile.replace(' ', '_');
		File sdCard = Environment.getExternalStorageDirectory();
		File dir = new File(sdCard.getAbsolutePath() + "/udpTesting");
		if (!dir.exists()) {
			dir.mkdirs();
		}

		File file = new File(dir, logfile);
		fileout = new FileOutputStream(file);
		outputWriter = new OutputStreamWriter(fileout);
	}

	public static void print(String logEntry, Object bytearray)
			throws FileNotFoundException {
		if (outputWriter == null) {
			init();
		}
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
			Date date = Calendar.getInstance().getTime();
			if (bytearray == null) {
				// outputWriter.write(logEntry + " " + dateFormat.format(date)
				// +"\n");
			} else if (bytearray instanceof byte[]) {
				String data = new String((byte[]) bytearray);
				if (data.contains("Packet")) {
					int start = data.indexOf("Packet");
					int end = data.indexOf("Packet") + 14 < data.length() ? data
							.indexOf("Packet") + 14 : data.length();
					outputWriter.write(logEntry + " , "
							+ data.substring(start, end) + " , "
							+ dateFormat.format(date) + "\n");
				}
			} else if (bytearray instanceof Integer) {
				outputWriter.write(counter + " , " + logEntry + " : "
						+ ((Integer) bytearray) + "\n");
			}
		} catch (IOException ioe) {
		}
	}

	public static void printByteArray(String logEntry, Object bytearray)
			throws FileNotFoundException {
		if (outputWriter == null) {
			init();
		}
		try {
			if (bytearray == null) {
				// outputWriter.write(logEntry + " " + dateFormat.format(date)
				// +"\n");
			} else if (bytearray instanceof byte[]) {
				StringBuilder output = new StringBuilder();
				for (int i = 0; i < ((byte[]) bytearray).length; i++) {
					output.append(((byte[]) bytearray)[i]);
				}
				outputWriter.write(output.toString() + "\n");
			} else if (bytearray instanceof Integer) {
				outputWriter.write(counter + " , " + logEntry + " : "
						+ ((Integer) bytearray) + "\n");
			}
		} catch (IOException ioe) {
		}
	}

	public static void printByteArray(String filename, byte[] bytearray, int from, int to)
			throws FileNotFoundException {
		File dir = getTestFolder("/ARO");
		File file = new File(dir, filename);
		fileout = new FileOutputStream(file, true);
		outputWriter = new OutputStreamWriter(fileout);
		try {
			if (bytearray != null) {
				StringBuilder output = new StringBuilder();
				for (int i = from; i < to; i++) {
					output.append(i + ": " + bytearray[i]+ ", ");
				}
				outputWriter.write(output.toString() + "\n");
				outputWriter.close();
				fileout.close();
			}
		} catch (IOException ioe) {
		}
	}

	public static void print(String fileName, String line) {
		try {
			File dir = getTestFolder("/ARO");
			File file = new File(dir, fileName);
			fileout = new FileOutputStream(file, true);
			outputWriter = new OutputStreamWriter(fileout);
			fileout.write(line.getBytes());
			outputWriter.close();
			fileout.close();
		} catch (FileNotFoundException fnf) {
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.getCause();
		}

	}

	public static FileOutputStream initSSLKey(String sslKeysFile) throws FileNotFoundException {
		File dir = getTestFolder("/ARO");
		File file = new File(dir, sslKeysFile);
		FileOutputStream fileout = new FileOutputStream(file, true);
		return fileout;
	}

	public static void printSSLKeys(FileOutputStream fileout, byte[] key) {
		try {
			fileout.write(key);
		} catch (FileNotFoundException fnf) {
		} catch (IOException e) {
		}
	}
	
	public static void terminate(FileOutputStream fileout) {
		if (fileout != null) {
			try {
				fileout.close();
			} catch (IOException ioe) {
			}
		}
	}

	public static void printKey(String folder, String fileName, String line) {
		try {
			File dir = getTestFolder("/" + folder);
			File file = new File(dir, fileName);
			fileout = new FileOutputStream(file, true);
			outputWriter = new OutputStreamWriter(fileout);
			fileout.write(line.getBytes());
			outputWriter.close();
			fileout.close();
		} catch (FileNotFoundException fnf) {
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.getCause();
		}

	}

	public static File getTestFolder(String subfolder) {
		File sdCard = Environment.getExternalStorageDirectory();
		File dir = new File(sdCard.getAbsolutePath() + subfolder);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	public static File getSubFolder(String subfolder) {
		File sdCard = Environment.getExternalStorageDirectory();
		File dir = new File(sdCard.getAbsolutePath() + subfolder);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	public static void printSecretKey(String fileName, String secretKey) {
		try {
			File dir = getSubFolder("/ARO");
			File file = new File(dir, fileName);
			fileout = new FileOutputStream(file, true);
			outputWriter = new OutputStreamWriter(fileout);
			fileout.write(secretKey.getBytes());
			outputWriter.close();
			fileout.close();
		} catch (FileNotFoundException fnf) {
		} catch (IOException e) {
			e.getCause();
		}
	}
	
	public static void closeSecretKeyFile(){
		try {
			outputWriter.close();
			fileout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
