package com.att.aro.ui.model.listener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.export.ExcelCell;
import com.att.aro.core.export.ExcelSheet;
import com.att.aro.core.export.ExcelWriter;
import com.att.aro.core.export.style.ColorStyle;
import com.att.aro.core.export.style.ExcelCellStyle;
import com.att.aro.core.export.style.FontStyle;
import com.att.aro.core.packetanalysis.impl.TimeRangeAnalysis;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.core.packetanalysis.pojo.Statistic;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.pojo.AROTraceData;
import com.google.common.collect.Lists;


public class BestPracticeResultsListener extends AbstractMenuItemListener {
    private static final Logger LOG = LoggerFactory.getLogger(BestPracticeResultsListener.class);

    private AROTraceData traceData;

    public BestPracticeResultsListener(AROTraceData traceData, List<FileNameExtensionFilter> fileNameExtensionFilters, int defaultExtensionFilterIndex) {
        super(null, fileNameExtensionFilters, defaultExtensionFilterIndex);
        super.tableName = "best_practices_results";
        this.traceData = traceData;
    }

    @Override
    public void writeExcel(File file) throws IOException {
        LOG.info("Start export data to {}", file.getAbsolutePath());

        // Write to excel file
        String sheetName = table == null || table.getName() == null ? "Sheet1" : table.getName();
        ExcelSheet sheet = new ExcelSheet(sheetName, new ArrayList<>(), getTraceRows());
        ExcelWriter excelWriter = new ExcelWriter(file.getAbsolutePath());
        excelWriter.export(Lists.newArrayList(sheet));

        LOG.info("Finish export data to {}", file.getAbsolutePath());
    }

    private List<List<Object>> getTraceRows() {
        List<List<Object>> dataRows = new ArrayList<>();

        AbstractTraceResult traceResults = traceData.getAnalyzerResult().getTraceresult();
        if (traceResults != null) {
            dataRows.add(Arrays.asList(getFontStyledColumn("Trace Date"), traceResults.getTraceDateTime().toString()));

            Path path = Paths.get(traceResults.getTraceDirectory());
            Path fileName = path.getFileName();
            Path directoryPath = path.getParent();
            dataRows.add(Arrays.asList(getFontStyledColumn("Trace Name"), fileName == null ? "" : fileName.toString()));
            dataRows.add(Arrays.asList(getFontStyledColumn("Trace Path"), directoryPath == null ? "" : directoryPath.toString()));

            // If it is from rooted collector and load from trace folder
            boolean isSecureTrace = false;
            if(TraceResultType.TRACE_DIRECTORY.equals(traceResults.getTraceResultType())){
                TraceDirectoryResult traceDirResult = (TraceDirectoryResult) traceResults;
                isSecureTrace = traceDirResult.isSecureTrace();

                String deviceModelInfo = traceDirResult.getDeviceMake() != null && traceDirResult.getDeviceModel() != null
                                             ? traceDirResult.getDeviceMake() + " / " + traceDirResult.getDeviceModel()
                                             : "Not Available";
                dataRows.add(Arrays.asList(getFontStyledColumn("Device Make/Model"), deviceModelInfo));

                String deviceOSInfo = traceDirResult.getOsType() != null && traceDirResult.getOsVersion() != null
                                          ? traceDirResult.getOsType()+ " / " +traceDirResult.getOsVersion()
                                          : "Android";
                dataRows.add(Arrays.asList(getFontStyledColumn("Device OS"), deviceOSInfo));

                dataRows.add(Arrays.asList(getFontStyledColumn("Network Type(s)"), traceDirResult.getNetworkTypesList()));
            }

            dataRows.add(Arrays.asList(getFontStyledColumn("Secure Trace"), isSecureTrace ? "Yes" : "No"));
        }

        addTimeRangeAnalysisAndStatisticRows(dataRows);
        dataRows.addAll(getBPSummaryRows());
        dataRows.addAll(getBpRows());

        return dataRows;
    }

