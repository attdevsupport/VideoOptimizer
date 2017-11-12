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
package com.att.arotcpcollector;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Bharath.
 */
class SessionTable extends ConcurrentHashMap<String, Session> {
    private static final String TAG = "SessionTable";

    private final int limit;

    SessionTable(int limit) {
        this.limit = limit;
    }

    @Override
    public Session put(@NonNull String key, @NonNull Session value) {
        if(size() >= limit) {
            evictEntry();
        }

        return super.put(key, value);
    }

    private void evictEntry() {
        long oldest = Long.MAX_VALUE;
        Session oldSession = null;

        for(final Session session: values()) {
            if(oldest > session.getLastAccessed()) {
                oldest = session.getLastAccessed();
                oldSession = session;
            }
        }

        if(oldSession != null) {
            Log.i(TAG, "Eviction initiated: " + oldSession.getSessionKey());
            SessionManager.getInstance().closeSession(oldSession);
        } else {
            Log.e(TAG, "Error evicting entry");
        }
    }
}

