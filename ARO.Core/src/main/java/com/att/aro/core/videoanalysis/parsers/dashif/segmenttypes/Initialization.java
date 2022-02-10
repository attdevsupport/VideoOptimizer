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
@XmlRootElement(name = "Initialization")
@XmlAccessorType(XmlAccessType.FIELD)
public class Initialization {
    @XmlAttribute(name="range") private String range;
    @XmlAttribute(name="sourceURL") private String sourceURL;
}
