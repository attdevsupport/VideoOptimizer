
# Video Optimizer

All works distributed in this package are covered by the Apache 2.0 License unless otherwise stated.

> Copyright 2017 AT&T Intellectual Property

> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at

> http://www.apache.org/licenses/LICENSE-2.0

> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.

AT&T Video Optimizer contains the following open source libraries or binaries within its distribution package.  For more information on any item listed below, please contact developer.program@att.com.


**JFreeChart**  
> The AT&T Video Optimizer uses Open Source Software that is licensed under the GNU Lesser General Public License (LGPL) version 3 (the "License"), and you may not use this file except in compliance with the License. You may obtain a copy of the Licenses at: http://www.jfree.org/jfreechart/. Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licenses for the specific language governing permissions and limitations under the Licenses.  

**JCommon**  
> The AT&T Video Optimizer uses Open Source Software that is licensed under the GNU Lesser General Public License (LGPL) version 2.1 (the "License"), and you may not use this file except in compliance with the License. You may obtain a copy of the Licenses at: http://www.jfree.org/jcommon/. Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licenses for the specific language governing permissions and limitations under the Licenses.  

**FFmpeg**  
> The AT&T Video Optimizer uses Open Source Software that is licensed under the GNU Lesser General Public License (LGPL) version 2.1 (the "License"), and you may not use this file except in compliance with the License. You may obtain a copy of the Licenses at: http://ffmpeg.org/download.html. Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licenses for the specific language governing permissions and limitations under the Licenses.  

**TCPDUMP/LIBPCAP**  
> The AT&T Video Optimizer uses Open Source Software that is licensed under the following BSD (the "License"), and you may not use this file except in compliance with the License. You may obtain a copy of the Licenses at: http://www.tcpdump.org/#contribute. Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licenses for the specific language governing permissions and limitations under the Licenses. License: BSD Redistribution and use in source and binary forms, with or withoutmodification, are permitted provided that the following conditionsare met: 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/o> rials provided with the distribution. 3. The names of the authors may not be used to endorse or promote products derived from this software without specific prior written permission.  THIS SOFTWARE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS ORIMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIEDWARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  

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
#### Video Optimizer 1.3

**System Requirements for Video Optimizer 1.3:**

Before you download Video Optimizer 1.3, make sure you meet the following system requirements for your operating system.

1. 2GB or more for RAM
2. Java 8 or above (should have the latest update of Java 1.8 or above)
3. Android SDK Level 19 or above for collecting Android devices trace
4. Latest Xcode for collecting iOS devices trace
5. WinPcap and WireShark for windows user
6. FFMpeg library and VLC media player installed and configured for HD video

#### Video Optimizer 1.2 Features

#### • New video tab
A new table called ‘Video Results Summary’ is available under the video tab and it includes all the chosen video related information such as Stalls, Start-up delay, Buffer occupancy, Segments info, Bytes buffer/Seconds buffer, IP sessions/addresses, Maximum concurrent sessions, TCP connections, Redundancy etc. It also allows to manage and analyze multiple manifest files in the same trace. ‘Start-up delay reminder’ option is now available whenever we have video in the traffic for analysis. ‘Maximum Concurrent sessions’ is another option available in video tab that will display the maximum number of concurrent video sessions.

#### • UI Window for User Configurable Best Practices
This is a new UI added to the tool to help users choose the best practices they want to test against depending on the application type. This option is available under File/preferences and once the selection is made, analysis will apply to the selected best practices. ADB path can be found under preferences instead of File menu. Additionally, we limit the video segments to be displayed in the diagnostics graph only when any of the Video best practices or any of the Image best practices such as Compression, Metadata or Format is selected for analysis.

#### • New Best Practice: Multiple simultaneous connections to one IP range
This is a new best practice added under “Connections” best practices group that reports unnecessary connections. It checks if the multiple connections can be consolidated into one single connection – which would speed up content delivery and leave connection capacity for additional content. Ensuring that your mobile application’s load can be handled by your back-end server is an important test to follow before you launch your service.

#### • Video Parser
Video parser has the ability to parse any streaming video URL to identify key streaming features. Using this tool VO can perform video stream analysis and will allow the user to test any non-encrypted stream of video. Streaming apps often change their profile which causes the developers to stop analyzing the application. Different stream types like video on demand vs. live stream often have a different type of profile and characteristics. This makes individual custom coding untenable for long term sustainability of the tool.

#### •	Video Best Practices
This release has enhancements to a group of best practice tests that offer guidance on mobile development issues with video. A new tab “Video” is available on the analyzer where all the manifest and the video segments are listed on this tab. A new pop-up window is displayed where all the video segments are listed when the user sets the startup delay. The stalls are automatically detected after the startup delay is set.

#### •	Image Best Practices
This release has enhancements to the best practice tests in the area of image format. Video Optimizer converts all the images to WebP format (for Android) or Jpeg2000 format (for iOS) and it gives a “Pass” if there are no savings after the conversion. This best practice will give a “Pass” when there are no images with a savings of more than 15 percent, and a Fail if there is at least one image with more than 15 percent savings when converted.

#### •  DNS packets capture
Video Optimizer will capture all the DNS packets


### What are the known issues in Release 1.2?

#### •	Video Parser Utility
+	IP addresses/IP Sessions fields in the Video tab will display all the sessions and addresses related to the entire traffic, not particular for the video traffic. Additionally, it doesn’t update when manifest selection changes.
+	In the Video Parser Wizard window, the result section of the wizard does not save the previously chosen XREF options for the data capture group when the next set of capture group is entered.
+	When a user sees a negative segment number, it means that there is no manifest in HLS and no recognized segment number is present in the request.
+	Analyzer is getting stuck while parsing the requests available in the Video requests under the Video tab.

#### •	iOS Issues
+	Video Analysis is not available for iOS devices. For this release video analysis is only available for Android.
+	Screen capture is failing while collecting a trace via Command line interface for iOS devices.
+	A black screen is displayed on the video viewer as soon as the trace is opened instead of an image from the device on which the trace was collected.

#### •	Android Device Data Capture – CPU Temperature
+	CPU temperature is device dependent.

#### •	Video Best Practices
+	In line videos are sometimes captured as part of trace analysis and a single segment is displayed on the diagnostics chart. The user is able to select the segment.
+	In the Video Results Summary table under Video tab, the values will remain, even if the manifest file(s) is deselected after setting the startup delay. Workaround is for the user to set the start-up delay/reselect the startup delay of a movie they would like to analyze.
+ The fields ‘Network Comparison’, ‘ IP sessions’, ‘IP Address’ and ‘Mbytes Total’ under the video results summary table are displaying results for all videos regardless of video selection.
+ For Non-DRM traces, video frame thumbnails are displayed only on a Mac platform. For Windows/Linux we show blobs.

#### •  DNS Packets
Analyzer displays DNS packets as UDP.

#### •	Miscellaneous issues
+	Unresponsive UI due to specific traces causing exceptions. Workaround is to restart the application.
+	Diagnostics tab is sorting the column ‘Remote IP End Point’ alphabetically instead of numerically.
