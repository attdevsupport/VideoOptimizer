/*
 *  Copyright 2014 AT&T
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
package com.att.arotcpcollector.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DateUtils {
    private static final String TAG = DateUtils.class.getSimpleName();


    public static String getDate(String dateFormat, long currenttimemillis) {
        return new SimpleDateFormat(dateFormat, Locale.getDefault())
                .format(currenttimemillis);
    }

    /**
     * @deprecated
     * After the project migrate to gradle, this method is not working properly
     */
    @Deprecated
    public static long getBuildDate(Context context)  {

        try {
            ApplicationInfo ai = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), 0);
            try (ZipFile zf = new ZipFile(ai.sourceDir)){
                ZipEntry ze = zf.getEntry("classes.dex");
                long time = ze.getTime();
                zf.close();
                return time;
            }
        } catch (IOException | PackageManager.NameNotFoundException e) {
            Log.i(TAG,"Error while fetching the build timestamp." , e);
        }
        return 0l;
    }
}
