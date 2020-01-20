
# Video Optimizer

All works distributed in this package are covered by the Apache 2.0 License unless otherwise stated.

> Copyright 2019
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
#### Video Optimizer 3.0

**System Requirements for Video Optimizer 3.0:**

Before you download Video Optimizer 3.0, make sure you meet the following system requirements for your operating system.

1. At least 2GB of RAM, but recommend at least 8GB
2. Java 8 or above
3. For Android developers, Android SDK Level 19 or above
4. Latest Xcode for collecting iOS devices trace
5. WinPcap or NPcap for Windows platform
6. FFMpeg library
7. VLC media player for Windows and Linux platform
8. Brew, Ifuse, OSXfuse ,libimobiledevice , ideviceinstallerfor, Wireshark  MacOS platform


#### Video Optimizer 3.0 Features

#### • Audio Analyis added for Video Streaming Applications
##### -	Added a new Audio Best Practice called "Streaming Separate Audio & Video" - Added as a self test detecting Muxed & Demuxed Video.
##### - Video Tab has separate tables to display Audio Segments for Demuxed Video.

#### • Video Enhancements
##### -	Corrected the Video Stall Indicator to display the exact stall.
##### - The video segment table has been updated to support streams with separate audio & video segments. A “Content” column indicates whether a segment is audio, video, or muxed audio & video.
##### -	A “Stall time” column has been added in the video/audio segment table to indicate the time when the stall occurred.
##### - A “TCP state” column has been added to the video/audio segment table to indicate when a packet is a “reset” or “finished” packet.
##### -	The “Quality” column in the video/audio segment table was renamed to the more accurate “Track.”.
##### -	Any gaps in segments are now identified in the Video Stream section.
##### -	Support has been added for more variance of HLS and DASH manifests for wider support of video streaming applications.

#### • Device & Desktop OS Support
##### -	Added Support for Collection on Android 10.
##### -	Added Support for Mac OS Catalina

#### • Other Enhancements
##### - Simplified HD trace collection on iOS devices. No longer requires install of an app on iOS devices.
##### - Export option in diagnostics tab is enhanced to support exporting only rows selected by the user.
##### - Sorting feature has been added to the tables in the Statistics tab.


### Known issues in Release 3.0

#### •	Some of the video traces don’t display Byte and time information under Video Summary table on Windows and Linux platform
#### •	In some traces, not all segment gaps are identified.


### Compilation instructions
+ Please follow the order to compile projects
+ ../ARO.Parent
+ ../ARO.Core
+ ../ARO.Analytics
+ ../DataCollectors/ARO.IOSCollector
+ ../DataCollectors/ARO.NorootAndroidCollector
+ ../DataCollectors/ARO.RootedAndroidCollector
+ ../ARO.UI
+ ../ARO.Console
