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
package com.att.arocollector.packetRebuild;

import java.io.IOException;

/**
 * Interface for writing capture file.<br>
 * There are many formats for capture files which may require
 * different handling, but this difference is not relevant when writing
 * a capture file for analyzing it packets.<br>
 * The interface provides the required abstraction.<br>
 * 
 * 
 * @author roni bar yanai
 *
 */
public interface CaptureFileWriter 
{
	/**
	 * write packet to file.
	 * @param thepkt - packet as byte array
	 * @param time - time in nano seconds.
	 * @return true for success.
	 * @throws IOException
	 */
	public boolean addPacket(byte[] thepkt,long time) throws IOException;
	
	
	/**
	 * close the file, make sure data flushed to disk.
	 * (will happen automatically eventually, should always be called when we want
	 *   to use the created file in the code)
	 * @throws IOException 
	 */
	public void close() throws IOException;
}
