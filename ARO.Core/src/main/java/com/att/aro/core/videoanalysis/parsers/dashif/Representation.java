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
package com.att.aro.core.videoanalysis.parsers.dashif;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.att.aro.core.videoanalysis.parsers.dashif.segmenttypes.SegmentBase;
import com.att.aro.core.videoanalysis.parsers.dashif.segmenttypes.SegmentList;
import com.att.aro.core.videoanalysis.parsers.dashif.segmenttypes.SegmentTemplate;

import lombok.Data;


/**
 * Refer https://standards.iso.org/ittf/PubliclyAvailableStandards/MPEG-DASH_schema_files/DASH-MPD.xsd
 * @author arpitbansal (ab090c)
 *
 */
@Data
@XmlRootElement(name = "Representation")
@XmlAccessorType(XmlAccessType.FIELD)
public class Representation {
    @XmlAttribute(name="bandwidth") private Integer bandwidth;
    @XmlAttribute(name="width") private Integer width;
    @XmlAttribute(name="height") private Integer height;
    @XmlAttribute(name="codecs") private String codecs;
    @XmlAttribute(name="mimeType") private String mimeType;

    @XmlElement(name="AudioChannelConfiguration") private AudioChannelConfiguration audioChannelConfiguration;
    @XmlElement(name="BaseURL") private String baseURL;
    @XmlElement(name="SegmentBase") private SegmentBase segmentBase;
    @XmlElement(name="SegmentList") private SegmentList segmentList;
    @XmlElement(name="SegmentTemplate") private SegmentTemplate segmentTemplate;
}
