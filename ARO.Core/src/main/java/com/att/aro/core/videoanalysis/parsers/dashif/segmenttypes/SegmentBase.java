package com.att.aro.core.videoanalysis.parsers.dashif.segmenttypes;

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
@XmlRootElement(name = "SegmentBase")
@XmlAccessorType(XmlAccessType.FIELD)
public class SegmentBase {
    @XmlAttribute(name="indexRange") private String indexRange;
    @XmlAttribute(name="indexRangeExact") private Boolean indexRangeExact;
    @XmlAttribute(name="timescale") private Integer timescale;

    @XmlElement(name="Initialization") private Initialization initialization;
}
