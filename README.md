## Video Optimizer Release 1.0 (02/13/2017)

Video Optimizer allows mobile application developers to test the network performance of video, images and other files for compression, speed, battery drain and other aspects of performance.

### System Requirements for Video Optimizer 1.0

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
