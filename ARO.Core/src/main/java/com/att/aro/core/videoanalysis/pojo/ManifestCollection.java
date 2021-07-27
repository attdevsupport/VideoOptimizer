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
package com.att.aro.core.videoanalysis.pojo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.videoanalysis.impl.SegmentInfo;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Data
public class ManifestCollection {
	private static final Logger LOG = LogManager.getLogger(ManifestCollection.class.getName());
	private Manifest manifest;
	
	@Setter(AccessLevel.NONE)
	private int minimumSequenceStart = 0;
	
	private int commonBaseLength = -1;
	private int segmentFileNameLength;
	private boolean segmentOrder;
	private int segmentCounter = 0;

	private int childUriNameSectionCount;
	
	/**<pre>
	 * Trie of SegmentInfo
	 * Key: segmentUriName
	 */
	@NonNull
	@Setter
	private PatriciaTrie<SegmentInfo> segmentTrie = new PatriciaTrie<>();
	
	public PatriciaTrie<SegmentInfo> getSegmentTrie() {
		return segmentTrie;
	}

	/**<pre>
	 * Trie of ChildManifest
	 * Key: segmentUriName
	 */
	@NonNull
	@Setter
	private PatriciaTrie<ChildManifest> segmentChildManifestTrie = new PatriciaTrie<>();

	@Getter
	@NonNull
	private List<ChildManifest> segmentChildManifestListInOrder = new ArrayList<>();

	/**<pre>
	 * TreeMap of ChildManifest
	 * Key: childUriName
	 */
	@Setter(AccessLevel.NONE)
	@NonNull
	private PatriciaTrie<ChildManifest> uriNameChildMap = new PatriciaTrie<>();
	
	/**<pre>
	 * TreeMap of ChildManifest
	 * Key: (Double)timestamp
	 */
	@Setter(AccessLevel.NONE)
	@NonNull
	@Getter
	private TreeMap<Double, ChildManifest> timestampChildManifestMap = new TreeMap<>(); 
	
	/**<pre>
	 * TreeMap of ChildManifest
	 * Key: (Double)bandwidth
	 */
	@Setter(AccessLevel.NONE)
	@NonNull
	@Getter
	private SortedMap<Double, ChildManifest> bandwidthMap = new TreeMap<>();
	
	public void addToUriNameChildMap(String childUriName, ChildManifest childManifest) {
		if (childUriName != null) {
			if (!uriNameChildMap.containsKey(childUriName)) {
				uriNameChildMap.put(childUriName, childManifest);
			} else {
				LOG.debug(childUriName + " already exists");
			}
		} else {
			LOG.error("invalid (null) key for childUriName" + childManifest);
		}
	}
	
	public ChildManifest getChildManifest(String childUriName) {
		return uriNameChildMap.get(childUriName);
	}
	
	public void addToTimestampChildManifestMap(Double timestamp, ChildManifest childManifest) {
		timestampChildManifestMap.put(timestamp, childManifest);
	}
	
	public void addToBandwidthMap(Double bandwidth, ChildManifest childManifest) {
		bandwidthMap.put(bandwidth, childManifest);
	}
	
	public void addToSegmentTrie(String segmentUriName, SegmentInfo segmentInfo) {
		segmentTrie.put(segmentUriName, segmentInfo);
	}

	public void addToSegmentChildManifestTrie(String segmentUriName, ChildManifest childManifest) {
		if (!segmentChildManifestTrie.containsKey(segmentUriName)) {
			segmentChildManifestTrie.put(segmentUriName, childManifest);
		}
		segmentChildManifestListInOrder.add(childManifest);

	}

	// debugging - logging info
	public String dumpTimeChildMap() {
		StringBuilder stblr = new StringBuilder("\n\t, timeChildMap<doubleTimestamp,ChildManifest>:");
		stblr.append("\tSize: " + timestampChildManifestMap.size());
		stblr.append("\n timestamps: ");
				
		Set<Entry<Double, ChildManifest>> eset = timestampChildManifestMap.entrySet();
		Iterator<Entry<Double, ChildManifest>> iset = eset.iterator();
		while (iset.hasNext()) {
			Entry<Double, ChildManifest> val = iset.next();
			stblr.append("\nKey :" + val.getKey());
			stblr.append("\t, " + val.getValue());
		}
		return stblr.toString();
	}

	public String dumpTrie(int skip) {
		int countDown = skip;
		StringBuilder stblr = new StringBuilder("segmentTrie<segmentUriName,SegmentInfo>:");
		stblr.append(segmentTrie.size());
		stblr.append(" entries");

		Set<Entry<String, SegmentInfo>> eset = segmentTrie.entrySet();
		Iterator<Entry<String, SegmentInfo>> iset = eset.iterator();
		while (iset.hasNext()) {
			Entry<String, SegmentInfo> val = iset.next();
			if (skip > 0 || (countDown-- > 0)) {
				break;
			}
			stblr.append("\nKey :" + val.getKey()).append("\t, " + val.getValue());
		}
		return stblr.toString();
	}

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("\nManifestCollection :");
		strblr.append("Manifest " + String.format("%.3f", manifest.getRequestTime()));
		strblr.append(dumpUriNameChildMap());
		strblr.append(dumpTrie(5));
		strblr.append(dumpTimeChildMap());
		return strblr.toString();
	}

	public String dumpUriNameChildMap() {
		StringBuilder strblr = new StringBuilder("\n\t, uriNameChildMap  :");
		strblr.append(uriNameChildMap.size());
		if (!uriNameChildMap.isEmpty()) {
			if (uriNameChildMap.size() > 0) {
				Iterator<String> keys = uriNameChildMap.keySet().iterator();
				while (keys.hasNext()) {
					String key = keys.next();
					strblr.append("\n\t\tchildMap_key: " + key);
					if (uriNameChildMap.get(key).getManifest() != null) {
						strblr.append("\n" + uriNameChildMap.get(key).dumpSegmentList());
					}
				}
			}
		}
		return strblr.toString();
	}

	public int getNextSegment() {
		return segmentCounter++;
	}
	
	public void setMinimumSequenceStart(int mediaSequence) {
		if (minimumSequenceStart == 0 || mediaSequence < minimumSequenceStart) {
			minimumSequenceStart = mediaSequence;
		}
	}
}
