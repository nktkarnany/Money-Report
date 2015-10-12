package com.example.ak.moneyreport;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ReportItem {

    private String reportType;
    private String reportAmt;
    private Long longTime;

    public ReportItem() {

    }

    public ReportItem(String reportType, String reportAmt, Long longTime) {
        this.reportType = reportType;
        this.reportAmt = reportAmt;
        this.longTime = longTime;
    }

    public String getReportType() {
        return this.reportType;
    }

    public String getReportAmt() {
        return this.reportAmt;
    }

    public String getReportTime() {
        return new SimpleDateFormat("HH:mm").format(new Date(this.longTime));
    }

    public String getReportDate() {
        return new SimpleDateFormat("d/MMM/yyyy").format(new Date(this.longTime));
    }
}
