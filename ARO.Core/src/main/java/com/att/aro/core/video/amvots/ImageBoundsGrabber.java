
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

package com.att.aro.core.video.amvots;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;
import org.sikuli.script.*;

import org.sikuli.basics.Debug;
import com.att.aro.core.ILogger;
import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class ImageBoundsGrabber {
	
	private IFileManager filemanager = SpringContextUtil.getInstance().getContext().getBean(IFileManager.class);
	private IExternalProcessRunner extrunner = SpringContextUtil.getInstance().getContext().getBean(IExternalProcessRunner.class);

	private final static String SEARCH_PART_LANDSCAPE = "searchPart.png";
	private final static String SEARCH_PART_PORTRAIT = "searchPartRotatedLeft.png";
	private final static float THRESHOLD = 0.8f;
	private String searchPart=null;
	private String orientation = "landscape";
	private boolean loaded = false;
	
	@InjectLogger
	private static ILogger log;

	private String videoPath;
	private String tracePath;
	private String thumbnailVideoPath;
	private BufferedImage thumbnailImg;
	private AROTraceData tracedata;
	private AROManifest manifest;
	private String matchResultBounds;
	
	public enum SikulixLib{
		
		libVisionProxy("libVisionProxy.so");
		
		String libName;
		SikulixLib(String libName){
			this.libName = libName;
		}
		
		public String getLibName(){
			return libName;
		}
		
	}
	
	public ImageBoundsGrabber(){
		if(Util.isLinuxOS() && (!loaded)){
			loadNativeLibraries();
			loaded = true;
		}
	}
	
	public void loadNativeLibraries(){
		for(SikulixLib lib : SikulixLib.values()){
			String libFileName = lib.toString();
			String targetLibFolder = Util.makeLibFilesFromJar(libFileName);
			Util.loadLibrary(libFileName, targetLibFolder);	
		}

	}
	
	public ImageBoundsGrabber(AROTraceData tracedata, AROManifest manifest){
		this.tracedata = tracedata;
		this.manifest = manifest;
		matchResultBounds = "";
		findImageBound();
	}
	
	public String checkMovFile(String[] video){
		String resultVideoFile="";
		video = filemanager.findFilesByExtention(tracePath, ".mov");
		if(video.length == 1){
			String cmd = Util.getFFPROBE() + tracePath + video[0];
			String result = extrunner.executeCmd(cmd);
			if(result != null){
				java.util.regex.Pattern patt = java.util.regex.Pattern.compile("(\\d+) fps");
				Matcher match = patt.matcher(result);
				if(match.find()){
					int fps = Integer.valueOf(match.group());
					if(fps > 20 && fps <= 30){
						resultVideoFile = video[0];
					}
				}
			}
		}
		
		return resultVideoFile;
	}
	public String findImageBound(){
		if(tracedata == null || manifest == null || tracedata.getAnalyzerResult().getTraceresult().getTraceDirectory() == null){
			log.error("Empty Tracedata or Manifest file.");
			return "";
		}
		
		tracePath = tracedata.getAnalyzerResult().getTraceresult().getTraceDirectory() + Util.FILE_SEPARATOR;

		String[] video = filemanager.findFilesByExtention(tracePath, ".mp4");

		if (video.length == 1) {
			videoPath = video[0];
		} else if (checkMovFile(video) != "") {
			videoPath = checkMovFile(video);
		} else {
			log.error("Unable to find .mp4 video file or more than one .mp4 video file exist.");
			return "";
		}
		
		int len = manifest.getVideoEventsBySegment().size();
		VideoEvent videoEvent = (VideoEvent) manifest.getVideoEventsBySegment().toArray()[len/2];
		
		double timestamp = videoEvent.getDLLastTimestamp();
		ffmpegFrameGrab(timestamp);
		orientation = getVideoOrientation();
		
		if(orientation != null && (orientation.equals("LANDSCAPE") || orientation.equals("landscape"))){
			searchPart = SEARCH_PART_LANDSCAPE;
		}else{//PORTRAIT
			searchPart = SEARCH_PART_PORTRAIT;
		}
		
		String result="";
		try {
			thumbnailImg = ImageIO.read(new File(thumbnailVideoPath));
			BufferedImage smallerImage = ImageIO.read(new File(getClass().getClassLoader().getResource(searchPart).getFile()));
			result = matchResult(thumbnailImg, smallerImage);

		} catch (IOException e) {
			log.error("ImageFileNotFound="+e.getMessage());
		}
        
		filemanager.deleteFile(thumbnailVideoPath);
		return result;
	}
	
	public String matchResult(BufferedImage thumbnailImg, BufferedImage smallerImage){
		String result="";
		Match match = matchImage(thumbnailImg, smallerImage);

		if (match != null && match.getScore() >= THRESHOLD) {
			result = getBounds(match);
		} else {
			match = resize(smallerImage);
			result = processMatch(match);
		}
		return result;
	}
	
	public String processMatch(Match match){
		if(match != null && match.getScore() >= THRESHOLD ){
        	return getBounds(match);
        }else{
			return "";
		}
	}
	
	public void ffmpegFrameGrab(double timestamp){
	    thumbnailVideoPath = tracePath + "thumbnailVideo.png";

		videoPath = tracePath + videoPath;
		String cmd = Util.getFFMPEG() + " -y -i " + "\"" + videoPath + "\"" + " -ss "+ timestamp+"   -vframes 1 " + "\""
				+ thumbnailVideoPath + "\"";
		extrunner.executeCmd(cmd);
	}
	
	public String getVideoOrientation() {
		String collectionPath = tracePath + "collect_options";
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
					BufferedReader inReader = new BufferedReader(
							new InputStreamReader(filemanager.getFileInputStream(tracePath + "screen_rotations")));
					line = inReader.readLine();
					while (line != null) {
						orientation = line.split(" ")[1];
						line = inReader.readLine();
					}
				}

			} catch (IOException e) {
				log.error("IOException="+e.getMessage());
			}

		} catch (FileNotFoundException e) {
			log.error("FileNotFound="+e.getMessage());
		}
		return orientation;
	}

	public String getBounds(Match match){
		if(orientation.equalsIgnoreCase("LANDSCAPE")){
			match.setH(match.getH()*2);
		}else{
			match.setX(match.getX() - match.getW());
			match.setW(match.getW()*2);
		}
		matchResultBounds = match.getX() +","+ match.getY() +","+  match.getW() + ","+ match.getH();
		return matchResultBounds;
	}
	
	public String getBounds(){
		return matchResultBounds;
	}
	
	public Match resize(BufferedImage smallerImage){
		BufferedImage searchPart;
        
		int round = 1;
		float rate = 0.1f;
		float value =0;
		double score = 0;
		double prevScore =0;
		final float COUNT = 0.1f;
		Match match,maxMatch = null;
		int tempRound = 0;
		boolean firstTime = true;
		
		while((thumbnailImg.getHeight() > smallerImage.getHeight()) && score < THRESHOLD){

			if (rate * 1000 > 1) {
				Image image2 = new Image(smallerImage);
				float resizefactor = value + (rate * round);

				searchPart = image2.resize(resizefactor);
				match = matchImage(thumbnailImg, searchPart);

				if (match == null) {
					score = 0;
				} else {
					score = match.getScore();
					if (score > prevScore) {
						prevScore = score;
						maxMatch = match;
					} else {
						value = value + (rate * (round - 1));
						rate = rate * COUNT;
						if(firstTime){
							tempRound = round;
							firstTime = false;
						}
						round = 0;
					}

				}
				round++;
			}else{
				rate = 0.1f;
				value = 0;
				prevScore = 0;
				round = tempRound;
				firstTime = true;
			}
		}
		return maxMatch;
		
	}
	
	public Match matchImage(BufferedImage thumbnailImg, BufferedImage smallerImage){
		
		Debug.setDebugLevel(3);
		Match match = null;
		
		Finder finder;
		Image image1 = new Image(thumbnailImg);
		image1.setSimilarity(0.3f);
        finder = new Finder(image1); 

		Pattern patt = new Pattern(smallerImage);
        patt.similar(0.3f);
        finder.find(patt);

        if (finder.hasNext()) {
        	match = finder.next();
        }  
        return match;
	}
	
	public void setThumbnailVideoPath(String thumbnailVideoPath){
		this.thumbnailVideoPath = thumbnailVideoPath;
	}
	
	public void setThumbnailImg(BufferedImage thumbnailImg){
		this.thumbnailImg = thumbnailImg;
	}
	public void setTracePath(String tracePath){
		this.tracePath = tracePath;
	}

	public void setTracedata(AROTraceData tracedata) {
		this.tracedata = tracedata;
	}

	public void setManifest(AROManifest manifest) {
		this.manifest = manifest;
	}
	
	public AROManifest getManifest() {
		return this.manifest;
	}
	public void setOrientation(String orientation){
		this.orientation = orientation;
	}

}

