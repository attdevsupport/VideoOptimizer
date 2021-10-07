
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
#### Video Optimizer 4.2

**System Requirements for Video Optimizer 4.2:**

*Before you download Video Optimizer 4.2, make sure you meet the following system requirements for your operating system.*

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



**Video Optimizer 4.2 Features**
- New graph showing device temperature and Thermal mitigation status for Android devices is added to the diagnostics tab. Values are collected every 5 seconds.

- Analysis of IPV6 data is now supported, increasing the number of apps that allow full video stream analysis.

- New graph displaying TCP handshake latency per session has been added to the diagnostics tab.

- VO now calculates and displays minimum, maximum, and average TCP handshake latency of a trace on the statistics tab.

- Full support for opening, reading and analyzing pcapng files.

- VO now calculates and displays Total Payload bytes and Percentages for every Application and IP address in a trace. New columns have been added to the End point summary charts for both per Application and per IP address statistics.

- CSI (Chunk Sequence Inferencer) analysis is automatically reloaded every time a trace is re-opened.

- User Interface Enhancements:
  -	Time Range Analysis:
   - Start time value can now be entered in minutes: seconds format or hours: minutes: seconds format and automatically converted to seconds format.
   - Analysis details are now selectable and can be copied to another application for comparison and further analysis.
  -	Option provided to reload a trace after making changes in the user preferences window.
  -	Select IPs/application feature allows for filtering of IPV4/IPV6 data as well as TCP/UDP/DNS packets.
  - Labels have been added to the Y-axis of the Throughput graph on the Diagnostics tab.
  - When VO opens a raw pcap file, the user is given the option to move it into a self-contained trace folder with the same name as the original pcap file. This feature automates better organization when analyzing multiple pcap files.
  - Overall speed improvements when opening traces.
- Video Enhancements:
 - Startup delay values are persistent when traces are reloaded, including for CSI analysis.
 - When segment 0 details are available for HLS video streams, they are now displayed.
 - Segment playtime graphs are now more accurate.
 - Start-up delay label changed to “Startup Time” for clarity.


**Known issues in Release 4.2**

- Total bytes count is off in a small percentage of iOS traces, typically by less than 1%.
- CPU temperature is not displayed for certain Android devices, primarily Pixel devices and older Android versions.
- When opening a pcap file, if the user says “No” to create a new directory, no progress bars appear to indicate that VO is opening the file.

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
