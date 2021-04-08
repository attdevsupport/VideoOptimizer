/*
 *  Copyright 2014 AT&T
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.packetanalysis.ICacheAnalysis;
import com.att.aro.core.packetanalysis.IHttpRequestResponseHelper;
import com.att.aro.core.packetanalysis.pojo.CacheAnalysis;
import com.att.aro.core.packetanalysis.pojo.CacheEntry;
import com.att.aro.core.packetanalysis.pojo.CacheExpiration;
import com.att.aro.core.packetanalysis.pojo.Diagnosis;
import com.att.aro.core.packetanalysis.pojo.DuplicateEntry;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfoWithSession;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Range;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.util.GoogleAnalyticsUtil;

/**
 * Contains functionality that performs a cache analysis of the HTTP requests
 * and responses in a set of TCP session data, and encapsulates the resulting
 * cache analysis information. Date: November 21, 2014
 */
public class CacheAnalysisImpl implements ICacheAnalysis {
	@Autowired
	IHttpRequestResponseHelper rrhelper;
	private static final Logger LOG = LogManager.getLogger(CacheAnalysisImpl.class.getName());
	int itindex = 0;
	List<DuplicateEntry> duplicateEntries;
	@Value("${ga.request.timing.cacheAnalysisTimings.title}")
	private String cacheAnalysisTitle;
	@Value("${ga.request.timing.analysisCategory.title}")
	private String analysisCategory;

