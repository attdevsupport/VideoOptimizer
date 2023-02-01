
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

The Video Optimizer Data Analyzer is the component of the Video Optimizer application that evalu  ates the trace data collected from an application and generates statistical and analytical results based on recommended best practices.

This Open Source Code Package contains all of the code needed to build both of these Video Optimizer components.


**Video Optimizer Web Resources:**  
Video Optimizer: https://developer.att.com/video-optimizer<br/>
Video Optimizer Support: https://developer.att.com/video-optimizer/support<br/>
Contact Us: http://developer.att.com/developer/contact_us.jsp<br/>


**Version:**  
#### Video Optimizer 4.6

**System Requirements for Video Optimizer 4.6:**

*Before you download Video Optimizer 4.6, make sure you meet the following system requirements for your operating system.*

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
  - MacFuse (formerly known as OSXFuse)
  - Libimobiledevice & ideviceinstaller
  - For iOS developers, use latest version of XCode and OS X
  - VLC media player 3.x.x or higher
  - Wireshark
- Linux requirements
  - For Ubuntu 18.04.x LTS (and newer) users, use VLC media player version 3.x.x
  - For Ubuntu 16.04.x LTS (and older) users, use VLC media player version 3.x.x up to 3.0.8
  - Wireshark



**Video Optimizer 4.6 New Features**
- Native support for Apple silicon (M1/M2) with a dedicated aarch64 installer.

- SNI information is now available for TCP and QUIC sessions.

- Date/Time/Time Zone are recorded as part of trace collection to improve video synchronization.

- Network and Device IPs are recorded for each trace for better categorization of uplink/downlink data.

- Carrier Name/ Radio Cell ID is recorded during the trace collection for Android devices.

- Export feature for various tables and graphs auto-populate with the appropriate trace name.

- Support multiple time resolutions (.1s,.2s,.5s,1s,2s,5s) with the default set to 1 second for graphs on the diagnostics tab.

- The Time Range analysis dialog now displays maximum throughput for both Uplink and Downlink data.

- Filters applied to the diagnostics tab are automatically applied to the Time Range analysis.

- Developed a script to automatically install libimobile dependencies for Video Optimizer.

- https://raw.githubusercontent.com/attdevsupport/VideoOptimzer/master/vo_dependency_installer.sh

- The preferences dialog autodetects installed dependencies and populates the path.

- Video Optimizer automatically checks installed versions of dependencies and raises an alert if updates are available.


**Known issues in Release 4.6**

- Tools -> Edit Metadata is disabled. Double-clicking the trace notes is the workaround.

- File -> open trace with time range is not saving custom time ranges when used for analysis.

- A workaround is to manually add a custom time range to the time-range.json file.

- When Video Optimizer is not able to parse the Remote or Host IP, for some QUIC video scenarios, SNI is not available.

- Live screen capture window from the iPhone relies on the Command Line Tools from XCode. Recent changes make it much slower to receive a screenshot. Sometimes the live screen capture will be delayed by 2 secs.  


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
