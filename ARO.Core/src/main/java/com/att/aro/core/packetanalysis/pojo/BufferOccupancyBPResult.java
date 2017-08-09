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
package com.att.aro.core.packetanalysis.pojo;

import java.util.List;

public class BufferOccupancyBPResult {

	private double maxBuffer=0.0;
	private List<Double> bufferByteDataSet;
	
	public BufferOccupancyBPResult(double maxBuffer){
		this.maxBuffer = maxBuffer;
	}
	
	public  double getMaxBuffer(){
		return maxBuffer;
	}

	public void setMaxBuffer(double maxBuffer) {
		this.maxBuffer = maxBuffer;
	}

	public List<Double> getBufferByteDataSet() {
		return bufferByteDataSet;
	}

	public void setBufferByteDataSet(List<Double> bufferByteDataSet) {
		this.bufferByteDataSet = bufferByteDataSet;
	}
	
	
}
