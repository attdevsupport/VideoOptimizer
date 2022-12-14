/*
 *  Copyright 2015 AT&T
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
package com.att.aro.ui.view.video;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.JOptionPane;

import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;

/**
 * This is a util class for the common methods for the video.
 * 
 *
 *
 */
public final class VideoUtil {

	public static final int FRAME_RATE = 25;
	
	public static final int PLAYER_CONTENT_WIDTH_PORTRAIT = 351;
	public static final int PLAYER_CONTENT_HEIGHT_PORTRAIT = 624;
    public static final int PLAYER_CONTENT_WIDTH_LANDSCAPE = 512;
	public static final int PLAYER_CONTENT_HEIGHT_LANDSCAPE = 288;
	
	public static final String WIN_VLCLIBPATH = "C:\\Program Files\\VideoLAN\\VLC";
    public static final String LINUX_VLCLIBPATH = "/usr/lib/vlc";
    
	private static File existingExternalVideoFile = null;
	private static boolean isMediaConversionError = false;
	private static final String operatingSystem = System.getProperty("os.name");

	public enum FileTypeEnum {
		QT, WMV, WMA, MPEG, _3GP, ASF, AVI, ASf, DV, MKV, MPG, RMVB, VOB, MOV, MP4;

	}

	/**
	 * Retrieves the media url from provided trace directory file after
	 * converting the file type from MP4 to MOV.
	 * 
	 * @param traceDirectory
	 *            {@link File} object that contains the trace directory.
	 * @return URL in {@link String} format.
	 * @throws IOException
	 */
	public static String getMediaUrl(File traceDirectory) throws IOException {
		String result = null;
		if ((traceDirectory != null) && (traceDirectory.isDirectory() || isPcaPFile(traceDirectory))) {
			if (isPcaPFile(traceDirectory)) {
				traceDirectory = traceDirectory.getParentFile();
			}
			File videoFile = new File(traceDirectory, "video.mov"); // don't care that this is hard coded
			int totalVideoFiles = getVideoFilesCount(traceDirectory);
			if ((totalVideoFiles > 0) || !videoFile.exists() || isMediaConversionError) {
				File videoFileFromDevice = new File(traceDirectory, "video.mp4" ); // don't care that this is hard coded

				if (totalVideoFiles > 1) {
					// if more then one media file exists in directory
					// alert message to user # of video file exists.
					MessageDialogFactory.showMessageDialog(null,
							ResourceBundleHelper.getMessageString("video.error.multipleVideoFiles"));

				} else if (totalVideoFiles == 1) {
					// convert the file to .mov file
					File exVideoFile = new File(traceDirectory,
							ResourceBundleHelper.getMessageString("video.exVideoDisplayFile"));
					convertVideoToMOV(traceDirectory, existingExternalVideoFile, exVideoFile);
					if (exVideoFile.canRead()) {
						result = "file:" + exVideoFile.getAbsolutePath();
					}

				} else if ((videoFileFromDevice.exists()) || (totalVideoFiles == 0)) {

					File exVideoMov = getExVideoMovIfPresent(traceDirectory);
					if (exVideoMov != null && exVideoMov.canRead()) {
						result = "file:" + exVideoMov.getAbsolutePath();
					}
				}
			}
			if ((videoFile.canRead()) && (totalVideoFiles == 0)) {
				result = "file:" + videoFile.getAbsolutePath();
			}
		}
		return result;
	}

	/**
	 * This method return true if the input file is PCAP.
	 * 
	 * @param pcapFilename
	 *            pcapFilename.
	 * @return boolean true if pcap else false.
	 */

