/*
 *  Copyright 2017 AT&T
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
package com.att.aro.core.cloud;

import java.util.List;


@SuppressWarnings("PMD")
public abstract class Repository {
	protected String uri;
	protected String id;
	protected String userName;
	protected String password;

	/**
	 * Uploads the trace from disk to cloud repository.
	 * 
	 * @param trace
	 *            Absolute path of the trace on disk
	 */
	public abstract void put(String trace);

	/**
	 * Downloads the trace from a cloud repository to local folder.
	 * 
	 * @param from
	 *            Trace location on cloud repository
	 * @return Location of the retrieved file on disk.
	 */
	public abstract String get(String from, String to);

	/**
	 * Lists all the traces available in the cloud repository.
	 * 
	 * @return List of available trace files
	 */
	public abstract List<String> list();

}
