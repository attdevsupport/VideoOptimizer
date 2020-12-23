/*
 *  Copyright 2020 AT&T
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
package com.att.aro.core.videoanalysis.parsers.dashif.segmenttypes;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;


/**
 * Refer https://standards.iso.org/ittf/PubliclyAvailableStandards/MPEG-DASH_schema_files/DASH-MPD.xsd
 * @author arpitbansal (ab090c)
 *
 */
@Data
@XmlRootElement(name = "SegmentList")
@XmlAccessorType(XmlAccessType.FIELD)
public class SegmentList {
    @XmlAttribute(name="indexRange") private String indexRange;
    @XmlAttribute(name="indexRangeExact") private Boolean indexRangeExact;
    @XmlAttribute(name="timescale") private Integer timescale;
    @XmlAttribute(name="duration") private Integer duration;

    @XmlElement(name="Initialization") private Initialization initialization;
    @XmlElement(name="SegmentURL") private List<SegmentURL> segmentURLs;
}
