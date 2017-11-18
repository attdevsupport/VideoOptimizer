/*
 *  Copyright 2015 AT&T
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
package com.att.aro.ui.utils;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Utility class 
 */
public class CommonHelper {

	/**
	 * Compare the null object
	 * @param toCheck
	 * @param ifNull
	 * @return
	 */
	public static <T> T ifNull(T toCheck, T ifNull){
	       if(toCheck == null){
	           return ifNull;
	       }
	       return toCheck;
	}
	
	/**
	 * Check for null objects
	 * @param argument
	 * @return
	 */
	public static <T> boolean isNotNull(T argument) {
	    return !(argument == null);
	}
	
	private String getMessage(String key) {
		return ResourceBundleHelper.getMessageString(key);
	}
	

	/**
	 * This is the utility method for number of the speed setting match with the cellular networks terms
	 * Define for Down link
	 * 1-10 LTE
	 * 11-125 4g
	 * 126-1999 3g
	 * 2000 2g
	 */

	public String transferSignalSignDownload(int number) {
		if (number == 0) {
			return getMessage("waterfall.na");
		} else if (number > 0 && number < 10) {
			return getMessage("dlog.collector.option.attenuator.4glte");
		} else if (number >= 10 && number < 126) {
			return getMessage("dlog.collector.option.attenuator.4g");
		} else if (number >= 126 && number < 2000) {
			return getMessage("dlog.collector.option.attenuator.3g");
		} else {
			return getMessage("dlog.collector.option.attenuator.2g");
		}

	}

	/**
	 * This is the utility method for number of the speed setting match with the cellular networks terms
	 * Define for Down link
	 * 102400 LTE
	 * 12288 - 102399 4g
	 * 5120 - 12287 3g
	 * 64 - 5119 2g
	 */

	public String numberTransferSignalDL(int number){
		if (number < 0) {
			return getMessage("waterfall.na");
		}else if(number ==0){
			return getMessage("dlog.collector.option.attenuator.block");
		}else if (number > 0 && number < 64) {
			return getMessage("dlog.collector.option.attenuator.2g");
		} else if (number >= 64 && number < 5120) {
			return getMessage("dlog.collector.option.attenuator.3g");
		} else if (number >= 5120 && number < 12288) {
			return getMessage("dlog.collector.option.attenuator.4g");
		} else {
			return getMessage("dlog.collector.option.attenuator.4glte");
		}

	}
	
	
	/**
	 * This is the utility method for number of the speed setting match with the cellular networks terms
	 * Define for up link
	 * 1-27 LTE
	 * 27-77 4g
	 * 77-84 3g
	 * 84 above 2g
	 */
	
	public String transferSignalSignUpload(int number){
		if (number == 0) {
			return getMessage("waterfall.na");
		} else if (number > 0 && number < 27) {
			return getMessage("dlog.collector.option.attenuator.4glte");
		} else if (number >= 27 && number < 77) {
			return getMessage("dlog.collector.option.attenuator.4g");
		} else if (number >= 77 && number < 84) {
			return getMessage("dlog.collector.option.attenuator.3g");
		} else {
			return getMessage("dlog.collector.option.attenuator.2g");
		}
	}
	
	/**
	 * This is the utility method for number of the speed setting match with the cellular networks terms
	 * Define for Up link
	 * 102400 LTE
	 * 12288 - 102399 4g
	 * 5120 - 12287 3g
	 * 64 - 5119 2g
	 */

	public String numberTransferSignalUL(int number){
		if (number < 0) {
			return getMessage("waterfall.na");
		}else if(number == 0){
			return getMessage("dlog.collector.option.attenuator.block");
		}else if (number > 0 && number < 64) {
			return getMessage("dlog.collector.option.attenuator.2g");
		} else if (number >= 64 && number < 5120) {
			return getMessage("dlog.collector.option.attenuator.3g");
		} else if (number >= 5120 && number < 12288) {
			return getMessage("dlog.collector.option.attenuator.4g");
		} else {
			return getMessage("dlog.collector.option.attenuator.4glte");
		}

	}

    public String messageConvert(int kbps) {
        String message = "";
        if(kbps >= 1024 * 100) {
            message = " None";
        } else if (kbps > 1024) {
            message = " " + unitConvert(kbps) + " Mbps";
        } else {
            message = " " + kbps + " Kbps";
        }
         return message;
    }

    private String unitConvert(int kbps) {
        double mTemp = kbps / 1024;
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(mTemp);

    }
	
}