	@Override
	public CacheAnalysis analyze(List<Session> sessionlist) {
		long analysisStartTime = System.currentTimeMillis();
		CacheAnalysis result = new CacheAnalysis();
		long totalRequestResponseBytes = 0;
		long totalRequestResponseDupBytes = 0;
		double duplicateContentBytesRatio = 0.0;
		Map<String, CacheEntry> cacheEntries = new HashMap<String, CacheEntry>();
		Map<String, CacheEntry> dupEntries = new HashMap<String, CacheEntry>();
		Map<String, SortedSet<Range>> rangeEntries = new HashMap<String, SortedSet<Range>>();
		List<CacheEntry> diagnosisResults = new ArrayList<CacheEntry>();
		List<CacheEntry> duplicateContent = new ArrayList<CacheEntry>();
		List<CacheEntry> duplicateContentWithOriginals = new ArrayList<CacheEntry>();
		Map<CacheExpiration, List<CacheEntry>> cacheExpirationResponses = result.getCacheExpirationResponses();
		duplicateEntries = new ArrayList<DuplicateEntry>();
		// Initialize cache expiration lists
		for (CacheExpiration expiration : CacheExpiration.values()) {
			cacheExpirationResponses.put(expiration, new ArrayList<CacheEntry>());
		}
		// Build a sorted list of all of the HTTP request/response in the trace
		List<HttpRequestResponseInfoWithSession> rrInfo = new ArrayList<HttpRequestResponseInfoWithSession>();
		for (Session session : sessionlist) {
			if (!session.isUdpOnly()) {
				// rrInfo.addAll(session.getRequestResponseInfo());
				for (HttpRequestResponseInfo item : session.getRequestResponseInfo()) {
					HttpRequestResponseInfoWithSession itemsession = new HttpRequestResponseInfoWithSession();
					itemsession.setInfo(item);
					itemsession.setSession(session);
					rrInfo.add(itemsession);
				}
			}
		}
		Collections.sort(rrInfo);
		// Iterate through responses looking for duplicates
		for (HttpRequestResponseInfoWithSession httpreqres : rrInfo) {
			HttpRequestResponseInfo response = httpreqres.getInfo();
			Session session = httpreqres.getSession();
			PacketInfo firstPacket = session.getPackets().get(0);
			if (response.getDirection() == HttpDirection.REQUEST) {
				// We only want to process responses
				continue;
			}
			// Check whether response is valid
			int statusCode = response.getStatusCode();
			if (statusCode == 0) {
				diagnosisResults
						.add(new CacheEntry(null, response, Diagnosis.CACHING_DIAG_INVALID_RESPONSE, 0, firstPacket));
				continue;
			}
			if (statusCode != 200 && statusCode != 206) {
				diagnosisResults
						.add(new CacheEntry(null, response, Diagnosis.CACHING_DIAG_INVALID_REQUEST, 0, firstPacket));
				continue;
			}
			// [A] Find corresponding request
			HttpRequestResponseInfo request = response.getAssocReqResp();
			if (request == null) {
				diagnosisResults.add(
						new CacheEntry(request, response, Diagnosis.CACHING_DIAG_REQUEST_NOT_FOUND, 0, firstPacket));
				continue;
			}
			totalRequestResponseBytes += response.getContentLength();
			// Request must by GET, POST, or PUT
			String requestType = request.getRequestType();
			if (!HttpRequestResponseInfo.HTTP_GET.equals(requestType)
					&& !HttpRequestResponseInfo.HTTP_PUT.equals(requestType)
					&& !HttpRequestResponseInfo.HTTP_POST.equals(requestType)) {
				diagnosisResults
						.add(new CacheEntry(request, response, Diagnosis.CACHING_DIAG_INVALID_REQUEST, 0, firstPacket));
				continue;
			}
			// Check for valid object name and host name
			if (request.getHostName() == null || request.getObjName() == null) {
				diagnosisResults.add(
						new CacheEntry(request, response, Diagnosis.CACHING_DIAG_INVALID_OBJ_NAME, 0, firstPacket));
				continue;
			}
			// [B] Object cacheable?
			if (response.isNoStore() || request.isNoStore() || HttpRequestResponseInfo.HTTP_POST.equals(requestType)
					|| HttpRequestResponseInfo.HTTP_PUT.equals(requestType)) {
				cacheEntries.remove(getObjFullName(request, response));
				dupEntries.remove(getObjDuplicateName(request, response));
				diagnosisResults
						.add(new CacheEntry(request, response, Diagnosis.CACHING_DIAG_NOT_CACHABLE, 0, firstPacket));
				continue;
			}
			// [C] Does it hit the cache?
			CacheEntry cacheEntry = cacheEntries.get(getObjFullName(request, response));
			CacheEntry cacheDuplicateEntry = dupEntries.get(getObjDuplicateName(request, response));
			CacheEntry newCacheEntry;
			if (cacheEntry == null) {
				Diagnosis diagnosis = Diagnosis.CACHING_DIAG_CACHE_MISSED;
				newCacheEntry = new CacheEntry(request, response, diagnosis, firstPacket);
				newCacheEntry.setSession(session);
				addToCache(newCacheEntry, rangeEntries, cacheEntries, dupEntries, session);
				newCacheEntry.setCacheCount(1);
				diagnosisResults.add(newCacheEntry);
				if (cacheDuplicateEntry != null) {
					diagnosis = Diagnosis.CACHING_DIAG_ETAG_DUPLICATE;
				}
				duplicateEntries.add(new DuplicateEntry(request, response, diagnosis, firstPacket, session,
						getContent(response, session)));
				continue;
			} else {
				int oldCount = cacheEntry.getCacheCount();
				cacheEntry.setCacheCount(oldCount + 1);
			}
			CacheExpiration expStatus = cacheExpired(cacheEntry, request.getAbsTimeStamp());
			SortedSet<Range> ranges = rangeEntries
					.get(getObjFullName(cacheEntry.getRequest(), cacheEntry.getResponse()));
			boolean isfullcachehit = isFullCacheHit(response, ranges);
			if (isfullcachehit) {
				// [D] Is it expired?
				switch (expStatus) {
				case CACHE_EXPIRED:
				case CACHE_EXPIRED_HEURISTIC:
					newCacheEntry = handleCacheExpired(session, response, request, firstPacket, cacheEntry);
					diagnosisResults.add(newCacheEntry);
					break;
				case CACHE_NOT_EXPIRED:
				case CACHE_NOT_EXPIRED_HEURISTIC:
					newCacheEntry = new CacheEntry(request, response, Diagnosis.CACHING_DIAG_NOT_EXPIRED_DUP,
							firstPacket);
					duplicateEntries.add(new DuplicateEntry(request, response, Diagnosis.CACHING_DIAG_NOT_EXPIRED_DUP,
							firstPacket, session, getContent(response, session)));
					diagnosisResults.add(newCacheEntry);
					break;
				default:
					// Should not occur
					newCacheEntry = null;
				}
			} else {
				long bytesInCache = getBytesInCache(response, ranges);
				// [D] Is it expired?
				switch (expStatus) {
				case CACHE_EXPIRED:
				case CACHE_EXPIRED_HEURISTIC:
					newCacheEntry = handleCacheExpiredWithByteInCache(session, response, request, firstPacket,
							cacheEntry, bytesInCache);
					diagnosisResults.add(newCacheEntry);
					break;
				case CACHE_NOT_EXPIRED:
				case CACHE_NOT_EXPIRED_HEURISTIC:
					newCacheEntry = new CacheEntry(request, response, Diagnosis.CACHING_DIAG_NOT_EXPIRED_DUP_PARTIALHIT,
							bytesInCache, firstPacket);
					duplicateEntries.add(new DuplicateEntry(request, response, Diagnosis.CACHING_DIAG_NOT_EXPIRED_DUP,
							firstPacket, session, getContent(response, session)));
					diagnosisResults.add(newCacheEntry);
					break;
				default:
					// Should not occur
					newCacheEntry = null;
				}
			}
			cacheExpirationResponses.get(expStatus).add(newCacheEntry);
			if (newCacheEntry != null) {
				newCacheEntry.setCacheHit(cacheEntry);
			}
			// addToCache(newCacheEntry);
		} // END: Iterate through responses looking for duplicates
			// Get cache problems
		Set<CacheEntry> dupsWithOrig = new HashSet<CacheEntry>();
		Map<String, DuplicateEntry> duplicateEntriesMap = new HashMap<String, DuplicateEntry>();
		CacheEntry cache;
		for (DuplicateEntry dupEntry : duplicateEntries) {
			if (dupEntry.getContentLength() > 0) {
				String key = dupEntry.getRequest().getHostName() + dupEntry.getHttpObjectName()
						+ dupEntry.getContentLength();
				if (dupEntry.getHttpObjectName() != null) {
					if (!duplicateEntriesMap.containsKey(key)) {
						dupEntry.setCount(1);
						duplicateEntriesMap.put(key, dupEntry);
					} else {
						if (Arrays.equals(duplicateEntriesMap.get(key).getContent(), dupEntry.getContent())) {
							int count = duplicateEntriesMap.get(key).getCount();
							if (count == 1) {
								cache = new CacheEntry(duplicateEntriesMap.get(key).getRequest(),
										duplicateEntriesMap.get(key).getResponse(),
										duplicateEntriesMap.get(key).getDiagnosis(),
										duplicateEntriesMap.get(key).getSessionFirstPacket());
								cache.setSession(dupEntry.getSession());
								dupsWithOrig.add(cache);
							}
							cache = new CacheEntry(dupEntry.getRequest(), dupEntry.getResponse(),
									dupEntry.getDiagnosis(), dupEntry.getSessionFirstPacket());
							dupsWithOrig.add(cache);
							dupEntry = new DuplicateEntry(dupEntry.getRequest(), dupEntry.getResponse(),
									dupEntry.getDiagnosis(), dupEntry.getSessionFirstPacket(), dupEntry.getSession(),
									dupEntry.getContent());
							dupEntry.setCount(count + 1);
							duplicateEntriesMap.replace(key, dupEntry);
						}
					}
				}
			}
		}
		for (Entry<String, DuplicateEntry> cacheEntry2 : duplicateEntriesMap.entrySet()) {
			if (cacheEntry2.getValue().getCount() > 1) {
				int count = cacheEntry2.getValue().getCount();
				cache = new CacheEntry(cacheEntry2.getValue().getRequest(), cacheEntry2.getValue().getResponse(),
						cacheEntry2.getValue().getDiagnosis(), cacheEntry2.getValue().getSessionFirstPacket());
				cache.setHitCount(count);
				if (count > 2) {
					totalRequestResponseDupBytes += (cacheEntry2.getValue().getHttpRequestResponse().getContentLength()
							* (count - 1));
				} else {
					totalRequestResponseDupBytes += cacheEntry2.getValue().getHttpRequestResponse().getContentLength();
				}
				duplicateContent.add(cache);
			}
		}
		duplicateContentWithOriginals.addAll(dupsWithOrig);
		Collections.sort(duplicateContentWithOriginals);
		duplicateContentBytesRatio = totalRequestResponseBytes != 0
				? (double) totalRequestResponseDupBytes / totalRequestResponseBytes
				: 0.0;
		result.setCacheExpirationResponses(cacheExpirationResponses);
		result.setDiagnosisResults(diagnosisResults);
		result.setDuplicateContent(duplicateContent);
		result.setDuplicateContentBytesRatio(duplicateContentBytesRatio);
		result.setDuplicateContentWithOriginals(duplicateContentWithOriginals);
		result.setTotalRequestResponseBytes(totalRequestResponseBytes);
		result.setTotalRequestResponseDupBytes(totalRequestResponseDupBytes);
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsTimings(cacheAnalysisTitle,
				System.currentTimeMillis() - analysisStartTime, analysisCategory);
		return result;
	}

