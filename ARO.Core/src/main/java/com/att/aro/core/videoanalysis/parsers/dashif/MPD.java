package com.att.aro.core.videoanalysis.parsers.dashif;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.Duration;

import com.att.aro.core.videoanalysis.parsers.MpdBase;

import lombok.Data;


/**
 * Refer https://standards.iso.org/ittf/PubliclyAvailableStandards/MPEG-DASH_schema_files/DASH-MPD.xsd
 * @author arpitbansal (ab090c)
 *
 */
@Data
@XmlRootElement(name="MPD", namespace="urn:mpeg:dash:schema:mpd:2011")
@XmlAccessorType(XmlAccessType.FIELD)
public class MPD implements MpdBase {
    @XmlAttribute(name = "id") 			                private String id;             
    @XmlAttribute(name = "profiles") 		            private String profiles;      
    @XmlAttribute(name = "type") 			            private String type;            
    @XmlAttribute(name = "minBufferTime")               private Duration minBufferTime;
	@XmlAttribute(name = "mediaPresentationDuration")	private String mediaPresentationDuration;
	@XmlAttribute(name = "minimumUpdatePeriod")			private String minimumUpdatePeriod;

    @XmlElement(name="Period") private List<Period> periods;

    @Override
    public String getMajorVersion() {
        return null;
    }

    @Override
    public int getSize() {
        return periods.size();
    }

    @Override
    public String getVideoName() {
        return null;
    }
}
