/*
 *  Copyright 2017 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.console;

import org.springframework.context.ApplicationContext;

import com.att.aro.console.util.ThrottleUtil;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.pojo.ErrorCode;

/**
 * validate commands against arguments
 * choose start collector or analyze ->  rest of the features
 */
public class Validator {
	public ErrorCode validate(Commands cmd, ApplicationContext context) {
		if (cmd.getAnalyze() != null) {
			if (cmd.getFormat().equals("json") && cmd.getFormat().equals("html")) {
				return ErrorCodeRegistry.getUnsupportedFormat();
			}

			if (cmd.getOutput() == null) {
				return ErrorCodeRegistry.getOutputRequired();
			}
			IFileManager filemg = context.getBean(IFileManager.class);
			if (filemg.fileExist(cmd.getOutput())) {
				if ("yes".equals(cmd.getOverwrite())) {
					filemg.deleteFile(cmd.getOutput());
				} else {
					return ErrorCodeRegistry.getFileExist();
				}
			}
		}else{
			if (cmd.getStartcollector() != null) {
				String colname = cmd.getStartcollector();
				if (!"rooted_android".equals(colname)
				 && !"vpn_android".equals(colname)
				 && !"ios".equals(colname)) {
					return ErrorCodeRegistry.getUnsupportedCollector();
				}
				if (cmd.getOutput() == null) {
					return ErrorCodeRegistry.getOutputRequired();
				}
			}

			if (cmd.getVideo() != null
					&& !cmd.getVideo().equals("yes")
					&& !cmd.getVideo().equals("no")
					&& !cmd.getVideo().equals("hd")
					&& !cmd.getVideo().equals("sd")
					&& !cmd.getVideo().equals("slow")
					) {
				return ErrorCodeRegistry.getInvalidVideoOption();
			}
			
			ErrorCode uplinkErrorCode = validateUplink(cmd);
			if (uplinkErrorCode != null) {
				return uplinkErrorCode;
			}
			ErrorCode downlinkErrorCode = validateDownlink(cmd);
			if (downlinkErrorCode != null) {
				return downlinkErrorCode;
			}
		}
		return null;
	}
	
	private ErrorCode validateUplink(Commands cmd) {
		if (cmd.getThrottleUL() != "-1") {
			if (("ios".equals(cmd.getStartcollector()) ||"vpn_android".equals(cmd.getStartcollector())) 
					&& !isNumberInRange(cmd.getThrottleDL(), 64, 102400)) {
				return ErrorCodeRegistry.getInvalidUplink();
			} else if ("rooted_android".equals(cmd.getStartcollector())) {
				return ErrorCodeRegistry.getUnsupportedCollector();
			}
		}
		return null;
	}
	
	private ErrorCode validateDownlink(Commands cmd) {
		if (cmd.getThrottleDL() != "-1") {
			if (("ios".equals(cmd.getStartcollector()) || "vpn_android".equals(cmd.getStartcollector())) 
					&& !isNumberInRange(cmd.getThrottleDL(), 64, 102400)) {								
				return ErrorCodeRegistry.getInvalidDownlink();
			} else if ( "rooted_android".equals(cmd.getStartcollector())) {
				return ErrorCodeRegistry.getUnsupportedCollector();
			}
		}
		return null;
	}
	
		
	private boolean isNumberInRange(String number, int from, int to) {
		int throughput = ThrottleUtil.getInstance().parseNumCvtUnit(number);
		return ThrottleUtil.getInstance().isNumberInRange(throughput, from, to);
	}
		
}
