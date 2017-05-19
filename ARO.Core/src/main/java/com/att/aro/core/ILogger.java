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
package com.att.aro.core;

import org.apache.log4j.Level;

public interface ILogger {
	void debug(String message);
	void debug(String message, Throwable throwable);
	void error(String error);
	void error(String error, Throwable throwable);
	void info(String info);
	void info(String info, Throwable throwable);
	void warn(String warn);
	void warn(String warn, Throwable throwable);
	Level getLevel();
	Level setLevel(Level level);
	/**<PRE>
	 * Record a message into the log under Level.INFO regardless of log4j.properties
	 * Use only for important messages about usage of the analyzer.
	 * eg. trace analyzed, trace launched
	 * 
	 * <B>Do not use for debugging!!!</B></PRE>
	 * 
	 * @param message
	 */
	void elevatedInfo(String message);
}
