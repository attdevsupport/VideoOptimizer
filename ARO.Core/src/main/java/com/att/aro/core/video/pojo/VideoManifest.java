package com.att.aro.core.video.pojo;

import lombok.Data;

import java.util.List;

import com.att.aro.core.videoanalysis.pojo.MediaType;

@Data
public class VideoManifest {
	
	private List<Track> tracks;
	private Track audioTrack;
	private double medianAudioTrackSize;
	private int maxVideoSegmentIndexSize;
	
	public void setTracks(List<Track> tracks) {
		
		int trackNumber = 0;
		this.tracks = tracks;
		for (Track track : this.tracks) {
			
			maxVideoSegmentIndexSize = (track.getMaxSegmentIndex() > maxVideoSegmentIndexSize)? track.getMaxSegmentIndex() : maxVideoSegmentIndexSize;
			
			if (track.getMediaType() == MediaType.VIDEO) {
				track.setTrackNumber(trackNumber++);
			}
			
			if (track.getMediaType() == MediaType.AUDIO && medianAudioTrackSize == 0) {
				audioTrack = track;
				List<Integer> segmentSizes = track.getSegmentSizes();
				int listSize = segmentSizes.size();
				medianAudioTrackSize = segmentSizes.stream().mapToInt(s -> s).sorted().skip((listSize-1)/2).limit(2-listSize%2).average().orElse(0.0);
			}
		}
	}
}
