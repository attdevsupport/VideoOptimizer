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
