
# Video Optimizer

All works distributed in this package are covered by the Apache 2.0 License unless otherwise stated.

> Copyright 2018
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
#### Video Optimizer 2.2.1

**System Requirements for Video Optimizer 2.2.1:**

Before you download Video Optimizer 2.2.1, make sure you meet the following system requirements for your operating system.

1. 2GB or more for RAM
2. Java 8 or above
3. Android SDK Level 19 or above for collecting Android devices trace
4. Latest Xcode for collecting iOS devices trace
5. WinPcap for Windows platform
6. FFMpeg library and VLC media player installed and configured for HD video
7. Brew, Ifuse, Xfuse, WireShark  for MacOS platform
8. Wireshark



#### Video Optimizer 2.2.1 Features

#### • New Best Practice “Variable Bitrate” is added to test if the video stream is using Variable bitrate or not.

#### • New ‘Configuration Required’ icon added to indicate if the Video Best Practice tests requires additional configuration

#### • Time Range analysis has been re-designed with addition of Total Payload Data displayed in Best Practices trace summary and Time range analysis results section.

#### • Attenuator enhancements to support Android 9 “Pie”

#### • iOS Secure Collector (Beta version)

#### • Android Secure Collector supports Pie version

#### •	Other enhancements include:
#### - Handling special characters in file names
#### - Handling missing traces in the recent files
#### - Updates to the iOS collector installation
#### - Updates to Mac install Pre-requisites list


### What are the known issues in Release 2.2.1?

#### •	Application names, CPU usage, signal strength is not displayed for traces taken with Android Version Oreo and above.
#### •	Some iOS traces taken using HD show as low resolution. 
#### •	No traffic file is displayed for iOS HD traces(intermittent). 

### •  Installation instruction
+ Please follow the order to compile projects
+ ../ARO.Parent
+ ../ARO.Core
+ ../ARO.Analytics
+ ../DataCollectors/ARO.IOSCollector
+ ../DataCollectors/ARO.NorootAndroidCollector
+ ../DataCollectors/ARO.RootedAndroidCollector
+ ../ARO.UI 
+ ../ARO.Console 
