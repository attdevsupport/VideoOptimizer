package com.att.aro.core.videouploadanalysis;

import java.io.File;
import java.io.IOException;

import javax.ws.rs.POST;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.apache.commons.lang.StringUtils;

import com.att.aro.core.datacollector.pojo.StatusResult;
import com.att.aro.core.pojo.ErrorCodeRegistry;
import com.att.aro.core.settings.impl.SettingsImpl;
import com.att.aro.core.util.IResultSubscriber;

public class FileSubmit implements Runnable{

	private static final Logger LOG = LogManager.getLogger(FileSubmit.class.getName());
	private IResultSubscriber subscriber;
	private String url;
	private File file1;
	private File file2;
	private boolean runState;
	private boolean useAuthentication;

	public void init(IResultSubscriber subscriber, String url, File file1, File file2, boolean useAuthentication) {
		this.url = url;
		this.file1 = file1;
		this.file2 = file2;
		this.useAuthentication = useAuthentication;
		setSubscriber(subscriber);
	}
	
	@Override
	public void run() {
		runState = true;
		LOG.info("Start Sending...");
		StatusResult statusResult = sendPost(url, file1, file2);

		LOG.info("Forward Status to subscriber");
		LOG.info(String.format("forward results \"%s\" \"%s\"to :%s", statusResult.isSuccess(), statusResult.getData(), subscriber.getClass().getSimpleName()));
		String result = statusResult.getData().toString();
		if (StringUtils.isEmpty(result)) {
			statusResult.setSuccess(false);
			result = "Status: " + statusResult.getStatus() + ". Reason: " + statusResult.getStatusInfo();  
		}
		subscriber.receiveResults(this.getClass(), statusResult.isSuccess(), result);
		LOG.info("Finished Sending...");
		LOG.info("Results : Success:" + statusResult.isSuccess() + ", Response:" + statusResult.getData());
		runState = false;
	}

	public boolean isRunning() {
		return runState;
	}

	@POST
	public StatusResult sendPost(String url, File file1, File file2) {

		StatusResult statusResult = new StatusResult();

		LOG.info("URL:" + url);
		LOG.info("Send POST multipart");
		LOG.info(String.format("part1:%s", file1));
		LOG.info(String.format("part2:%s", file2));
		LOG.info("Building Client...");
		
		final Client client = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();
		LOG.info("fin: ClientBuilder.newBuilder().register(MultiPartFeature.class).build()");	
		client.property(ClientProperties.REQUEST_ENTITY_PROCESSING, "CHUNKED");
		client.property(ClientProperties.CHUNKED_ENCODING_SIZE, 1024);
	    
		if(useAuthentication) {
			String userName = SettingsImpl.getInstance().getAttribute("user.name");
			String userPassword = SettingsImpl.getInstance().getAttribute("user.password");
			if(StringUtils.isNotEmpty(userName) && StringUtils.isNotEmpty(userPassword)) {
				HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(userName, userPassword);
				client.register(feature);
			}
		}
		FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
		
		FileDataBodyPart part1 = new FileDataBodyPart("file", file1, MediaType.APPLICATION_OCTET_STREAM_TYPE);
		FileDataBodyPart part2 = new FileDataBodyPart("file", file2, MediaType.APPLICATION_JSON_TYPE);
		LOG.info(String.format("File Information: FileDataBodyPart(%s, %s)", file1.getName(), file2.getName()));
		formDataMultiPart.bodyPart(part1).bodyPart(part2).setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
		
		WebTarget target = client.target(url);
		long timeStart = System.currentTimeMillis();
		LOG.info("Initiate POST to " + url);
		
		if (isRunning()) {

			Entity<FormDataMultiPart> theEntity = Entity.entity(formDataMultiPart, formDataMultiPart.getMediaType());
			Response response = null;
			long timeEnd;
			try {
				response = target.request().post(theEntity);

				timeEnd = System.currentTimeMillis();
				LOG.debug("post finished :" + response.getStatus());
				LOG.debug("POST process time :" + ((double) (timeEnd - timeStart)) / 1000);
				statusResult.setSuccess(true);
				statusResult.setData("elapsed time: " + ((double) (timeEnd - timeStart) / 1000));
				statusResult.setStatus(response.getStatus());
				statusResult.setStatusInfo(response.getStatusInfo().getReasonPhrase());
				statusResult.setData(response.readEntity(String.class));

			} catch (Exception e) {
				timeEnd = System.currentTimeMillis();
				LOG.error(String.format("target.request().post(%s) Exception :%s", theEntity.toString(), e.getMessage()));
				LOG.error("target.request().post(theEntity) Exception :" + e.getMessage());
				LOG.error("POST process time :" + ((double) (timeEnd - timeStart)) / 1000);
				statusResult.setSuccess(false);
				statusResult.setData(e);
				statusResult.setError(ErrorCodeRegistry.getPostError(e.getMessage()));
				return statusResult;

			} finally {
				try {
					LOG.debug("closing the multipart - formDataMultiPart.close()");
					formDataMultiPart.close();
				} catch (IOException e) {
					LOG.error("Error :" + e.getMessage());
				}
			}
			// Use response object to verify upload success
			if (response != null) {
				LOG.info("RESULTS :" + response.toString());
				LOG.info("RESULTS DATA :" + statusResult.getData().toString());
			}

			LOG.debug("Total time :" + timeEnd + " - " + timeStart + " = " + ((double) (timeEnd - timeStart) / 1000));
		}
		return statusResult;
	}

	public void setSubscriber(IResultSubscriber subscriber) {
		this.subscriber = subscriber;
	}

	public void setStop() {
		runState = false;
	}
}