	CacheEntry handleCacheExpiredWithByteInCache(Session session, HttpRequestResponseInfo response,
			HttpRequestResponseInfo request, PacketInfo firstPacket, CacheEntry cacheEntry, long bytesInCache) {
		CacheEntry newCacheEntry = handleCacheExpiredCommon(session, response, request, firstPacket, cacheEntry);
		if (newCacheEntry == null) {
			if (request.isIfModifiedSince() || request.isIfNoneMatch()) {
				newCacheEntry = new CacheEntry(request, response,
						Diagnosis.CACHING_DIAG_OBJ_NOT_CHANGED_DUP_PARTIALHIT_SERVER, bytesInCache, firstPacket);
				duplicateEntries.add(new DuplicateEntry(request, response,
						Diagnosis.CACHING_DIAG_OBJ_NOT_CHANGED_DUP_PARTIALHIT_SERVER, firstPacket, session,
						getContent(response, session)));
			} else {
				newCacheEntry = new CacheEntry(request, response,
						Diagnosis.CACHING_DIAG_OBJ_NOT_CHANGED_DUP_PARTIALHIT_CLIENT, bytesInCache, firstPacket);
				duplicateEntries.add(new DuplicateEntry(request, response,
						Diagnosis.CACHING_DIAG_OBJ_NOT_CHANGED_DUP_PARTIALHIT_CLIENT, firstPacket, session,
						getContent(response, session)));
			}
		}
		return newCacheEntry;
	}

