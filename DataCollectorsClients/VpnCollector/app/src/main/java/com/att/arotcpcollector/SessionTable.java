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

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * Created by Bharath.
 */

class SessionTable implements Map<String, Session>{
    private static final String TAG = "SessionTable";

    private final int limit;
    private final Hashtable<String, Session> sessionTable = new Hashtable<>();

    public SessionTable(int limit) {
        this.limit = limit;
    }

    @Override
    public void clear() {
        sessionTable.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return sessionTable.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return sessionTable.containsValue(value);
    }

    @NonNull
    @Override
    public Set<Entry<String, Session>> entrySet() {
        return sessionTable.entrySet();
    }

    @Override
    public Session get(Object key) {
        return sessionTable.get(key);
    }

    @Override
    public boolean isEmpty() {
        return sessionTable.isEmpty();
    }

    @NonNull
    @Override
    public Set<String> keySet() {
        return sessionTable.keySet();
    }

    @Override
    public synchronized Session put(String key, Session value) {
        if(sessionTable.size() >= limit) {
            evictEntry();
        }
        return sessionTable.put(key, value);
    }

    private void evictEntry() {
        long oldest = Long.MAX_VALUE;
        Session oldSession = null;
        for(Entry<String, Session> se : sessionTable.entrySet()) {
            Session session = se.getValue();
            if(oldest > session.getLastAccessed()) {
                oldest = session.getLastAccessed();
                oldSession = session;
            }
        }
        if(oldSession != null) {
            Log.i(TAG, "Eviction initiated:" + oldSession.getSessionKey());
            SessionManager.getInstance().closeSession(oldSession);
        } else {
            Log.e(TAG, "Error evicting entry");
        }
    }

    @Override
    public void putAll(@NonNull Map<? extends String, ? extends Session> map) {
        throw new UnsupportedOperationException("This operation is not supported");
    }

    @Override
    public Session remove(Object key) {
        return sessionTable.remove(key);
    }

    @Override
    public int size() {
        return sessionTable.size();
    }

    @NonNull
    @Override
    public Collection<Session> values() {
        return sessionTable.values();
    }
}
