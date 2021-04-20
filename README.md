
# Video Optimizer

All works distributed in this package are covered by the Apache 2.0 License unless otherwise stated.

> Copyright 2020
Intellectual Property

> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at

> http://www.apache.org/licenses/LICENSE-2.0

> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.

Video Optimizer contains the following open source libraries or binaries within its distribution package.  For more information on any item listed below, please contact developer.program@att.com.


**JFreeChart**  
> Video Optimizer uses Open Source Software that is licensed under the GNU Lesser General Public License (LGPL) version 3 (the "License"), and you may not use this file except in compliance with the License. You may obtain a copy of the Licenses at: http://www.jfree.org/jfreechart/. Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licenses for the specific language governing permissions and limitations under the Licenses.  

**JCommon**  
> Video Optimizer uses Open Source Software that is licensed under the GNU Lesser General Public License (LGPL) version 2.1 (the "License"), and you may not use this file except in compliance with the License. You may obtain a copy of the Licenses at: http://www.jfree.org/jcommon/. Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licenses for the specific language governing permissions and limitations under the Licenses.  

**FFmpeg**  
> Video Optimizer uses Open Source Software that is licensed under the GNU Lesser General Public License (LGPL) version 2.1 (the "License"), and you may not use this file except in compliance with the License. You may obtain a copy of the Licenses at: http://ffmpeg.org/download.html. Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licenses for the specific language governing permissions and limitations under the Licenses.  

**TCPDUMP/LIBPCAP**  
> Video Optimizer uses Open Source Software that is licensed under the following BSD (the "License"), and you may not use this file except in compliance with the License. You may obtain a copy of the Licenses at: http://www.tcpdump.org/#contribute. Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licenses for the specific language governing permissions and limitations under the Licenses. License: BSD Redistribution and use in source and binary forms, with or withoutmodification, are permitted provided that the following conditionsare met: 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/o> rials provided with the distribution. 3. The names of the authors may not be used to endorse or promote products derived from this software without specific prior written permission.  THIS SOFTWARE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS ORIMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIEDWARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  


## Open Source Code Package

Video Optimizer allows mobile application developers to test the network performance of video, images and other files for compression, speed, battery drain and other aspects of performance.

The Data Collector is the component of the Video Optimizer application that captures the data traffic of a mobile device and stores that information in trace files that can be analyzed using the Video Optimizer Data Analyzer.

The Video Optimizer Data Analyzer is the component of the Video Optimizer application that evaluates the trace data collected from an application and generates statistical and analytical results based on recommended best practices.

This Open Source Code Package contains all of the code needed to build both of these Video Optimizer components.


**Video Optimizer Web Resources:**  
Video Optimizer: https://developer.att.com/video-optimizer<br/>
Video Optimizer Support: https://developer.att.com/video-optimizer/support<br/>
Contact Us: http://developer.att.com/developer/contact_us.jsp<br/>


**Version:**  
#### Video Optimizer 4.1

**System Requirements for Video Optimizer 4.1:**

*Before you download Video Optimizer 4.1, make sure you meet the following system requirements for your operating system.*

- At least 4GB of RAM, but recommend at least 8GB
- Java 8 or above
- For Android developers, Android SDK Level 23 or above
- FFmpeg & FFprobe
- Windows requirements
  - Wireshark (Install Npcap addon with Winpcap API-compatible mode)
  - VLC media player 3.x.x or higher
- Mac requirements
  - Brew (or any similar package manager)
  - Ifuse
  - OSXFuse
  - Libimobiledevice & ideviceinstaller
  - For iOS developers, use latest version of XCode and OS X
  - VLC media player 3.x.x or higher
  - Wireshark
- Linux requirements
  - For Ubuntu 18.04.x LTS (and newer) users, use VLC media player version 3.x.x
  - For Ubuntu 16.04.x LTS (and older) users, use VLC media player version 3.x.x up to 3.0.8
  - Wireshark



**Video Optimizer 4.1 Features**
- This release brings to the Linux version all the following features and enhancements that were brought to the Mac and PC version in release 4.1:

- Analysis of IPV6 data is now supported, increasing the number of apps that allow full video stream analysis.

- Capture of IPV6 data by the VPN collector is now supported, allowing analysis of IPV6 data from Android devices.

- New Segment Progress graph in the Video tab displays the download time progress and the play time progress of the segments.
  - Video segments, Audio segments, Video PlayTime, and Audio PlayTime are displayed for demuxed video.
  -	Download_Progress and PlayTime_Progress are displayed for muxed video.
  - Hovering over the points displays the following information - Segment #, Track #, Download Start Time, Download End Time, Play Start Time, Play End Time, Duration, Progress, and the Content (Video/Audio).
  -	Graph refreshes when each stream is selected.

- New Segment Buffer graph displays information about the playback buffer.          
  - Hovering over the points in the graph displays the following information – Segment #, PlayBack Buffer in seconds, and the TimeStamp.
  - The graph refreshes when each selected stream has its startup delay set.
- Redesigned Startup Delay window
  -	In the User Touch Event section, navigate through the touch events viewing the simultaneously changing frame images on the right-hand side of the dialog to choose the time stamp of the precise video playback requested time.
  -	In the Video Startup Time section, navigate through the startup times viewing the simultaneously changing frame images on the right-hand side of the dialog to set the video startup time to any value greater than the Play requested time
  -	Video Startup Time is now saved:  Once the startup delay has been set for a stream/streams, the values for the play requested time and the startup delay are saved in a json file in the trace folder.  When the trace is opened again, these previously set values are displayed when the Startup Delay window opens for the stream.

- CSI (Chunk Sequence Inferencer) - the CSI feature can be used for analysis if the default video analysis does not provide any results and a manifest is available.

- User Interface Enhancements:
  -	A clearer message ‘Video segments could not be assigned to a Video Stream’, when there are video requests, but no Streams showing in video tab.
  -	Enhanced ffmpeg and ffprobe detection.
  -	Enhanced Content View tab which displays all the requests and responses in the same order as in Request/Response View.
  - Enhanced handling of 0 value for throttling and stopping data flow during trace collection.


**Known issues in Release 4.1**

- Startup Delay window does not display the Set/Cancel button if the vertical resolution of the display is below 1080.  Work around is to capture landscape orientation traces for video.
-	Trace collection cannot be started when the device is on the lock screen, otherwise the VPN collector app will freeze.

**Compilation instructions**
+ Please follow the order to compile projects
+ ../ARO.Parent
+ ../ARO.Core
+ ../ARO.Analytics
+ ../DataCollectors/ARO.IOSCollector
+ ../DataCollectors/ARO.NorootAndroidCollector
+ ../DataCollectors/ARO.RootedAndroidCollector
+ ../ARO.UI
+ ../ARO.Console
