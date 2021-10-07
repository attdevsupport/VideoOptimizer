
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

package com.att.aro.core.videouploadanalysis;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.sikuli.script.Image;
import org.sikuli.script.ImageFinder;
import org.sikuli.script.Match;
import org.sikuli.script.Pattern;

import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.resourceextractor.IReadWriteFileExtractor;
import com.att.aro.core.util.IResultSubscriber;
import com.att.aro.core.util.IStringParse;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;

public class ImageBoundsGrabber implements Runnable {

	public enum Status {
		FAILED
	}

	private IFileManager filemanager = SpringContextUtil.getInstance().getContext().getBean(IFileManager.class);
	private IExternalProcessRunner extrunner = SpringContextUtil.getInstance().getContext().getBean(IExternalProcessRunner.class);
	private IReadWriteFileExtractor extractor = SpringContextUtil.getInstance().getContext().getBean(IReadWriteFileExtractor.class);
	private IStringParse stringParse = SpringContextUtil.getInstance().getContext().getBean(IStringParse.class);
	private static final Logger LOG = LogManager.getLogger(ImageBoundsGrabber.class.getName());	
	private static final String SEARCH_PART_LANDSCAPE = "searchPartLandScape.png";
	private static final String SEARCH_PART_PORTRAIT = "searchPartPortrait.png";
	protected static final String IMAGE_NAME = "frameImage.png";
	private static final String VIDEO_NAME = "video.mp4";
	
	private float threshold = 0.8f;
	private String orientation = null;

	private IResultSubscriber subscriber;

	private String videoPath;
	private String imagePath;
	private String tracefolder;
	private VideoStream videoStream;
	private String matchResultBounds;
	private boolean runState;
	
	private BufferedImage targetBufImg;
	private String searchImagePath;

	private ImageFinder finder = null;
	private Image targetImage;
	
	private double baseSearchH;
	private double baseSearchW;

	@Override
	public void run() {
		runState = true;
		LOG.info("running");
		subscriber.receiveResults(this.getClass(), null, "Scanning...");
		try {
			String results = findImageBounds();
			if (isRunning()) {
				if (results != null && !results.isEmpty()) {
					LOG.info(String.format("forward results \"%s\"to :%s", results, subscriber.getClass().getSimpleName()));
					subscriber.receiveResults(this.getClass(), true, results);
				} else {
					LOG.info(String.format("forward results FAILED to :%s", subscriber.getClass().getSimpleName()));
					subscriber.receiveResults(this.getClass(), false, "Unable to locate video bounds");
				}
			} else {
				LOG.info("stopped");
			}
		} catch (Exception e) {
			LOG.error("Failed to match video :", e);
			LOG.info(String.format("forward results Exception:%s to :%s", e.getMessage(), subscriber.getClass().getSimpleName()));
			if (isRunning() && subscriber != null) {
				subscriber.receiveResults(this.getClass(), false, "Unable to locate video frame");
			}
		}
		runState = false;
	}

	public boolean isRunning() {
		return runState;
	}
	
	public void setStop() {
		LOG.info("stopping");
		runState = false;
	}

	public ImageBoundsGrabber() {
		loadNativeLibraries();
	}

	public void loadNativeLibraries() {
		String[] libs = null;
		if (Util.isMacOS()) {
			
			libs = new String[] { "libjpeg.8.dylib", "liblept.3.dylib", "libMacHotkeyManager.dylib", "libMacUtil.dylib", "libopencv_java248.dylib", "libpng15.15.dylib",
					"libtesseract.3.dylib", "libtiff.5.dylib", "libVisionProxy.dylib" };
//			return; // no special handling required
		} else if (Util.isWindows64OS()) {
			libs = new String[] { "JIntellitype.dll", "libwinpthread-1.dll", "libz-1.dll", "libgcc_s_seh-1.dll", "libpng14-14.dll", "libstdc++-6.dll", "WinUtil.dll",
					"libopencv_core248.dll", "liblept-3.dll", "libopencv_flann248.dll", "libopencv_highgui248.dll", "libopencv_imgproc248.dll", "libtesseract-3.dll",
					"VisionProxy.dll", "libopencv_features2d248.dll", "libopencv_java248.dll" };

		} else if (Util.isWindows32OS()) {
			libs = new String[] {"no worries mate"};
			
		} else if (Util.isLinuxOS()) {
			libs = new String[] { "libVisionProxy.so" };
		}
		try {
			String destination = extractSikuliLibs();
			if (!destination.isEmpty()) {
				loadSikuliLibs(destination, libs);
			}
		} catch (Exception e) {
			// FIXME - handle this failure
			e.printStackTrace();
		}
	}

