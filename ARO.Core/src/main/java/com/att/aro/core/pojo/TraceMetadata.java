package com.att.aro.core.pojo;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;

enum TraceType {
	IDLE, SEARCH, BROWSING, STREAMING, UNKNOWN;
}

enum Orientation {
	PORTRAIT, LANDSCAPE
}

public class TraceMetadata {
	private static final String UNKNOWN = "Unknown";
	private PacketAnalyzerResult analyzerResult;
	private String description = "<Placeholder>";
	private TraceType traceType = TraceType.UNKNOWN;
	private Orientation deviceOrientation = Orientation.PORTRAIT;

	// private String phoneModel;
	// private String os;
	// private String osVersion;
	private String networkCarrier = UNKNOWN;
	private String targetedApp = UNKNOWN;
	private String applicationProducer = UNKNOWN;

	private ZonedDateTime startUTC;
	private ZonedDateTime endUTC;

	private String traceSource = "MANUAL";
	private String traceOwner = UNKNOWN;
	private long lifetimeSec = 0;

	public TraceMetadata(PacketAnalyzerResult analyzerResult) {
		this.analyzerResult = analyzerResult;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTraceType() {
		return traceType.name().toLowerCase();
	}

	public void setTraceType(TraceType traceType) {
		this.traceType = traceType;
	}

	public String getDeviceOrientation() {
		return deviceOrientation.name();
	}

	public void setDeviceOrientation(Orientation deviceOrientation) {
		this.deviceOrientation = deviceOrientation;
	}

	public String getPhoneModel() {
		boolean directoryPresent = analyzerResult != null && analyzerResult.getTraceresult() != null
				&& (analyzerResult.getTraceresult() instanceof TraceDirectoryResult);
		return directoryPresent ? ((TraceDirectoryResult) analyzerResult.getTraceresult()).getDeviceModel() : "UNKNOWN";
	}

	public String getOs() {
		boolean directoryPresent = analyzerResult != null && analyzerResult.getTraceresult() != null
				&& (analyzerResult.getTraceresult() instanceof TraceDirectoryResult);
		return directoryPresent ? ((TraceDirectoryResult) analyzerResult.getTraceresult()).getOsType() : "UNKNOWN";
	}

	public String getOsVersion() {
		boolean directoryPresent = analyzerResult != null && analyzerResult.getTraceresult() != null
				&& (analyzerResult.getTraceresult() instanceof TraceDirectoryResult);
		return directoryPresent ? ((TraceDirectoryResult) analyzerResult.getTraceresult()).getOsVersion() : "UNKNOWN";
	}

	public String getNetworkCarrier() {
		return networkCarrier;
	}

	public void setNetworkCarrier(String networkCarrier) {
		this.networkCarrier = networkCarrier;
	}

	public String getTargetedApp() {
		return targetedApp;
	}

	public void setTargetedApp(String targetedApp) {
		this.targetedApp = targetedApp;
	}

	public String getApplicationProducer() {
		return applicationProducer;
	}

	public void setApplicationProducer(String applicationProducer) {
		this.applicationProducer = applicationProducer;
	}

	public String getStartUTC() {
		if (startUTC == null) {
			boolean isNotNull = analyzerResult != null && analyzerResult.getTraceresult() != null
					&& analyzerResult.getTraceresult().getTraceDateTime() != null;
			startUTC = isNotNull
					? ZonedDateTime.ofInstant(analyzerResult.getTraceresult().getTraceDateTime().toInstant(),
							ZoneId.systemDefault())
					: null;
		}
		return startUTC != null ? startUTC.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : "";
	}

	public void setStartUTC(ZonedDateTime startUTC) {
		this.startUTC = startUTC;
	}

	public String getEndUTC() {
		if (endUTC == null) {
			boolean isNotNull = analyzerResult != null && analyzerResult.getTraceresult() != null;
			endUTC = isNotNull
					? startUTC
							.plusSeconds((long)analyzerResult.getTraceresult().getTraceDuration())
					: null;
		}
		return endUTC != null ? endUTC.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : "";
	}

	public void setEndUTC(ZonedDateTime endUTC) {
		this.endUTC = endUTC;
	}

	public String getTraceSource() {
		return traceSource;
	}

	public void setTraceSource(String traceSource) {
		this.traceSource = traceSource;
	}

	public String getTraceOwner() {
		return traceOwner;
	}

	public void setTraceOwner(String traceOwner) {
		this.traceOwner = traceOwner;
	}

	public long getLifetimeSec() {
		return lifetimeSec;
	}

	public void setLifetimeSec(long lifetimeSec) {
		this.lifetimeSec = lifetimeSec;
	}
}
