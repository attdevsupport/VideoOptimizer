/*
 *  Copyright 2019 AT&T
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
package com.att.aro.core.packetanalysis.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.packetanalysis.IByteArrayLineReader;
import com.att.aro.core.packetanalysis.IHttpRequestResponseHelper;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.Session;

/**
 * helper class for dealing HttpRequestResponseInfo object
 * Date: November 20, 2014
 */
public class HttpRequestResponseHelperImpl implements IHttpRequestResponseHelper {
	private static final int TWO_MB = 2 * 1024 * 1024;
	private static final Logger LOG = LogManager.getLogger(HttpRequestResponseHelperImpl.class.getName());
	
	private IByteArrayLineReader storageReader;
	
	@Autowired
	public void setByteArrayLineReader(IByteArrayLineReader reader){
		this.storageReader = reader;
	}

	/**
	 * Indicates whether the content type is CSS or not.
	 * 
	 * The following content types are considered as CSS:
	 * 
	 * - text/css
	 * 
	 * @return returns true if the content type is CSS otherwise return false
	 * 
	 */
	public boolean isCss(String contentType) {
		return "text/css".equals(contentType);
	}

	/**
	 * Indicates whether the content type is HTML or not.
	 * 
	 * The following content types are considered as HTML:
	 * 
	 * - text/html
	 * 
	 * @return returns true if the content type is HTML otherwise return false
	 * 
	 */
	public boolean isHtml(String contentType) {
		return "text/html".equals(contentType);
	}

	public boolean isJSON(String contentType) {
		return "application/json".equals(contentType);
	}

	/**
	 * Indicates whether the content type is JavaScript or not.
	 * 
	 * The following content types are considered as JavaScript:
	 * 
	 * - application/ecmascript - application/javascript - text/javascript
	 * 
	 * @return returns true if the content type is JavaScript otherwise return false
	 * 
	 */
	public boolean isJavaScript(String contentType) {

		return ("application/ecmascript".equals(contentType) || "application/javascript".equals(contentType) || "text/javascript".equals(contentType));
	}

	/**
	 * Returns the request/response body as a text string. The returned text may not be readable.
	 * 
	 * @return The content of the request/response body as a string, or null if the method does not execute successfully.
	 * 
	 * @throws ContentException
	 *                              - When part of the content is not available.
	 */
	public String getContentString(HttpRequestResponseInfo req, Session session) throws Exception {
		byte[] content = getContent(req, session);
		if (content == null || content.length == 0) {
			return "";
		} else if (content.length > TWO_MB) {
			LOG.error("Ignoring this html file as it's too big to process - possibly a speed test file.");
			return "HTML File too big to process";
		} else {
			return new String(content, "UTF-8");
		}
	}

	/**
	 * get content of the request/response in byte[]
	 * 
	 * @param request
	 * @return byte array
	 * @throws Exception
	 */
	public byte[] getContent(HttpRequestResponseInfo request, Session session) throws Exception {

		LOG.debug("getContent(Req, Session) :" + request.toString());
		String contentEncoding = request.getContentEncoding();
		byte[] payload;
		ByteArrayOutputStream output;
		boolean logFlag = false;		
		if(request.getAssocReqResp()==null) {
			logFlag = true;//avoid through NPE when we tried to get logger or exception information
		}

		payload = request.getPayloadData().toByteArray();

		if (request.isChunked()) {
			storageReader.init(payload);
			String line;
			output = new ByteArrayOutputStream();
			while (true) {

				line = storageReader.readLine();

				if (line != null) {
					String[] str = line.split(";");
					int size = 0;
					try {
						String trim = str[0].trim();
						size = Integer.parseInt(trim, 16);
					} catch (NumberFormatException e) {
						LOG.warn("Unexpected begin of the chunk format : " + line);
					}
					if (size > 0) {

						// Save content offsets
						output.write(
								Arrays.copyOfRange(payload, storageReader.getIndex(), storageReader.getIndex() + size));
						storageReader.skipForward(size);

						// CRLF at end of each chunk
						line = storageReader.readLine();

						if (line != null && line.length() > 0) {
							LOG.warn("Unexpected end of chunk: " + line);
						}
					} else {
						request.setChunkModeFinished(true);
						line = storageReader.readLine(); // End of chunks

						if (line != null && line.length() > 0) {
							LOG.warn("Unexpected end of chunked data: " + line);
						}
						break;
					}
				} else {
					break;
				}
			}
			if (request.isChunkModeFinished()) {
				payload = output.toByteArray();
			} else {
				throw new Exception(String.format("Unexpected Chunk End at request time: %.3f, request.getAssocReqResp() is %s The content may be corrupted.", request.getTimeStamp(), logFlag?"N/A":request.getAssocReqResp().getObjNameWithoutParams()));
			}
		} else if (payload.length < request.getContentLength()) {
			request.setExtractable(false);
			int percentage = payload.length / request.getContentLength() * 100;
			throw new Exception(String.format("PayloadException At request time: %.3f, request.getAssocReqResp() is %s . The content may be corrupted. Buffer exceeded: only %d percent arrived", request.getTimeStamp(), logFlag?"N/A":request.getAssocReqResp().getObjNameWithoutParams(), percentage));
		}

		// Decompress GZIP Content
		if ("gzip".equals(contentEncoding) && payload != null) {
			GZIPInputStream gzip = null;
			output = new ByteArrayOutputStream();
			gzip = new GZIPInputStream(new ByteArrayInputStream(payload));
			try {
				byte[] buffer = new byte[2048];
				int len;
				while ((len = gzip.read(buffer)) >= 0) {
					output.write(buffer, 0, len);
				}
				gzip.close();
			} catch (IOException ioe) {
				LOG.error("Error Extracting Content from Request");
				throw new Exception(String.format("Zip Extract Exception  At request time: %.3f, request.getAssocReqResp() is %s. The content may be corrupted.", request.getTimeStamp(), logFlag?"N/A":request.getAssocReqResp()));
			}
		} else {
			return payload;
		}

		if (output.size() > 0) {
			return output.toByteArray();
		} else {
			request.setExtractable(false);
			return new byte[0];
		}
	}

