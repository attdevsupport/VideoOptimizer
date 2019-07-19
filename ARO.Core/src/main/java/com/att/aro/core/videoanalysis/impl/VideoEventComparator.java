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
package com.att.aro.core.videoanalysis.impl;

import java.util.Comparator;

import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class VideoEventComparator implements Comparator<VideoEvent> {

	private SortSelection choice;
	public VideoEventComparator(SortSelection choice) {
		this.choice = choice;
	}

	@Override
	public int compare(VideoEvent ve1, VideoEvent ve2) {
		if(choice == SortSelection.END_TS){
			double endTime1 = ve1.getEndTS();
			double endTime2 = ve2.getEndTS();
			if (endTime1 < endTime2){
				return -1;
			} else if (endTime1 > endTime2){
				return 1;
			}
		} else if(choice == SortSelection.START_TS){
			double startTime1 = ve1.getStartTS();
			double startTime2 = ve2.getStartTS();
			if (startTime1 < startTime2){
				return -1;
			} else if (startTime1 > startTime2){
				return 1;
			}
		} else if(choice == SortSelection.SEGMENT){
			double seg1 = ve1.getSegmentID();
			double seg2 = ve2.getSegmentID();
			if (seg1 < seg2){
				return -1;
			} else if (seg1 > seg2){
				return 1;
			}			
		}else if(choice == SortSelection.END_TS_DESCENDING){
			double endTime2 = ve2.getEndTS();
			double endTime1 = ve1.getEndTS();
			if (endTime2 < endTime1){
				return -1;
			} else if (endTime2 > endTime1){
				return 1;
			}			
		}
		return 0;
	}

}