	CacheEntry handleCacheExpired(Session session, HttpRequestResponseInfo response, HttpRequestResponseInfo request,
			PacketInfo firstPacket, CacheEntry cacheEntry) {
		CacheEntry newCacheEntry = handleCacheExpiredCommon(session, response, request, firstPacket, cacheEntry);
		if (newCacheEntry == null) {
			if (request.isIfModifiedSince() || request.isIfNoneMatch()) {
				newCacheEntry = new CacheEntry(request, response, Diagnosis.CACHING_DIAG_OBJ_NOT_CHANGED_DUP_SERVER,
						firstPacket);
				duplicateEntries
						.add(new DuplicateEntry(request, response, Diagnosis.CACHING_DIAG_OBJ_NOT_CHANGED_DUP_SERVER,
								firstPacket, session, getContent(response, session)));
			} else {
				newCacheEntry = new CacheEntry(request, response, Diagnosis.CACHING_DIAG_OBJ_NOT_CHANGED_DUP_CLIENT,
						firstPacket);
				duplicateEntries
						.add(new DuplicateEntry(request, response, Diagnosis.CACHING_DIAG_OBJ_NOT_CHANGED_DUP_CLIENT,
								firstPacket, session, getContent(response, session)));
			}
		}
		return newCacheEntry;
	}

	CacheEntry handleCacheExpiredCommon(Session session, HttpRequestResponseInfo response,
			HttpRequestResponseInfo request, PacketInfo firstPacket, CacheEntry cacheEntry) {
		// Check to see if object changed
		HttpRequestResponseInfo cachedResponse = cacheEntry.getResponse();
		boolean isTheSame = rrhelper.isSameContent(response, cachedResponse, session, cacheEntry.getSession());
		CacheEntry newCacheEntry;
		if ((response.getLastModified() != null && cachedResponse.getLastModified() != null
				&& !response.getLastModified().equals(cachedResponse.getLastModified())) || !isTheSame) {
			newCacheEntry = new CacheEntry(request, response, Diagnosis.CACHING_DIAG_OBJ_CHANGED, firstPacket);
		} else if (response.getStatusCode() == 304) {
			newCacheEntry = new CacheEntry(request, response, Diagnosis.CACHING_DIAG_OBJ_NOT_CHANGED_304, firstPacket);
		} else {
			newCacheEntry = null;
		}
		return newCacheEntry;
	}