    private void addTimeRangeAnalysisAndStatisticRows(List<List<Object>> dataRows) {
    	if (traceData.getAnalyzerResult() != null) {
    		TimeRangeAnalysis timeRangeAnalysis = traceData.getAnalyzerResult().getTimeRangeAnalysis();

    		// Calculate time range
    		if (timeRangeAnalysis == null) {
	    		AnalysisFilter filter = traceData.getAnalyzerResult().getFilter();
	    		double beginTime = 0.0d;
	    		double endTime = 0.0d;
	    		boolean readyForAnalysis = false;
	
	    		if (filter != null && filter.getTimeRange() != null) {
	    			beginTime = filter.getTimeRange().getBeginTime();
	    			endTime = filter.getTimeRange().getEndTime();
	    			readyForAnalysis = true;
	    		} else if (traceData.getAnalyzerResult().getTraceresult() != null){
	    			endTime = traceData.getAnalyzerResult().getTraceresult().getTraceDuration();
	    			readyForAnalysis = true;
	    		}
	    		
	    		if (readyForAnalysis) {
	    			timeRangeAnalysis = new TimeRangeAnalysis(beginTime, endTime, traceData.getAnalyzerResult());
	    		}
    		}

    		if (timeRangeAnalysis != null) {
    			double traceDuration = timeRangeAnalysis.getEndTime() - timeRangeAnalysis.getStartTime();
    			long traceDurationLong = (long) traceDuration;
                dataRows.add(Arrays.asList(getFontStyledColumn("Duration (second)"), formatToFixedDecimal(3, traceDuration)));
                dataRows.add(Arrays.asList(getFontStyledColumn("Duration (hh:mm:ss.sss)"), 
                		String.format("%02d:%02d:%02d", traceDurationLong/3600, (traceDurationLong%3600) / 60, traceDurationLong % 60) +
                		String.format("%.3f", traceDuration - Math.floor(traceDuration)).substring(1)
        		));
    			dataRows.add(Arrays.asList(getFontStyledColumn("Start Time (second)"), formatToFixedDecimal(3, timeRangeAnalysis.getStartTime())));
    			dataRows.add(Arrays.asList(getFontStyledColumn("End Time (second)"), formatToFixedDecimal(3, timeRangeAnalysis.getEndTime())));
    		}
    		
    		Statistic statistic = traceData.getAnalyzerResult().getStatistic();
    		if (statistic != null) {
                dataRows.add(Arrays.asList(getFontStyledColumn("Total Data (KB)"), formatToFixedDecimal(2, statistic.getTotalByte() / 1000.0)));
                dataRows.add(Arrays.asList(getFontStyledColumn("Total Payload Data (KB)"), formatToFixedDecimal(2, statistic.getTotalPayloadBytes() / 1000.0)));
                dataRows.add(Arrays.asList(getFontStyledColumn("HTTPS Data (KB)"), formatToFixedDecimal(2, statistic.getTotalHTTPSByte() / 1000.0)));
                dataRows.add(Arrays.asList(getFontStyledColumn("Unanalyzed HTTPS Data (KB)"), formatToFixedDecimal(2, statistic.getTotalHTTPSBytesNotAnalyzed() / 1000.0)));
            }

    		if (timeRangeAnalysis != null) {
    			dataRows.add(Arrays.asList(getFontStyledColumn("Average Throughput (kbps)"), formatToFixedDecimal(2, timeRangeAnalysis.getAverageThroughput())));
    			dataRows.add(Arrays.asList(getFontStyledColumn("Total Upload Data (KB)"), formatToFixedDecimal(2, timeRangeAnalysis.getUplinkBytes() / 1000.0)));
    			dataRows.add(Arrays.asList(getFontStyledColumn("Average Upload Throughput (kbps)"), formatToFixedDecimal(2, timeRangeAnalysis.getAverageUplinkThroughput())));
    			dataRows.add(Arrays.asList(getFontStyledColumn("Total Download Data (KB)"), formatToFixedDecimal(2, timeRangeAnalysis.getDownlinkBytes() / 1000.0)));
    			dataRows.add(Arrays.asList(getFontStyledColumn("Average Download Throughput (kbps)"), formatToFixedDecimal(2, timeRangeAnalysis.getAverageDownlinkThroughput())));
    		}

    		if (statistic != null) {
    			dataRows.add(Arrays.asList(getFontStyledColumn("Total Energy Consumed (J)"), formatToFixedDecimal(2, traceData.getAnalyzerResult().getEnergyModel().getTotalEnergyConsumed())));
    		}
    	}
    }