	/**
	 * Determines whether the same content is contained in this request/response as in the specified request/response
	 * 
	 * @param right
	 *                  The request to compare to
	 * @return true if the content is the same
	 */
	public boolean isSameContent(HttpRequestResponseInfo left, HttpRequestResponseInfo right, Session session, Session sessionRight) {
		// Check specified content length
		if (left.getContentLength() > 0 && left.getContentLength() != right.getContentLength()) {
			return false;
		}
		boolean yes = true;
		long leftcount = getActualByteCount(left, session);
		long rightcount = getActualByteCount(right, sessionRight);
		if (leftcount == rightcount) {

			if (leftcount == 0) {
				return true;
			}

			// Otherwise do byte by byte compare
			byte[] bufferLeft = left.getPayloadData().toByteArray();
			byte[] bufferRight = right.getPayloadData().toByteArray();

			Iterator<Map.Entry<Integer, Integer>> itleft = left.getContentOffsetLength().entrySet().iterator();
			Iterator<Map.Entry<Integer, Integer>> itright = right.getContentOffsetLength().entrySet().iterator();
			int indexLeft = 0;
			int stopLeft = 0;
			int indexRight = 0;
			int stopRight = 0;
			if (itleft.hasNext() && itright.hasNext()) {
				Map.Entry<Integer, Integer> entryLeft = itleft.next();
				Map.Entry<Integer, Integer> entryRight = itright.next();
				indexLeft = entryLeft.getKey();
				stopLeft = indexLeft + entryLeft.getValue();
				indexRight = entryRight.getKey();
				stopRight = entryRight.getValue();
				do {
					if (bufferLeft[indexLeft] != bufferRight[indexRight]) {
						return false;
					}
					++indexLeft;
					++indexRight;
					if (indexLeft >= bufferLeft.length || indexRight >= bufferRight.length) {
						break;
					}
					if (indexLeft >= stopLeft) {
						if (itleft.hasNext()) {
							entryLeft = itleft.next();
							indexLeft = entryLeft.getKey();
							stopLeft = indexLeft + entryLeft.getValue();
						} else {
							break;
						}
					}
					if (indexRight >= stopRight) {
						if (itright.hasNext()) {
							entryRight = itright.next();
							indexRight = entryRight.getKey();
							stopRight = entryRight.getValue();
						} else {
							break;
						}
					}
				} while (true);
			}
			yes = true;
		} else {
			yes = false;
		}
		return yes;
	}

	/**
	 * Gets the number of bytes in the request/response body. The actual byte count.
	 * 
	 * @return The total number of bytes in the request/response body. If contentOffsetLength is null, then this method returns 0.
	 */
	public long getActualByteCount(HttpRequestResponseInfo item, Session session) {
		if (item.getContentOffsetLength() != null) {

			byte[] buffer = getStorageBuffer(item, session);
			int bufferSize = buffer != null ? buffer.length : 0;

			long result = 0;
			for (Map.Entry<Integer, Integer> entry : item.getContentOffsetLength().entrySet()) {
				int start = entry.getKey();
				int size = entry.getValue();
				if (bufferSize < start + size) {

					// Only include what was actually downloaded.
					size = bufferSize - start;
				}
				result += size;
			}
			return result;
		} else {
			return 0;
		}
	}
	
	/**
	 * Convenience method that gets the storage array in the session where this request/ response is located.
	 * 
	 * @return
	 */
	private byte[] getStorageBuffer(HttpRequestResponseInfo req, Session session) {
		switch (req.getPacketDirection()) {
		case DOWNLINK:
			return session.getStorageDl();
		case UPLINK:
			return session.getStorageUl();
		default:
			return null;
		}

	}
}
