
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
2. Java 8 or above
3. Android SDK Level 19 or above for collecting Android devices trace
4. Latest Xcode for collecting iOS devices trace
5. WinPcap and WireShark for Windows platform
6. FFMpeg library and VLC media player installed and configured for HD video

#### Video Optimizer 1.3 Features

#### • New Best Practice: Concurrent Session
Concurrent session is a new best practice that is added to the set of Video Best Practices section. The concurrent session best practice displays the maximum number of concurrent sessions for each manifest in the Video trace. The table displays the manifest name, the maximum number of concurrent sessions for the manifest and the duration that concurrency was active for. If the maximum concurrency occurs more than once in the manifest then the duration of concurrency for each instance is added up and displayed in the table.

#### • New Best Practice: Image comparison
This best practice compares the dimensions of the downloaded images verses the dimensions displayed on the screen and fails the test if it is greater than or equal to 50% savings of pixels.

#### • New Best Practice: Multiple simultaneous connections to Many Endpoints
Opening many connections all at once can cause bottlenecks in slower network conditions. This best practice will help detect those connections that connect to many endpoints. It triggers fail when it detects more than 12 connections in use at the same time.

#### • Video best practices pass fail configuration
Startup delay, Stall duration and Segment Redundancy best practices now provide user an option to trigger pass and fail criteria. Users will have an option under File, under preferences to define the custom values.


#### • Command Line option to throttle speed for Network Attenuator
Video Optimizer command line will now accept both Uplink and Downlink parameters applied at the same time via command line options.

#### • DNS Packet Analysis
This release has enhancements to the best practice tests in the area of image format. Video Optimizer converts all the images to WebP format (for Android) or Jpeg2000 format (for iOS) and it gives a “Pass” if there are no savings after the conversion. This best practice will give a “Pass” when there are no images with a savings of more than 15 percent, and a Fail if there is at least one image with more than 15 percent savings when converted.

#### • Video Request Table
Video Request Table in Video tab now has an option to export the video requests to an excel sheet and allows to save in user preferred location.

#### • Extend Manifest Dash Format
There is a new MPD(XML) format that is parsed into a ManifestDASH which can handle “.m4a” video segments.

#### • Preferences UI
Preferences UI under the File menu now provides 3 logging levels such as debug, info, and error options to collect the log so it helps in debugging if any issues. It also has configurable paths for ffmpeg, ffprobe and dumpcap.

#### • iOS Enhancements
When collecting iOS traces, user will now have an option to select HD recording. Also, iOS traces now capture the Bluetooth event if it is on and will be displayed in Diagnostics chart.  

### What are the known issues in Release 1.3?

#### •	Video Best Practices
+	Under preferences, when user enters a higher value than the default fail value for any of the video best practices, it saves the value without providing any error message. It instead saves the profile but doesn’t apply. Make sure to enter a value lower than default value.
+	In the Video Results Summary table under Video tab, the values will remain, even if the manifest file(s) is deselected after setting the startup delay. Workaround is for the user to set the start-up delay/reselect the startup delay of a movie they would like to analyze.
+ The fields ‘Network Comparison’, ‘ IP sessions’ and ‘IP Address’ under the video results summary table are displaying results for all videos regardless of video selection.

#### •	Android trace analysis result
+ Application names are not displayed for traces taken with Oreo version

#### •	Android Device Data Capture – CPU Temperature
+	CPU temperature is device dependent.

#### •	iOS setting
+ User will require to create a new provision file if user deletes provisioning profile and certificate from the preferences
+	Screen capture is failing while collecting a trace via Command line interface for iOS devices.

#### •	Video Parser Utility
+	IP addresses/IP Sessions fields in the Video tab will display all the sessions and addresses related to the entire traffic, not particular for the video traffic. Additionally, it doesn’t update when manifest selection changes.
+	In the Video Parser Wizard window, the result section of the wizard does not save the previously chosen XREF options for the data capture group when the next set of capture group is entered.
+	When a user sees a negative segment number, it means that there is no manifest in HLS and no recognized segment number is present in the request.

#### •	Miscellaneous issues
+	Unresponsive UI due to specific traces causing exceptions. Workaround is to restart the application.

### •  Installation instruction
+ Please follow the order to compile projects
+ ../ARO.Core
+ ../ARO.Analytics
+ ../DataCollectors/ARO.IOSCollector
+ ../DataCollectors/ARO.NorootAndroidCollector
+ ../DataCollectors/ARO.RootedAndroidCollector
+ ../ARO.Console  
+ ../ARO.UI
