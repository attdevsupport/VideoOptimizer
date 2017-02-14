#Video Optimizer

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
> The AT&T Application Resource Optimizer(ARO) uses Open Source Software that is licensed under the GNU Lesser General Public License (LGPL) version 3 (the "License"), and you may not use this file except in compliance with the License. You may obtain a copy of the Licenses at: http://www.jfree.org/jfreechart/. Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licenses for the specific language governing permissions and limitations under the Licenses.  

**JCommon**  
> The AT&T Application Resource Optimizer(ARO) uses Open Source Software that is licensed under the GNU Lesser General Public License (LGPL) version 2.1 (the "License"), and you may not use this file except in compliance with the License. You may obtain a copy of the Licenses at: http://www.jfree.org/jcommon/. Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licenses for the specific language governing permissions and limitations under the Licenses.  

**FFmpeg**  
> The AT&T Application Resource Optimizer(ARO) uses Open Source Software that is licensed under the GNU Lesser General Public License (LGPL) version 2.1 (the "License"), and you may not use this file except in compliance with the License. You may obtain a copy of the Licenses at: http://ffmpeg.org/download.html. Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licenses for the specific language governing permissions and limitations under the Licenses.  

**TCPDUMP/LIBPCAP**  
> The AT&T Application Resource Optimizer(ARO) uses Open Source Software that is licensed under the following BSD (the "License"), and you may not use this file except in compliance with the License. You may obtain a copy of the Licenses at: http://www.tcpdump.org/#contribute. Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licenses for the specific language governing permissions and limitations under the Licenses. License: BSD Redistribution and use in source and binary forms, with or withoutmodification, are permitted provided that the following conditionsare met: 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/o> rials provided with the distribution. 3. The names of the authors may not be used to endorse or promote products derived from this software without specific prior written permission.  THIS SOFTWARE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS ORIMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIEDWARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  

##Open Source Code Package

Video Optimizer allows mobile application developers to test the network performance of video, images and other files for compression, speed, battery drain and other aspects of performance.

The Data Collector is the component of the Video Optimizer application that captures the data traffic of a mobile device and stores that information in trace files that can be analyzed using the Video Optimizer Data Analyzer.

The Video Optimizer Data Analyzer is the component of the Video Optimizer application that evaluates the trace data collected from an application and generates statistical and analytical results based on recommended best practices.

This Open Source Code Package contains all of the code needed to build both of these Video Optimizer components.


**Video Optimizer Web Resources:**  
Video Optimizer: https://developer.att.com/video-optimizer 
Video Optimizer Support: https://developer.att.com/video-optimizer/support
Contact Us: http://developer.att.com/developer/contact_us.jsp


**Version:**  
Video Optimizer 1.0  

**System Requirements for Video Optimizer 1.0:**

Before you download Video Optimizer, make sure you meet the following system requirements for your operating system.

1. 2GB or more for RAM
2. Java 7 or above (should have the latest update of Java 1.7 or above)
3. Android SDK Level 19 or above for collecting Android devices trace
4. Latest Xcode for collecting iOS devices trace
5. WinPcap for windows user
6. FFMpeg library installed and configured for HD video

### Video Optimizer 1.0 Features

#### • Video Best Practices
A group of Best Practice tests that offer guidance on mobile development issues with video. These tests let you visualize and diagnose common streaming issues to optimally balance Deliver Quality (DQ) versus Video Quality (VQ). The DQ key performance indicators (KPI) are startup time, stalls, track switches, and latency.

#### •	Security Best Practices
A section of Best Practice tests in the area of security help you check that your app is using HTTPS to better secure private data, and that you are using the latest HTTPS version to help you avoid unsecure SSL connections. Other security tests look for weak ciphers to protect the data you are transmitting with a stronger method of encryption and checks that the ciphers you are using support forward secrecy.

#### •	Image Best Practices
New best practice tests to help you use images more efficiently. One new test looks at your image metadata to determine if unnecessary metadata is being included. Metadata can greatly increase file size, but has little benefit for the end user. Removing extra metadata makes the images smaller and lets them download faster.
Another new test helps you find the best level of image compression so that the images in your app maintain quality at a smaller size. You can analyze all images (JPG, GIF, PNG, WebP) to ensure that the image was properly compressed.

#### •  Peripheral support using the VPN Collector
The VPN collector in Video Optimizer 1.0 can collect data from peripherals.

### What are the known issues in Release 1.0?

#### •	Security Best Practices
+	In our testing, only Android devices with Kitkat OS detected a weak cipher.
+	Private data transmission: There is a false positive scenario that Analyzer reports a sixteen digit number as a credit card number.

#### •	Video Best Practices
+	Traces that are collected from a paused video do not capture the manifest file, so the thumbnails are not displayed.
+	Some traces do not generate segments as the manifest file is not created.
+	Only stalls have the pass/fail criteria. All the other video best practices are currently informative only.
+	Although there are stalls in the trace, sometimes the buffer occupancy graph doesn't report them.
+	After setting the video start-up delay, you cannot set the recovery time that occurred in the trace.

#### •	HD/SD Video Options
+	HD option is disabled for windows.
+	HD/SD option is disabled for Linux.

#### •	Analyzer
+	HTTPS cache statistics tab displays all zeros.
+	When the user click the zoom option on the Diagnostics Tab, the cursor is not kept at the same point.

#### •	Other Issues
+	When the Graph on the Diagnostics Tab is zoomed in, the video in the Image/Viewer may take longer to respond to commands such as Pause.
+	In the Diagnostic chart it shows a delay of the chunk arriving as a stall on the graph. However on the video viewer the spinning wheel is not displayed so it is not really a stall.