	/**
	 * Cache Expired status analysis.
	 *
	 * @param cacheEntry
	 * @param timestamp
	 * @return
	 */
	private CacheExpiration cacheExpired(CacheEntry cacheEntry, Date timestamp) {
		HttpRequestResponseInfo request = cacheEntry.getRequest();
		HttpRequestResponseInfo response = cacheEntry.getResponse();
		/*
		 * Cases when an object expires (t=time, s=server, c=client) (1) "no-cache"
		 * header in request/response (2) t >= s.expire (3) t >= c.date + c.max_age (4)
		 * t >= s.date + s.max_age (overrides 1) (5) s.age >= s.expire - s.date (6)
		 * s.age >= s.max_age (overrides 4) (7) s.age >= c.max_age
		 */
		if (request.isNoCache() || request.isPragmaNoCache() || response.isNoCache() || response.isPragmaNoCache()) {
			return CacheExpiration.CACHE_EXPIRED;
		} else if (response.getDate() != null && response.getMaxAge() != null && timestamp
				.getTime() > response.getAbsTimeStamp().getTime() + (response.getMaxAge().longValue() * 1000)) {
			return CacheExpiration.CACHE_EXPIRED;
		} else if (response.getExpires() != null && !timestamp.before(response.getExpires())) {
			return CacheExpiration.CACHE_EXPIRED;
		} else if (request.getDate() != null && request.getMaxAge() != null && timestamp
				.getTime() > request.getAbsTimeStamp().getTime() + (request.getMaxAge().longValue() * 1000)) {
			return CacheExpiration.CACHE_EXPIRED;
		} else if (response.getAge() != null && response.getMaxAge() != null
				&& response.getAge().longValue() > response.getMaxAge().longValue()) {
			return CacheExpiration.CACHE_EXPIRED;
		} else if (response.getAge() != null && response.getExpires() != null && response.getDate() != null
				&& (response.getAge().longValue() * 1000) >= response.getExpires().getTime()
						- response.getDate().getTime()) {
			return CacheExpiration.CACHE_EXPIRED;
		} else if (response.getAge() != null && request.getMaxAge() != null
				&& response.getAge().longValue() >= request.getMaxAge().longValue()) {
			return CacheExpiration.CACHE_EXPIRED;
		}
		// we don't consider s-maxage since the cache on the phone is a private
		// cache
		/*
		 * Cases when an object is not expired (1) t < s.expire (2) t < s.date +
		 * s.maxage
		 */
		if (response.getExpires() != null && timestamp.before(response.getExpires())) {
			return CacheExpiration.CACHE_NOT_EXPIRED;
		} else if (response.getDate() != null && response.getMaxAge() != null
				&& timestamp.getTime() < response.getDate().getTime() + (response.getMaxAge().longValue() * 1000)) {
			return CacheExpiration.CACHE_NOT_EXPIRED;
		}
		long oneDay = 86400000L;
		if (response.getDate() != null) {
			if (timestamp.getTime() < response.getDate().getTime() + oneDay) {
				return CacheExpiration.CACHE_NOT_EXPIRED_HEURISTIC;
			} else {
				return CacheExpiration.CACHE_EXPIRED_HEURISTIC;
			}
		}
		return CacheExpiration.CACHE_NOT_EXPIRED_HEURISTIC;
	}

	/**
	 * This method checks a response's byte range to see if it falls fully in the
	 * specified ranges to determine if the response was a full cache hit
	 *
	 * @param request
	 * @param response
	 * @param ranges
	 * @return true full cache found else false.
	 */
	private boolean isFullCacheHit(HttpRequestResponseInfo response, SortedSet<Range> ranges) {
		if (ranges != null) {
			for (Range range : ranges) {
				// Here we are looking at the numbers IN THE HEADER instead
				// of ON THE WIRE
				// We assume "Content-Range" in the RESPONSE header match
				// "Range" in the REQUEST
				if (response.getRangeFirst() >= range.getFirstByte() && response.getRangeLast() < range.getLastByte()) {
					return true;
				}
			}
			// Partial cache hit
			return false;
		}
		// the cache entry contains the entire object
		return true;
	}

