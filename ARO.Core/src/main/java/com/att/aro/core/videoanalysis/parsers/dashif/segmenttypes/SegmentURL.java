package com.att.aro.core.videoanalysis.parsers.dashif.segmenttypes;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;


/**
 * Refer https://standards.iso.org/ittf/PubliclyAvailableStandards/MPEG-DASH_schema_files/DASH-MPD.xsd
 * @author arpitbansal (ab090c)
 *
 */
@Data
@XmlRootElement(name = "SegmentURL")
@XmlAccessorType(XmlAccessType.FIELD)
public class SegmentURL {
    @XmlAttribute(name="media") private String media;
    @XmlAttribute(name="mediaRange") private String mediaRange;
    @XmlAttribute(name="index") private String index;
    @XmlAttribute(name="indexRange") private String indexRange;
}
