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
package com.att.aro.ui.view.video;

import javax.swing.SwingUtilities;
import com.att.aro.core.ILogger;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.view.diagnostictab.DiagnosticsTab;

/**
 * Synchronizes the video and the hairline tracker
 * 
 *
 *
 */
public class VideoSyncThread implements Runnable {

	// When invoked this runnable sets the Aro Advanced Tab's hairline tracker
	// to the same value as video player time
	private final class TrackerPlacementRunnable implements Runnable {
		@Override
		public void run() {
			double trackerPosition = videoSecondsElapsed + videoOffset;
			if (!aroAdvancedTab.IsGraphPanelClicked()) {
				if (trackerPosition < 0) {
					trackerPosition = 0.0;
				}
				aroAdvancedTab.setTimeLineLinkedComponents(trackerPosition, true);
			} else {
				aroAdvancedTab.setTimeLineLinkedComponents(trackerPosition, true);
			}
		}
	}
	//End of inner class

	private ILogger logger = ContextAware.getAROConfigContext().getBean(ILogger.class);
	private double videoSecondsElapsed;
	private IVideoPlayer videoPlayer;
	private DiagnosticsTab aroAdvancedTab;
	private double videoOffset;

	public VideoSyncThread(IVideoPlayer videoPlayer, DiagnosticsTab aroAdvancedTab, double videoOffset) {
		this.videoPlayer = videoPlayer;
		this.aroAdvancedTab = aroAdvancedTab;
		this.videoOffset = videoOffset;
	}

	@Override
	public void run() {
		boolean isPlaying;
		double currentVideoTime;
		do {
			synchronized (this) {
				if (videoPlayer != null) {
					currentVideoTime = videoPlayer.getMediaTime();
					isPlaying = videoPlayer.isPlaying();
				} else {
					break;
				}
			}
			if (currentVideoTime != videoSecondsElapsed) {
				if (aroAdvancedTab != null) {
					videoSecondsElapsed = currentVideoTime;
					SwingUtilities.invokeLater(new TrackerPlacementRunnable());
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException exception) {
				logger.error("InterruptedException", exception);
			}
		} while (isPlaying);
	}
}
