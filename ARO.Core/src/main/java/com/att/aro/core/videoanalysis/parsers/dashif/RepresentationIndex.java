package com.att.aro.core.videoanalysis.parsers.dashif;

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
@XmlRootElement(name = "RepresentationIndex")
@XmlAccessorType(XmlAccessType.FIELD)
public class RepresentationIndex {
    @XmlAttribute(name="sourceURL") private String sourceURL;
    @XmlAttribute(name="range") private String range;
}
