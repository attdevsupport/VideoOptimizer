
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
#### Video Optimizer 1.1 

**System Requirements for Video Optimizer 1.1:**

Before you download Video Optimizer 1.1, make sure you meet the following system requirements for your operating system.

1. 2GB or more for RAM
2. Java 8 or above (should have the latest update of Java 1.8 or above)
3. Android SDK Level 19 or above for collecting Android devices trace
4. Latest Xcode for collecting iOS devices trace
5. WinPcap for windows user
6. FFMpeg library installed and configured for HD video

#### Video Optimizer 1.1 Features
Release 1.1 extends the features released in the Video Optimizer version 1.0.

#### • Video Parser Utility (Beta version)
The Video Parser utility significantly broadens the reach of video optimization by supporting different video formats. This tool has the ability to parse any streaming video URL to identify key streaming features using regular expressions. By using this tool, Video Optimizer can perform video stream analysis and allow the user to test any unencrypted stream of video. Different stream types like video on demand vs. live stream often have a different type of profile and characteristics.

#### •	Video Best Practices
This release has enhancements to a group of best practice tests that offer guidance on mobile development issues with video. A new tab “Video” is available on the analyzer where all the manifest and the video segments are listed on this tab. A new pop-up window is displayed where all the video segments are listed when the user sets the startup delay. The stalls are automatically detected after the startup delay is set.

#### •	Image Best Practices
This release has enhancements to the best practice tests in the area of image format. Video Optimizer converts all the images to WebP format (for Android) or Jpeg2000 format (for iOS) and it gives a “Pass” if there are no savings after the conversion. This best practice will give a “Pass” when there are no images with a savings of more than 15 percent, and a Fail if there is at least one image with more than 15 percent savings when converted.

#### •  Android Device Data Capture – CPU Temperature and GPS Events
This release includes Android device data capture and Diagnostic tab correlation, CPU Temperature, and location events.

##### - CPU Temperature
A new option “CPU Temperature” is available under View/Options/CPU Temperature. The thermal temperature of the device is displayed on the diagnostic chart as the CPU Temperature. A green graph is plotted on the diagnostic chart.

##### - GPS/Location events
GPS/location events information is collected from the device and displayed on the diagnostic chart. Active, Standby state is plotted on the graph. Pings are also marked on the diagnostic chart. The following information is available by hovering on hovering on the pings:
+	Time
+	Location info
+	Latitude
+	Longitude
+	Locality
+	Provider

#### •  Performance Enhancements
This release includes performance-related enhancements such as a faster launch for Video Optimizer and support for bigger traces.

#### •  Rebranding
This release includes rebranding of the ARO Command Line Utility and folder structures to the Video Optimizer.

### What are the known issues in Release 1.1?

#### •	Video Parser Utility
+	The “search success” message is displayed on top when you launch a new Video Parser Wizard from the menu.
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
+	When there are two manifest files in the video trace, the buffer graph for bytes and seconds are inaccurate.
+ 	“Show Video viewer” will still be available for traces with no videos and when clicked, it will open the previous trace video but it will not be playable.

#### •	Other Issues
+	When the Graph on the Diagnostics Tab is zoomed in, the video in the Image/Viewer may take longer to respond to commands such as Pause.
+	In the Diagnostic chart it shows a delay of the chunk arriving as a stall on the graph. However on the video viewer the spinning wheel is not displayed so it is not really a stall.
