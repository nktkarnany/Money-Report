package com.example.ak.moneyreport;

public class Sms {

    private String msgType;
    private String msgAmt;
    private String msgDate;
    private String msgBal;


    public Sms() {
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
}