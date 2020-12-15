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
