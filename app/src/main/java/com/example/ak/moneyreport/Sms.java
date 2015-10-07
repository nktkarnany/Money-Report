package com.example.ak.moneyreport;

import java.io.Serializable;

public class Sms implements Serializable {

    private String msgType;
    private String msgAmt;
    private String msgDate;
    private String formatDate;
    private String msgBal;
    private String msgAddress;


    public Sms() {
    }

    public Sms(String msgType, String msgAmt, String msgDate, String msgBal) {
        this.msgType = msgType;
        this.msgAmt = msgAmt;
        this.msgDate = msgDate;
        this.msgBal = msgBal;
    }

    public String getMsgType() {
        return this.msgType;
    }

    public String getMsgAmt() {
        return this.msgAmt;
    }

    public String getMsgDate() {
        return this.msgDate;
    }

    public String getMsgBal() {
        return this.msgBal;
    }

    public int getBalInt() {
        return Integer.parseInt(this.msgBal);
    }

    public String getMsgAddress() {
        return this.msgAddress = msgAddress;
    }

    public void setMsgAddress(String msgAddress) {
        this.msgAddress = msgAddress;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public void setMsgAmt(String msgAmt) {
        this.msgAmt = msgAmt;
    }

    public void setMsgDate(String msgDate) {
        this.msgDate = msgDate;
    }

    public void setMsgBal(String msgBal) {
        this.msgBal = msgBal;
    }

    public long getDateLong() {
        return Long.parseLong(this.msgDate);
    }

    public String getFormatDate() {
        return this.formatDate;
    }

    public void setFormatDate(String formatDate) {
        this.formatDate = formatDate;
    }
}