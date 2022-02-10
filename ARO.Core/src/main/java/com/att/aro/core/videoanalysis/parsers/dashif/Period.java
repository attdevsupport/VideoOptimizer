package com.att.aro.core.videoanalysis.parsers.dashif;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.Duration;

import lombok.Data;


/**
 * Refer https://standards.iso.org/ittf/PubliclyAvailableStandards/MPEG-DASH_schema_files/DASH-MPD.xsd
 * @author arpitbansal (ab090c)
 *
 */
@Data
@XmlRootElement(name = "Period")
@XmlAccessorType(XmlAccessType.FIELD)
public class Period {
    @XmlAttribute(name="duration") private Duration duration;

    @XmlElement(name="AdaptationSet") private List<AdaptationSet> adaptationSet;
}
