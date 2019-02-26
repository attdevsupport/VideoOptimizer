package com.att.aro.datacollector.ioscollector.attenuator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.util.Util;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

/**
 * The class is light weight web server to host the user certificate generated
 * by Littleproxy. The user visit the server and download it to the device
 * starting secure collection.
 * 
 * @author ls661n
 *
 */
public class CertDLServer extends NanoHTTPD {
	private static final Logger LOG = LogManager.getLogger(CertDLServer.class.getName());
	private static final String USER_CERT = "VO.pem";

	public CertDLServer(int port) {
		super(port);
		LOG.info("About to start CertDLServer at port: " + port);
	}

	@Override
	public Response serve(IHTTPSession session) {

		File file = new File(Util.getVideoOptimizerLibrary() + Util.FILE_SEPARATOR + USER_CERT);
		if (file != null && file.exists()) {
			return myCustomResponse(file);
		} else {
			LOG.debug("File does not exist.");
		}
		return newFixedLengthResponse(
				"<html><body><h1>VO Couldn't start user certificate server</h1>\n</body></html>\n");
	}

	private Response myCustomResponse(File file) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			LOG.debug("File does not exist.");
			LOG.error(e.getMessage(), e);
			return newFixedLengthResponse(
					"<html><body><h1>VO Couldn't start user certificate server</h1>\n</body></html>\n");
		}
		Response response = newFixedLengthResponse(Status.OK, "application/x-pem-file", fis, file.length());
		response.addHeader("Content-Disposition", "attachment;filename=\"VO.pem\"");
		return response;
	}
}
