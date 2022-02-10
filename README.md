
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
#### Video Optimizer 4.4

**System Requirements for Video Optimizer 4.4:**

*Before you download Video Optimizer 4.4, make sure you meet the following system requirements for your operating system.*

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



**Video Optimizer 4.4 New Features**
- Automatic DNS lookup now assigns names to the endpoints associated with a session regardless of the session type (when available).

- The SNI (server name indicator) is now automatically determined for all TLS flows and can be quickly ascertained by double clicking a flow and copying it from the pop-up window.

- Filter options have been added to Time Range Analysis to allow for IPv4, IPv6, TCP, UDP and DNS filtering. The statistics button will display results based on selected filters. Reanalysis will be done based on selected filters.

- Complete environment details are captured and stored in json format that is searchable without opening a trace. Details include:
  - Video Optimizer version
  - VPN Collector version
  - Device make/model and OS
  - Host machine OS and Java versions
  - Xcode, Libimobiledevice and dumpcap versions (for Mac)

- Optional trace details can now be added directly in the Start Collector window when launching a trace, or hidden if not needed.

- Enhancements:
  -	Improved accuracy of the throughput graph for throttled Android traces.
  - When performing pcap analysis, if the user opts to not retain the sub-directory VO creates, the original path is remembered, where the file was saved.  
  - Addition of a Trace Notes section in the trace summary where the user can add useful trace info which can later be read or searched without even opening a trace.
  -	Enhancements to allow easier viewing and editing of trace metadata, including simple double-clicking the notes field.
  -	Automatic restart of VO upon receiving a memory exception, followed by an informational pop-up box explaining the crash and recovery.

- Video Enhancements:
    - Segment Buffer display is populated based on VO estimated startup time even if the user does not manually set a startup time in the StartupDelay Dialog.
    - Video Best-Practices now show Pass/Fail results based on VO estimated startup time even if the user does not manually set a startup time in the StartupDelay Dialog.
    - VO now shows segments that failed to download in the Video Stream tables, in addition to successfully downloaded segments. “TCP state” column indicates “Failed” for these segments.
    - Network type graph now displays user friendly names for 5G Sub-6, 5G mmWave and Advanced pro LTE network types.
    - Clicking on the Configuration Required icon next to each of the video best practices on the Best Practices/Results tab navigates to the Video tab.


**Known issues in Release 4.4**

- Video analysis can display incorrect results if the Stall Pause Point, and Stall Recovery fields in the File->Preferences->Video tab use the default value of 0. We suggest changing this to a value of 0.1 or greater to get a proper analysis. This will also resolve the issue of the Stalls, Startup Delay, and Buffer Occupancy results showing that Startup Delay has not been set even when it has.  


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
