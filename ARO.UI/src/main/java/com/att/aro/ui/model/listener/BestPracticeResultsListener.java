/*
 *  Copyright 2021 AT&T
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
package com.att.aro.ui.model.listener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

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
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.Statistic;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.preferences.UserPreferencesFactory;
import com.att.aro.core.util.Util;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.google.common.collect.Lists;


public class BestPracticeResultsListener extends AbstractMenuItemListener {
    private static final Logger LOG = LoggerFactory.getLogger(BestPracticeResultsListener.class);

    private AROTraceData traceData;

    public BestPracticeResultsListener(AROTraceData traceData) {
        super(null);
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

    @Override
    protected JFileChooser getDefaultFileChooser(File file) {
        JFileChooser chooser;
        if (file != null) {
            chooser = new JFileChooser(file);
        } else {
            String defaultFilePath = "";
            AbstractTraceResult traceResults = traceData.getAnalyzerResult().getTraceresult();
            if (traceResults != null && traceResults.getTraceDirectory() != null) {
                defaultFilePath = traceResults.getTraceDirectory();
                Path fileName = Paths.get(defaultFilePath).getFileName();
                defaultFilePath += Util.FILE_SEPARATOR + (fileName == null ? "" : fileName.toString());
            } else {
                defaultFilePath = UserPreferencesFactory.getInstance().create().getTracePath() + Util.FILE_SEPARATOR + "best_practice_results";
            }

            chooser = new JFileChooser();
            chooser.setSelectedFile(new File(defaultFilePath));
        }

        // Set allowed file extensions
        FileNameExtensionFilter xlsxFilter = new FileNameExtensionFilter(ResourceBundleHelper.getMessageString("fileChooser.desc.excel"),
                ResourceBundleHelper.getMessageString("fileChooser.contentType.xls"), ResourceBundleHelper.getMessageString("fileChooser.contentType.xlsx"));

        chooser.setDialogTitle(ResourceBundleHelper.getMessageString("fileChooser.Title"));
        chooser.addChoosableFileFilter(xlsxFilter);
        chooser.setFileFilter(xlsxFilter);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setApproveButtonText(ResourceBundleHelper.getMessageString("fileChooser.Save"));
        chooser.setMultiSelectionEnabled(false);

        return chooser;
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

            dataRows.add(Arrays.asList(getFontStyledColumn("Duration (min)"), formatToFixedDecimal(traceResults.getTraceDuration()/60)));
        }

        Statistic statistic = traceData.getAnalyzerResult().getStatistic();
        if(statistic != null){
            dataRows.add(Arrays.asList(getFontStyledColumn("Total Data (KB)"), formatToFixedDecimal(statistic.getTotalByte() / 1000.0)));
            dataRows.add(Arrays.asList(getFontStyledColumn("Total Payload Data (KB)"), formatToFixedDecimal(statistic.getTotalPayloadBytes() / 1000.0)));
            dataRows.add(Arrays.asList(getFontStyledColumn("HTTPS Data (KB)"), formatToFixedDecimal(statistic.getTotalHTTPSByte() / 1000.0)));
            dataRows.add(Arrays.asList(getFontStyledColumn("Unanalyzed HTTPS Data (KB)"), formatToFixedDecimal(statistic.getTotalHTTPSBytesNotAnalyzed() / 1000.0)));
            dataRows.add(Arrays.asList(getFontStyledColumn("Energy Consumed (J)"), formatToFixedDecimal(traceData.getAnalyzerResult().getEnergyModel().getTotalEnergyConsumed())));
        }

        dataRows.addAll(getBPSummaryRows());
        dataRows.addAll(getBpRows());

        return dataRows;
    }

    private Double formatToFixedDecimal(double num) {
        return Double.valueOf(String.format("%.2f", num));
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
