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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.amazonaws.AmazonClientException;
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
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.Transfer.TransferState;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.att.aro.core.cloud.Repository;

/**
 * Amazon S3 Repository management
 * TransferManager and client objects may pool connections and threads. 
 * Reuse TransferManager and client objects and share them throughout applications.
 */
public class AwsRepository extends Repository {
	private static final Logger LOGGER = LogManager.getLogger(AwsRepository.class.getName());

	private AmazonS3 s3Client = null;
	private TransferManager transferMgr;
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
			try {
				AWSCredentials creds = new BasicAWSCredentials(accessId, secretKey);
				Regions regions = Regions.fromName(region);
				this.bucketName = bucketName;
				s3Client = AmazonS3ClientBuilder.standard()
						.withCredentials(new AWSStaticCredentialsProvider(creds))
						.withRegion(regions)
						.withClientConfiguration(config)
						.build();
				transferMgr = TransferManagerBuilder.standard()
			            .withS3Client(s3Client)
			            .build();
			} catch (IllegalArgumentException ille) {
				LOGGER.error(ille.getMessage(),ille);
			} catch (Exception exp) {
				LOGGER.error(exp.getMessage(),exp);
			}
		}
	}	
	
	@Override
	public TransferState put(File file) {
		try {
			PutObjectRequest req = new PutObjectRequest(bucketName, file.getName(), file);
			Upload myUpload = transferMgr.upload(req);
			myUpload.waitForCompletion();
			transferMgr.shutdownNow();
			return myUpload.getState();
		}catch (AmazonServiceException ase) {
			LOGGER.error("Error Message:  " + ase.getMessage());
  		}catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
 		}
		return null;
	}

	@Override
	public String get(String remotePath, String localPath) {
		String downloadedFilePath = "";
		if (s3Client != null && (localPath != null && localPath.length() > 0)
				&& (remotePath != null && remotePath.length() > 0)) {
			downloadedFilePath = localPath + "/" + remotePath;
			try {
 				File downloadTarget = new File(downloadedFilePath);
				Download myDownload = transferMgr.download(new GetObjectRequest(bucketName, remotePath), downloadTarget);
				myDownload.waitForCompletion();
				transferMgr.shutdownNow();
			} catch (AmazonClientException  ace) {
				LOGGER.error("Error Message:    " + ace.getMessage());
			} catch (InterruptedException ire) {
				LOGGER.error(ire.getMessage());
			} catch (Exception exception) {
				LOGGER.error(exception.getMessage(), exception);
	 		}
		}
		return downloadedFilePath;
	}

	public List<S3ObjectSummary> getlist() {
		List<S3ObjectSummary> objects = null;
		if (s3Client != null) {
			ObjectListing objectListing = null;
			try {
				objectListing = s3Client.listObjects(bucketName);
				objects = objectListing.getObjectSummaries();
				for (S3ObjectSummary objectSummary : objects) {
					LOGGER.debug(" - " + objectSummary.getKey() + "  " + "(size = " + objectSummary.getSize() + ")");
				}
			} catch (Exception exc) {
				LOGGER.error("Error Message: " + exc.getMessage());
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

	@Override
	public void put(String trace) {
		// TODO Auto-generated method stub		
	}
}
