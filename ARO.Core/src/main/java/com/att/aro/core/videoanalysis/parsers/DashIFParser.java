package com.att.aro.core.videoanalysis.parsers;

import java.util.List;

import org.springframework.util.CollectionUtils;

import com.att.aro.core.videoanalysis.parsers.dashif.AdaptationSet;
import com.att.aro.core.videoanalysis.parsers.dashif.MPD;
import com.att.aro.core.videoanalysis.parsers.dashif.Period;
import com.att.aro.core.videoanalysis.pojo.ChildManifest;
import com.att.aro.core.videoanalysis.pojo.Manifest;
import com.att.aro.core.videoanalysis.pojo.ManifestCollection;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;


public class DashIFParser extends DashParser {
    private final MPD mpd;

    public DashIFParser(VideoType videoType, MPD mpd, Manifest manifest, ManifestCollection manifestCollection, ChildManifest childManifest) {
        super(videoType, manifest, manifestCollection, childManifest);
        this.mpd = mpd;
    }

    /**
     * Returns the adaptation list associated with the first period in the manifest file.
     * Multiple periods need to be handled in future releases.
     * @return adaptation set's list
     */
    public List<AdaptationSet> getAdaptationSet() {
        List<Period> periods = mpd.getPeriods();
        return CollectionUtils.isEmpty(periods) ? null : periods.get(0).getAdaptationSet();
    }
}
