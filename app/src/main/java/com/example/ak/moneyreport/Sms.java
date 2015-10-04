package com.example.ak.moneyreport;

public class Sms {
    private String msgBody;
    private String msgDate;

    public Sms() {
    }

    public Sms(String msgBody, String msgDate) {
        this.msgBody = msgBody;
        this.msgDate = msgDate;
    }

    public String getMsgBody() {
        return this.msgBody;
    }

    public String getMsgDate() {
        return this.msgDate;
    }

    public void setMsgBody(String msgBody) {
        this.msgBody = msgBody;
    }

    public void setMsgDate(String msgDate) {
        this.msgDate = msgDate;
    }
}