	public static boolean isPcaPFile(File pcapFilename) {
		String extension = "";

		String filePath = pcapFilename.getPath();
		int dot = filePath.lastIndexOf(".");
		if (dot > 0) {
			extension = filePath.substring(dot + 1);
		}
		if ((extension.equals("cap")) || (extension.equals("pcap"))) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * This method returns external video files count.
	 * 
	 * @param traceDirectory,source,destination
	 *            {@link File} object that contains the trace directory.
	 * @return count of external video files.
	 */
	private static int getVideoFilesCount(File traceDirectory) {
		int totalVideoFile = 0;
		if ((traceDirectory != null) && (traceDirectory.isDirectory())) {
			File[] files = traceDirectory.listFiles();

			if(files == null) {
				return totalVideoFile;
			}
			
			for (File file : files) {
				if (!file.isDirectory()) {
					String fileExtension = getExtension(file.getName());
					if (fileExtension != null && file.getName().startsWith("._")) {
						try {
							if (fileExtension.equals("3gp")) {
								fileExtension = "_3GP";
							}
							switch (FileTypeEnum.valueOf(fileExtension.toUpperCase())) {
							case QT:
							case WMV:
							case WMA:
							case MPEG:
							case _3GP:
							case ASF:
							case AVI:
							case ASf:
							case DV:
							case MKV:
							case MPG:
							case RMVB:
							case VOB:
							case MOV:
							case MP4:
								if (!file.getName().equals(TraceDataConst.FileName.VIDEO_MP4_FILE) 
										&& !file.getName().equals(TraceDataConst.FileName.VIDEO_MOV_FILE)
										&& !file.getName().equals("exvideo.mov")) {
									existingExternalVideoFile = file;
									totalVideoFile++;
								}
								break;
							default:
								break;
							}
						} catch (IllegalArgumentException iAEx) {

							continue;
						}
					} // end of if (fileExtension != null)
				}
			}
		} // end of if ((traceDirectory != null)
		return totalVideoFile;
	}

	/**
	 * Returns file extension.
	 * 
	 * @param fileName
	 * @return extension.
	 */
	private static String getExtension(String fileName) {
		int extensionIndex = fileName.lastIndexOf(".");
		if (extensionIndex == -1) {
			return null;
		}
		return fileName.substring(extensionIndex + 1, fileName.length());
	}

	/**
	 * Converts the external video source, MP4,WMA to MOV format.
	 * 
	 * @param traceDirectory,source,destination
	 *            {@link File} object that contains the trace directory. source:
	 *            video file to be converted. destination: Converted video file
	 *            with .MOV format.
	 * @throws IOException
	 */

	private static void convertVideoToMOV(File traceDirectory, File source, File destination) throws IOException {
		System.out.println("convertVideoToMOV( src:"+source+", dest:"+destination+")");
		String strProgramName = ResourceBundleHelper.getMessageString("video.converter.programName");
		if (operatingSystem.startsWith("Mac")) {
			strProgramName = ResourceBundleHelper.getMessageString("video.converter.programNameMac");
		}
		File fileFullPathFFMPEGProgram = new File(traceDirectory, strProgramName);

		// Overwrite any existing ffmpeg file in trace
		if (fileFullPathFFMPEGProgram.exists()) {
			fileFullPathFFMPEGProgram.delete();
		}
		InputStream is = AROVideoPlayer.class.getClassLoader().getResourceAsStream(strProgramName);
		FileOutputStream fos = new FileOutputStream(fileFullPathFFMPEGProgram);
		
		try {
			byte[] buf = new byte[2048];
			int i;
			while ((i = is.read(buf)) > 0) {
				fos.write(buf, 0, i);
			}
			buf = null;
		} catch (Exception e) {
			System.out.println("src:"+source+", dest:"+destination+", e.getMessage() :" + e.getMessage());
			e.printStackTrace();
		} finally {
			fos.close();
		}
		
		if (!fileFullPathFFMPEGProgram.canExecute()) {
			fileFullPathFFMPEGProgram.setExecutable(true);
		}

		try {
			convertMp4ToMov(source, destination, fileFullPathFFMPEGProgram.getAbsolutePath());
		} finally {
			fileFullPathFFMPEGProgram.delete();
		}

	}

	/**
	 * Convert the MP4 file into Mov format.
	 * 
	 * @param videoInputFile
	 * @param videoOutputFile
	 * @param strFullPathConvertProgram
	 * @throws IOException
	 */
	private static void convertMp4ToMov(File videoInputFile, File videoOutputFile, String strFullPathConvertProgram)
			throws IOException {

		if (!videoInputFile.exists()) {
			/*
			 * logger.fine(rb.getString("video.error.inputFileDoesNotExist") +
			 * " " + videoInputFile.toString());
			 */
			return;
		} else if (videoInputFile.isDirectory()) {
			/*
			 * logger.fine(rb.getString("video.error.inputFileIsADirectory") +
			 * " " + videoInputFile.toString());
			 */
			return;
		}

		String stdErrLine;
		if (videoOutputFile.exists()) {
			if (!videoOutputFile.isDirectory()) {
				if (videoOutputFile.canWrite()) {
					System.gc();
					boolean bSuccess = videoOutputFile.delete();
					if (!bSuccess) {
						/*
						 * logger.warning("Failed in deletion of output file " +
						 * videoOutputFile.toString());
						 */
						return;
					}
				} else {
					/*
					 * logger.warning(rb .getString(
					 * "video.error.lackPermissionToWriteToConvertFile") + " " +
					 * videoOutputFile.toString());
					 */
					return;
				}
			} else {
				/*
				 * logger.warning(rb
				 * .getString("video.error.outputFileIsADirectory") +
				 * videoOutputFile.toString());
				 */
				return;
			}
		}

		if (videoOutputFile.exists()) {
			/*
			 * logger.warning(rb
			 * .getString("video.error.priorFileVersionCannotBeDeleted") + " " +
			 * videoOutputFile.toString());
			 */
			return;
		}

		String[] aConvertProgramParameters = ResourceBundleHelper.getMessageString("video.converter.programParameters")
				.split(" ");
		String[] aArgs = new String[aConvertProgramParameters.length + 4];
		aArgs[0] = strFullPathConvertProgram;
		aArgs[1] = "-i";
		aArgs[2] = videoInputFile.getAbsolutePath();
		for (int iIdx = 0; iIdx < aConvertProgramParameters.length; iIdx++) {
			aArgs[3 + iIdx] = aConvertProgramParameters[iIdx];
		}
		aArgs[3 + aConvertProgramParameters.length] = videoOutputFile.getAbsolutePath();
		BufferedReader bufReaderInput = null;
		try {
			/*
			 * if (logger.isLoggable(Level.FINE)) { logger.log(Level.FINE,
			 * "Converting MP4 to MOV: "); for (int i = 0; i < aArgs.length;
			 * i++) { logger.log(Level.FINE, "    {0}", aArgs[i]); } }
			 */
			Process p = Runtime.getRuntime().exec(aArgs);

			InputStream stderr = p.getErrorStream();
			bufReaderInput = new BufferedReader(new InputStreamReader(stderr));
			while ((stdErrLine = bufReaderInput.readLine()) != null) {
				if (stdErrLine.contains("not permitted") || stdErrLine.contains("atom not found")) {
					MessageDialogFactory.showMessageDialog(null,
							ResourceBundleHelper.getMessageString("video.error.conversionFailed"),
							ResourceBundleHelper.getMessageString("Error.title"), JOptionPane.ERROR_MESSAGE);
					break;
				}
			}
		} finally {
			// logger.log(Level.FINE, "Converting MP4 to MOV was completed");
			if (bufReaderInput != null) {
				bufReaderInput.close();
			}
		}
	}

	/**
	 * It checks if we already have exVideo.Mov, returns the file if present.
	 * 
	 * @param traceDirectory
	 *            directory of pcap
	 * @return File exvideo.Mov
	 */
	private static File getExVideoMovIfPresent(File traceDirectory) {
		File exVideoFileMatch[] = traceDirectory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().equals("exvideo.mov");
			}
		});
		if (exVideoFileMatch != null && exVideoFileMatch.length > 0) {
			return exVideoFileMatch[0];
		} else {
			return null;
		}
	}
	/**
	 * the method is for check video orientation so we can display video recording with landscpae mode
	 * @param traceResult
	 * @return
	 */
	public static boolean isVideoLandscape(AbstractTraceResult traceResult) {

		String videoOrientation = "";
		if(traceResult == null){
			return false;
		}else{
	    	TraceResultType resultType = traceResult.getTraceResultType();
			if (resultType.equals(TraceResultType.TRACE_FILE)) {
				return false;
	 		} else {
				TraceDirectoryResult traceresult = (TraceDirectoryResult)traceResult;			
				videoOrientation = traceresult.getCollectOptions().getOrientation();			 
			}
			return TraceDataConst.UserEvent.KEY_LANDSCAPE.equalsIgnoreCase(videoOrientation);

		}
		
 	}
	
	public static boolean mp4VideoExists(String traceDirectory) {
		if (traceDirectory == null) {
			return false;
		}		
		String videoMp4FilePath = traceDirectory 
								+ System.getProperty("file.separator") 
								+ ResourceBundleHelper.getMessageString("video.videoFileOnDevice");  
		if (new File(videoMp4FilePath).exists()) {
			return true;
		}		
		return false;
	}

	public static boolean movVideoExists(String traceDirectory) {
		if (traceDirectory == null) {
			return false;
		}		
		String movPath = traceDirectory + System.getProperty("file.separator")
				+ ResourceBundleHelper.getMessageString("video.videoDisplayFile");
		if (new File(movPath).exists()) {
			return true;
		}
		return false;
	}

}