	/**
	 * Cache contents calculated in bytes.
	 *
	 * @param request
	 * @param response
	 * @param ranges
	 * @return cache vaules in bytes
	 */
	private long getBytesInCache(HttpRequestResponseInfo response, SortedSet<Range> ranges) {
		long xferFirst = response.isRangeResponse() ? response.getRangeFirst() : 0;
		long xferLast = xferFirst + response.getContentLength();
		long cachedBytes = 0L;
		for (Range range : ranges) {
			cachedBytes += Math.max(0,
					Math.min(xferLast, range.getLastByte()) - Math.max(xferFirst, range.getFirstByte()));
		}
		return cachedBytes;
	}

	private String getObjFullName(HttpRequestResponseInfo request, HttpRequestResponseInfo response) {
		return request.getHostName() + "|" + request.getObjName() + "|" + response.getEtag() + "|"
				+ response.getContentLength();
	}

	private String getObjDuplicateName(HttpRequestResponseInfo request, HttpRequestResponseInfo response) {
		return request.getHostName() + "|" + request.getObjName() + "|" + response.getContentLength();
	}

	/**
	 * Adds the request and response in cacheEntries after the analysis.
	 *
	 * @param cacheEntry
	 * @param dupEntries
	 */
	private void addToCache(CacheEntry cacheEntry, Map<String, SortedSet<Range>> rangeEntries,
			Map<String, CacheEntry> cacheEntries, Map<String, CacheEntry> dupEntries, Session session) {
		HttpRequestResponseInfo response = cacheEntry.getResponse();
		Range range = null;
		long xferSize = calculatePartialTransfer(response, session);
		if (response.isRangeResponse()) {
			long first = response.getRangeFirst();
			long last = xferSize > 0 ? first + xferSize - 1 : response.getRangeLast();
			range = new Range(first, last + 1);
		} else if (xferSize > 0) {
			range = new Range(0, xferSize);
		}
		String objFullName = getObjFullName(cacheEntry.getRequest(), cacheEntry.getResponse());
		if (range != null) {
			SortedSet<Range> ranges = rangeEntries.get(objFullName);
			if (ranges != null) {
				ranges.add(range);
				Iterator<Range> iter = ranges.iterator();
				Range last = iter.next();
				while (iter.hasNext()) {
					Range curr = iter.next();
					if (curr.getFirstByte() >= last.getFirstByte() && curr.getFirstByte() <= last.getLastByte()) {
						last.setLastByte(Math.max(last.getLastByte(), curr.getLastByte()));
						iter.remove();
					}
				}
			} else {
				ranges = new TreeSet<Range>();
				ranges.add(range);
				rangeEntries.put(objFullName, ranges);
			}
		} else {
			rangeEntries.remove(objFullName);
		}
		cacheEntries.put(objFullName + cacheEntry.getContentLength(), cacheEntry);
		dupEntries.put(getObjDuplicateName(cacheEntry.getRequest(), cacheEntry.getResponse()), cacheEntry);
	}

	/**
	 * by cross checking the content length and the actual byte count partial
	 * transfer is calculated.
	 *
	 * @param request
	 * @param response
	 * @return -1 for partial transfers else actual response bytes.
	 */
	private long calculatePartialTransfer(HttpRequestResponseInfo response, Session session) {
		if (response.isChunked()) {
			return response.isChunkModeFinished() ? 0 : response.getContentLength();
		}
		// compute expectedBytes
		int expectedBytes;
		if (response.isRangeResponse()) {
			expectedBytes = response.getRangeLast() - response.getRangeFirst() + 1;
			if (expectedBytes <= 0) {
				expectedBytes = response.getContentLength();
			}
		} else {
			expectedBytes = response.getContentLength();
		}
		if (expectedBytes <= 0) {
			return -1;
		}
		// compute actualTransferred
		long actualBytes = rrhelper.getActualByteCount(response, session);
		if (actualBytes <= 0) {
			return -1;
		}
		if (actualBytes < (int) (expectedBytes * 0.9f)) {
			return actualBytes;
		} else {
			return -1;
		}
	}

	public byte[] getContent(HttpRequestResponseInfo response, Session session) {
		byte[] content = new byte[0];
		try {
			content = rrhelper.getContent(response, session);
		} catch (Exception e) {
			LOG.error("Error in retrieving Content: "+e.getMessage());
		}
		return content;
	}
}// end class
