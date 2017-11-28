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

package com.att.aro.core.cloud.aws;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.att.aro.core.cloud.Repository;

public class AwsRepository extends Repository {
	private static final Logger LOGGER = Logger.getLogger(AwsRepository.class.getName());

	private AmazonS3 s3Client = null;
	private String bucketName = null;

	public AwsRepository(Map<AWSInfoCredentials, String> credentials, ClientConfiguration config) {
		if (credentials != null && credentials.size() >= 4) {
			constructRepo(credentials.get(AWSInfoCredentials.AccessID), credentials.get(AWSInfoCredentials.SecretKey),
					credentials.get(AWSInfoCredentials.Region), credentials.get(AWSInfoCredentials.BucketName),config);
		}
	}

	public AwsRepository(String accessId, String secretKey, String region, String bucketName,ClientConfiguration config) {
		constructRepo(accessId, secretKey, region, bucketName,config);
	}

	private void constructRepo(String accessId, String secretKey, String region, String bucketName, ClientConfiguration config) {
		System.setProperty("java.net.useSystemProxies", "true");
		if (isNotBlank(accessId) && isNotBlank(secretKey) && isNotBlank(region) && isNotBlank(bucketName)) {
			AWSCredentials creds = new BasicAWSCredentials(accessId, secretKey);
			 
			try {
				Regions regions = Regions.fromName(region);
				this.bucketName = bucketName;
				s3Client = AmazonS3ClientBuilder.standard()
						.withCredentials(new AWSStaticCredentialsProvider(creds))
						.withRegion(regions)
						.withClientConfiguration(config)
						.build();
			} catch (IllegalArgumentException e) {
				LOGGER.error("Region value parameter is wrong");
			} catch (Exception e) {
				LOGGER.error(e.getMessage());
			}
		}
	}

	@Override
	public void put(String tracePath) {
		if (s3Client != null) {
			try {
				File file = new File(tracePath);
				s3Client.putObject(new PutObjectRequest(bucketName, file.getName(), file));
			}catch (AmazonServiceException ase) {
				LOGGER.error(ase.getMessage(), ase);
			}catch (Exception exception) {
				LOGGER.error(exception.getMessage(), exception);
			}
		}
	}

	@Override
	public String get(String remotePath, String localPath) {
		String downloadedFilePath = "";
		if (s3Client != null && (localPath != null && localPath.length() > 0)
				&& (remotePath != null && remotePath.length() > 0)) {
			downloadedFilePath = localPath + "/" + remotePath;
			try {
				S3Object object = s3Client.getObject(new GetObjectRequest(bucketName, remotePath));
				S3ObjectInputStream s3is = object.getObjectContent();
				FileOutputStream fos = new FileOutputStream(new File(downloadedFilePath));
				byte[] readBuf = new byte[1024];
				int readLen = 0;
				while ((readLen = s3is.read(readBuf)) > 0) {
					fos.write(readBuf, 0, readLen);
				}
				s3is.close();
				fos.close();
			} catch (AmazonServiceException | IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		return downloadedFilePath;
	}

	
	public List<S3ObjectSummary> getlist() {
		List<S3ObjectSummary> objects = null;
		if (s3Client != null) {
			try {
				ObjectListing objectListing = s3Client.listObjects(bucketName);
				objects = objectListing.getObjectSummaries();
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
			return objects;
		}
		return objects;
	}

	public boolean isAuthenticated() {
		return s3Client != null;
	}

	@Override
	public List<String> list() {
		// Really required???
		return null;
	}
}
