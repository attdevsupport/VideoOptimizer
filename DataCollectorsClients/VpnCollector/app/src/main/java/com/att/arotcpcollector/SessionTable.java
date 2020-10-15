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

import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Bharath.
 */

class SessionTable implements Map<String, Session>{
    private static final String TAG = "SessionTable";
    private static final long IDLE_THRESHOLD = 10 * 1000; //ms

    private final int limit;
    private final Hashtable<String, Session> sessionTable = new Hashtable<>();
    private final Map<Channel, Session> channelBasedSessionTable = new ConcurrentHashMap<>();

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

	    DatagramChannel channel = value.getUdpChannel();
        if (channel != null && channel.isConnected()){
	        channelBasedSessionTable.put(channel,value);
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
        if(oldSession != null && (System.currentTimeMillis() - oldSession.getLastAccessed()) > IDLE_THRESHOLD) {
            Log.i(TAG, "Eviction initiated:" + oldSession.getSessionKey());
            SessionManager.getInstance().closeSession(oldSession);
        } else {
            Log.e(TAG, "Error evicting entry: " + ((oldSession != null) ? oldSession.getSessionKey() : "Null Session"));
        }
    }

    @Override
    public void putAll(@NonNull Map<? extends String, ? extends Session> map) {
        throw new UnsupportedOperationException("This operation is not supported");
    }

    @Override
    public Session remove(Object key) {
    	Session value = sessionTable.remove(key);
	    DatagramChannel channel = value.getUdpChannel();
	    if (channel != null){
	    	channelBasedSessionTable.remove(channel);
	    }
        return value;
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

	public Session getSessionByChannel(Channel channel) {
    	return channelBasedSessionTable.get(channel);
	}
}
