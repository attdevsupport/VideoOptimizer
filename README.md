
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
#### Video Optimizer 3.2

**System Requirements for Video Optimizer 3.2:**

*Before you download Video Optimizer 3.2, make sure you meet the following system requirements for your operating system.*

- At least 4GB of RAM, but recommend at least 8GB
- Java 8 or above
- For Android developers, Android SDK Level 23 or above
- FFmpeg
- Windows requirements
  - Wireshark (Install Npcap addon with Winpcap API-compatible mode)
  - VLC media player 3.x.x or higher
- Mac requirements
  - Brew (or any similar package manager)
  - Ifuse
  - OSXFuse
  - Libimobiledevice & ideviceinstaller
  - For iOS developers, use latest version of Xcode and OS X
  - VLC media player 3.x.x or higher
  - Wireshark
- Linux requirements
  - For Ubuntu 18.04.x LTS (and newer) users, use VLC media player version 3.x.x
  - For Ubuntu 16.04.x LTS (and older) users, use VLC media player version 3.x.x up to 3.0.8
  - Wireshark



**Video Optimizer 3.2 Features**

- New “Delay” feature is added to Request Response panel under Diagnostics tab. It displays the HTTP Level Delay for every HTTP Request and Response
  - This new Delay tab will display Request Time Stamp, First Packet Arrival Time, First Packet Delay, Last Packet Arrival Time, Last Packet Delay and Content length

- Export feature in the TCP/UDP table includes timestamps for SYN and SYN-ACK

- In the Diagnostics tab, Session Count graph is updated to reflect the session closure flags

- In the Diagnostics tab, User Input graph includes additional user events such as screen touch, volume, power and screen orientation

- Dynamic Network Attenuator is enhanced to simulate network latency

- Startup delay dialog is enhanced to identify/select the video playback requested time based on user touch

- Video stream table is redesigned to accommodate more information

- Video stream table includes Export feature

- Best Practice test “Network Comparison” results table includes “Declared Manifest Bandwidth” & “Calculated Network Bitrate data”

- Best Practice test “Multiple Simultaneous Connections to One Endpoint” is modified to fail the test if it detects more than 7 connections to the same server

- Best Practice test “Analyzing the Adaptive Bitrate Ladder” results table displays Resolution, Playback %, Segment Count, Total Duration, Segment Position, Declared Bitrate, and

- Average Bitrate information for each Track

- Best Practice test “HTTPS Usage” includes Total HTTPS Data, percentage of Total HTTPS Data, and Total Unanalyzed HTTPS Data

- macOS users will be able to configure the Wireshark path under Video Optimizer->File->Preferences

- Export options are enhanced with default file name based on the trace and saves the file under the trace folder


**Known issues in Release 3.2**

- Playtime propagation and therefore stall detection issues appear intermittently in some traces
- Time stamp shows 0.00 in the Request/Response view for some of the iOS traces

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
