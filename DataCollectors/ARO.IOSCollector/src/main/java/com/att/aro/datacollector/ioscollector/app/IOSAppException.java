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

package com.att.aro.datacollector.ioscollector.app;

import com.att.aro.core.pojo.ErrorCode;

public class IOSAppException extends Exception {

	private static final long serialVersionUID = 1L;
	private ErrorCode errCode;
	
	public IOSAppException(ErrorCode errCode) {
		super(errCode.getDescription());
		this.errCode = errCode;
	}
		
	public IOSAppException(String message) {
		super(message);
	}
	
	public ErrorCode getErrorCode() {
		return errCode;
	}
}