    private Double formatToFixedDecimal(int precision, double number) {
		return Precision.round(number, precision);
    }

    private List<List<Object>> getBPSummaryRows() {

        int pass = 0;
        int fail = 0;
        int warning = 0;
        int selftest = 0;
        int configure = 0;
        int nodata = 0;
        for (AbstractBestPracticeResult result : traceData.getBestPracticeResults()) {
            switch (result.getResultType()) {
                case PASS:
                    ++pass;
                    break;
                case FAIL:
                    ++fail;
                    break;
                case WARNING:
                    ++warning;
                    break;
                case SELF_TEST:
                    ++selftest;
                    break;
                case CONFIG_REQUIRED:
                    ++configure;
                    break;
                case NO_DATA:
                    ++nodata;
                    break;
                default:
                    break;
            }
        }

        List<List<Object>> data = new ArrayList<>();
        data.add(Arrays.asList(getFontStyledColumn(BPResultType.PASS.getDescription()), pass));
        data.add(Arrays.asList(getFontStyledColumn(BPResultType.FAIL.getDescription()), fail));
        data.add(Arrays.asList(getFontStyledColumn(BPResultType.WARNING.getDescription()), warning));
        data.add(Arrays.asList(getFontStyledColumn(BPResultType.SELF_TEST.getDescription()), selftest));
        data.add(Arrays.asList(getFontStyledColumn(BPResultType.CONFIG_REQUIRED.getDescription()), configure));
        data.add(Arrays.asList(getFontStyledColumn(BPResultType.NO_DATA.getDescription()), nodata));

        return data;
    }

    // gets all bp rows - name, result (pass/fail/warning/selftest)
    private List<List<Object>> getBpRows() {
    	
    	List<List<Object>> dataRows = new ArrayList<>();
        
        for (AbstractBestPracticeResult bestPracticeResult : traceData.getBestPracticeResults()) {
        	if (bestPracticeResult.getResultType() == BPResultType.NONE) {
				continue;
			}
        	dataRows.add(Arrays.asList(getFontStyledColumn(bestPracticeResult.getBestPracticeType().getDescription()), getColorStyledColumn(bestPracticeResult)));
        }
        
        return dataRows;
    }

    private ExcelCell getFontStyledColumn(String content) {
        return new ExcelCell(FontStyle.DEFAULT_BOLD, content);
    }

    private ExcelCell getColorStyledColumn(AbstractBestPracticeResult bestPracticeResult) {
        ExcelCellStyle style;

        switch (bestPracticeResult.getResultType()) {
            case PASS:
                style = ColorStyle.LIME;
                break;
            case FAIL:
                style = ColorStyle.CORAL;
                break;
            case WARNING:
                style = ColorStyle.YELLOW;
                break;
            case SELF_TEST:
                style = ColorStyle.SKY_BLUE;
                break;
            case CONFIG_REQUIRED:
                style = ColorStyle.LIGHT_GREY;
                break;
            case NO_DATA:
                style = ColorStyle.LIGHT_TURQUOISE;
                break;
            default:
                style = ColorStyle.RED;
        }

        return new ExcelCell(style, bestPracticeResult.getResultExcelText());
    }

    @Override
    public void writeCSV(File file) {
        String text = "Writing to CSV file is not supported by " + this.getClass().getName();
        LOG.error(text);
        throw new UnsupportedOperationException(text);
    }

}
