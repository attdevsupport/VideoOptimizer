package com.att.aro.datacollector.ioscollector.attenuator;

import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class CertDLServerWrapper implements Runnable{
	private static final Logger LOG = LogManager.getLogger(CertDLServerWrapper.class.getName());
	private CertDLServer certServer;
	
	@Override
	public void run() {
		launchCertDLServer();
	}
	
	private void launchCertDLServer() {
		certServer = new CertDLServer(9091);
 		try {
			certServer.start();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	public void stop() {
		if(certServer!=null) {
			LOG.info("stop cert server");
			certServer.stop();
		}
	}

}
