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
package com.att.aro.core.bestpractice.pojo;

/**
 *
 * list out all ARO defined (default) private data type for security best practice 2: transmission of private data
 * 
 */
public enum PrivateDataType {
	
	regex_phone_number ("Phone Number"),
	
	regex_date_birth ("Date of Birth"),
	
	regex_credit_card_number ("Credit Card"),
	
	regex_ssn ("SSN"),
	
	regex_other ("Other");
	
	private final String name;
	
	private PrivateDataType(String name) {
		this.name = name;
	}
	
	public String toString() {
		return name;
	}
}