	private String extractSikuliLibs() throws Exception {

		String sikJar = "";
		String sPattern = "";
		String suffix = "";
		String pathExten = "";
		String sep = ":";

		if (Util.isMacOS()) {
			// no special handling required
			sikJar = "sikulixlibsmac-1.1.0.jar";
			sPattern = "([a-zA-Z0-9\\/]*VideoOptimizer)[\\.:][a-zA-Z\\/]*:";
			suffix = ".dylib";
			pathExten = ".app/Contents/java/app/";
		} else if (Util.isWindows64OS() || Util.isWindows32OS() || Util.isWindowsOS()) {
			sikJar = "sikulixlibswin-1.1.0.jar";
			suffix = ".dll";
			sPattern = ";([a-zA-Z0-9\\\\: ]*VideoOptimizer);";
			pathExten = Util.FILE_SEPARATOR;
			sep = ";";

		} else if (Util.isLinuxOS()) {
			sikJar = "sikulixlibslux-1.1.0.jar";
			sPattern = ":([a-zA-Z0-9\\/]*VideoOptimizer):";
			suffix = ".so";
			pathExten = Util.FILE_SEPARATOR;
		}

		String destination = Util.getExtractedDrivers();
		filemanager.mkDir(destination);
		if (filemanager.directoryExist(destination) && filemanager.directoryExistAndNotEmpty(destination)) {
			return destination;
		}
		
		String javaPath = System.getProperty("java.library.path") + sep;
		LOG.info((">>>>> java.library.path = " + javaPath).replaceAll("([" + sep + "=])", "\n\t$1"));
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(sPattern);
		String[] found = stringParse.parse(javaPath, pattern);
		String path;
		if (found == null) {
			return "";
		}
		path = found[0];
		
		sikJar = path + pathExten + sikJar;
		JarFile jar = new JarFile(sikJar);
		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			File efile = new File(destination, entry.getName());
			boolean libs = (!Util.isLinuxOS() || efile.getPath().contains("libs64"));
			if (!entry.isDirectory() && efile.getName().endsWith(suffix) && libs) {
				LOG.info(efile.getPath() + efile.getName());
				filemanager.saveFile(new BufferedInputStream(jar.getInputStream(entry)), destination + efile.getName());
			}
		}
		jar.close();
		return destination;
	}


	private void loadSikuliLibs(String voLpath, String[] libs) throws Exception {

		for (String lib : libs) {
			try {
				System.load(voLpath + lib);
				LOG.info("loaded :" + voLpath + lib);
			} catch (Exception e) {
				LOG.error("MISSING!" + lib);
				e.printStackTrace();
			}
		}
	}

	public void init(IResultSubscriber videoPostDialog, String tracefolder, VideoStream videoStream) throws Exception {
		this.subscriber = videoPostDialog;
		if (tracefolder == null) {
			LOG.error("Empty Tracedata or Manifest file.");
			throw new Exception("Null in arguments");
		}
		this.tracefolder = (new File(tracefolder)).toString() + Util.FILE_SEPARATOR;
		this.videoStream = videoStream;
		matchResultBounds = "";
		videoPath = locateVideo();
	}

	/**
	 * 
	 * @param orientation
	 * @param imagePath 
	 * @param searchImagePath
	 * @param targetImagePath
	 */
	public void setImages(String orientation, String videoPath, String imagePath, double timestamp, float threshold) {
		this.orientation = orientation;
		this.imagePath = imagePath;
		this.threshold = threshold;
		this.videoPath = videoPath;
		if (imagePath == null) {
			this.imagePath = Util.extractFrameToPNG(timestamp, videoPath, tracefolder+IMAGE_NAME);
		}
	}

	/**
	 * Find the bounds of an image within an image.
	 * 
	 * @return bounds in a comma separated string ex: "1,2,3,4"
	 * @throws Exception
	 */
	protected String findImageBounds() throws Exception {
		String searchPart;
		LOG.info(">>>>> java.library.path = " + System.getProperty("java.library.path"));
//		Debug.setDebugLevel(3);

		if (orientation == null) {
			videoPath = tracefolder + "" + VIDEO_NAME;
			imagePath = tracefolder + "" + IMAGE_NAME;
			imagePath = Util.extractFrameToPNG(findMidPoint(videoStream), videoPath, imagePath);
			orientation = getVideoOrientation();
		}

		if ("landscape".equalsIgnoreCase(orientation)) {
			searchPart = SEARCH_PART_LANDSCAPE;
		} else { // PORTRAIT
			searchPart = SEARCH_PART_PORTRAIT;
		}

		searchImagePath = tracefolder + searchPart;
		if (!extractor.extractFiles(searchImagePath, searchPart, this.getClass().getClassLoader())) {
			LOG.error("Failed to extract search image");
			throw new Exception("Failed to extract search image:" + searchPart);
		}

		subscriber.receiveResults(this.getClass(), null, "Scanning..., Pulled sample frame, Orientation:"+orientation);
		
		LOG.info(String.format("===%s Search <<", searchImagePath));
		LOG.info(String.format("===%s Target <<", imagePath));

		BufferedImage searchBufImage = null;
		try {
			targetBufImg = ImageIO.read(new File(imagePath));
			LOG.info(String.format("===%s Target dim %d x %d\n", (new File(imagePath)).getName(), targetBufImg.getWidth(), targetBufImg.getHeight()));

			searchBufImage = ImageIO.read(new File(searchImagePath));
			LOG.info(String.format("===%s Search dim %d x %d", (new File(searchImagePath)).getName(), searchBufImage.getWidth(), searchBufImage.getHeight()));

		} catch (IOException e) {
			LOG.error("ImageFileNotFound=", e);
		}

		if (targetBufImg != null && searchBufImage != null) {
			finder = null;
			matchResultBounds = matchResult(targetBufImg, searchBufImage);
		}
		// FIXME remove debugging code
		// filemanager.deleteFile(imagePath);
		// filemanager.deleteFile(searchImagePath);
		return matchResultBounds;
	}

	/**
	 * Find a timestamp that might be within range of actual play.
	 * 
	 * @return timestamp in seconds
	 * @throws Exception
	 */
	private double findMidPoint(VideoStream videoStream) throws Exception {
		locateVideo();

		double timestamp;
		if (videoStream != null) {
			int len = videoStream.getVideoEventsBySegment().size();
			VideoEvent videoEvent = (VideoEvent) videoStream.getVideoEventsBySegment().toArray()[len / 2];

			timestamp = videoEvent.getDLLastTimestamp();
			LOG.info("timestamp :" + timestamp);
		} else {
			String[] times = filemanager.readAllLine(filemanager.createFile(tracefolder, "time").getAbsolutePath());
			if (times.length >= 3) {
				double t1 = Double.valueOf(times[1]);
				double t2 = Double.valueOf(times[3]);
				timestamp = (t2 - t1) *.5;
			} else {
				throw new Exception("Missing or invalid time file");
			}
		}
		return timestamp;
	}

	private String locateVideo() {
		File temp = filemanager.createFile(tracefolder, "video.mp4");
		if (temp.exists()) {
			return temp.getName();
		} else {
			String tmp = checkMovFile("video.mov");
			if (!tmp.isEmpty()) {
				return tmp;
			}
		}

		LOG.error("Unable to find video.mp4 or video.mov video file.");
		return null;
	}

	/**
	 * <pre>
	 * Locate video.mov in tracefolder. Determine if the mov is a full motion video and not a "slideshow"
	 * 
	 * @param videoMov
	 * @return full path if video exists and is full motion, else returns empty string
	 */
	public String checkMovFile(String videoMov) {
		File temp = filemanager.createFile(tracefolder, videoMov);
		String video;
		if (temp.exists()) {
			video = temp.getAbsolutePath();

			String cmd = Util.getFFPROBE() + Util.wrapText(video);
			String result = extrunner.executeCmd(cmd);
			if (result != null) {
				java.util.regex.Pattern patt = java.util.regex.Pattern.compile("(\\d+) fps");
				Matcher match = patt.matcher(result);
				if (match.find()) {
					int fps = Integer.valueOf(match.group());
					if (fps > 20 && fps <= 30) {
						return temp.getName();
					}
				}
			}
		}

		return "";
	}

	/**
	 * targetImg, searchImage
	 * 
	 * @param targetBufImg
	 *            a buffered image
	 * @param searchBufImage
	 *            a buffered image
	 * @return
	 */
	public String matchResult(BufferedImage targetBufImg, BufferedImage searchBufImage) {
		String result = "";
		Match match = matchImage(targetBufImg, searchBufImage);

		if (match != null && match.getScore() >= threshold) {
			result = getBounds(match);
		} else {
			match = resize(searchBufImage);
			result = processMatch(match);
		}
		return result;
	}

	// FIXME - remove this debugging method
	private void debugSaveImages(BufferedImage targetBufImg, BufferedImage searchBufImage) {
//		try {
//			String debugFolder = tracefolder + "debug";
//			filemanager.mkDir(debugFolder);
//			ImageIO.write(searchBufImage, "png", new File(String.format("%s/T%dx%d_searchBufImage.png", debugFolder, searchBufImage.getWidth(), searchBufImage.getHeight())));
//			ImageIO.write(targetBufImg, "png", new File(String.format("%s/T_%dx%d_targetBufImg.png", debugFolder, targetBufImg.getWidth(), targetBufImg.getHeight())));
//		} catch (Exception e) {
//			log.error("Failed to save file :", e);
//		}
	}

	public String processMatch(Match match) {
		if (match != null && match.getScore() >= threshold) {
			return getBounds(match);
		} else {
			return "";
		}
	}

	public String getVideoOrientation() {
		String collectionPath = tracefolder + "collect_options";
		String orientation = null;
		try {
			InputStream input = filemanager.getFileInputStream(collectionPath);
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			try {
				String line = reader.readLine();
				while (line != null) {
					if (line.contains("orientation")) {
						orientation = line.split("=")[1];
						break;
					}
					line = reader.readLine();
				}
				if (orientation == null) {
					targetBufImg = ImageIO.read(new File(imagePath));
					orientation = targetBufImg.getHeight() > targetBufImg.getWidth() ? "portrait" : "landscape";
				}

			} catch (IOException e) {
				LOG.error("IOException=" + e.getMessage());
			}

		} catch (FileNotFoundException e) {
			LOG.error("FileNotFound=" + e.getMessage());
		}
		return orientation;
	}

	// defined for 720x1280
	final double baseInsetH =  66; // 67;// 97;
	final double baseInsetV =  82; // 91;// 98;
	final double baseH =  673  ; //  720;
	final double baseW = 1196  ; // 1280;

	/**
	 * 
	 * @param match
	 * @return
	 */
	public String getBounds(Match match) {

		double screenV = match.getY(); // 158
		double screenH = match.getX(); // 108
		double searchH = match.getH(); // 57
		double searchW = match.getW(); // 131

		double vTop = screenV - baseInsetV * (searchH / baseSearchH);
		double vLeft = screenH - baseInsetH * (searchW / baseSearchW);

		Dimension targetDimension = targetImage.getSize();
		LOG.info(String.format("H:%.0f W:%.0f", targetDimension.getHeight(), targetDimension.getWidth()));

		double vBot = vTop + (baseH * (searchH / baseSearchH));
		double wBot = vLeft + (baseW * (searchW / baseSearchW));

		// keep within bounds
		if (vTop < 0) {
			vTop = 0;
		}
		if (vLeft < 0) {
			vLeft = 0;
		}
		if (vBot > targetDimension.getHeight()) {
			vBot = targetDimension.getHeight();
		}
		if (wBot > targetDimension.getWidth()) {
			wBot = targetDimension.getWidth();
		}

		LOG.info(String.format("screenV %.0f, screenH %.0f, searchH %.0f, searchW %.0f", screenV, screenH, searchH, searchW));
		LOG.info(String.format("topLeft (%.0f,%.0f) bottomRight (%.0f,%.0f)", vLeft, vTop, wBot, vBot));

		matchResultBounds = String.format("%.0f,%.0f,%.0f,%.0f", vLeft, vTop, wBot, vBot);
		return matchResultBounds;
	}

	public String getBounds() {
		return matchResultBounds;
	}
	
	/**
	 * Search for the best match while steppingdown the size of the searchImage from 99% to 80%
	 * @param searchBufImage
	 * @return
	 */
	public Match resize(BufferedImage searchBufImage) {
		Match match, maxMatch = null;
		double score = 0;
		double maxScore = 0;
		float step = 1000;
		BufferedImage temp;

		float factor = step;
		temp = scale(factor-- / step, searchBufImage);
		float minFactor = step * .70f;

		do {
			try {
				match = matchImage(targetBufImg, temp);
				if (match != null) {
					score = match.getScore();
					if (score > maxScore) {
						maxMatch = match;
						maxScore = score;
					}
				}
			} catch (Exception e) {
				LOG.error("Exception in matching image :" + e.getMessage());
				match = null;
			}
			LOG.info(String.format("Score:%.2f Search dim %d x %d", ((match != null) ? match.getScore() : 0d) * 100, temp.getWidth(), temp.getHeight()));
			if (score >= .99 || score / maxScore < .90) {
				break;
			}
			// temp = (new Image(searchBufImage)).resize(factor-- / step);
			temp = scale(factor-- / step, searchBufImage);
			debugSaveImages(targetBufImg, temp);


		} while (factor > minFactor && isRunning());
		LOG.info(maxMatch != null ? maxMatch.toString() : "");
		return maxMatch;
	}

	public BufferedImage scale(double scale, BufferedImage bufferedImage) {

		double h = (double) bufferedImage.getHeight() * scale;
		double w = (double) bufferedImage.getWidth() * scale;

		BufferedImage scaledBufferedImage = new BufferedImage((int) w, (int) h, bufferedImage.getType());
		AffineTransform at = new AffineTransform();
		at.scale(scale, scale);
		AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
		scaleOp.filter(bufferedImage, scaledBufferedImage);

		return scaledBufferedImage;
	}


	/**
	 * Look for searchImage inside of targetImage
	 * 
	 * @param targetBufImg
	 *            a buffered image
	 * @param searchBufImage
	 *            a buffered image
	 * @return match
	 */
	public Match matchImage(BufferedImage targetBufImg, BufferedImage searchBufImage) {
		baseSearchH = searchBufImage.getHeight() + 1D;
		baseSearchW = searchBufImage.getWidth() + 1D;

		debugSaveImages(targetBufImg, searchBufImage);
		LOG.info(String.format("Search dim %d x %d", searchBufImage.getWidth(), searchBufImage.getHeight()));
		LOG.info(String.format("Target dim %d x %d", targetBufImg.getWidth(), targetBufImg.getHeight()));

		try {
			Match match = null;
//			finder = null;
//			if (finder == null) {
				targetImage = new Image(targetBufImg);
				targetImage.setSimilarity(0.3f);
				finder = new ImageFinder();
				finder.setImage(targetImage);
//			}
			Pattern patt = new Pattern(searchBufImage);
			patt.similar(0.3f);
			try {
				finder.find(patt);
				if (finder.hasNext()) {
					match = finder.next();
				}
			} catch (Exception e) {
				match = null;
			}
			LOG.info("match = " + match);
			return match;
		} catch (Exception e) {
			LOG.error("Exception :", e);
			return null;
		}
	}

}
