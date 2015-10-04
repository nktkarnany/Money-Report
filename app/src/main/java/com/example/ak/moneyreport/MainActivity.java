package com.example.ak.moneyreport;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private Context context = this;

    private List<Sms> smsList = new ArrayList<>();

    private String amt, type, balance;
    private int bal;

    MyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button readSms = (Button) findViewById(R.id.readInbox);
        RecyclerView report = (RecyclerView) findViewById(R.id.report);

        report.setHasFixedSize(true);
        report.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyAdapter(smsList, context);
        report.setAdapter(adapter);

        readSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readMessages();
            }
        });

    }

    private void readMessages() {

        Sms sms;
        Uri uri = Uri.parse("content://sms/draft");
        String filter = null;

        String pattern = "([r][s]|[i][n][r])(\\s*.\\s*\\d*)";
        Pattern regex = Pattern.compile(pattern);

        if (smsList.size() - 1 >= 0) {
            String lastDate = smsList.get(0).getMsgDate();
            filter = "date>" + lastDate;
        }

        Cursor c = getContentResolver().query(uri, new String[]{"date", "body"}, filter, null, null);
        startManagingCursor(c);
        int total = c.getCount();

        if (c.moveToLast()) {
            for (int i = 0; i < total; i++) {
                sms = new Sms();

                String body = c.getString(c.getColumnIndexOrThrow("body"));
                String date = c.getString(c.getColumnIndexOrThrow("date"));

                sms.setMsgDate(date);
                body = body.toLowerCase();

                if ((body.contains("debit") || body.contains("withdraw")) && body.contains("bal")) {
                    sms.setMsgType("dr");
                    int flag = 0;
                    Matcher m1 = regex.matcher(body);
                    if (m1.find()) {
                        try {
                            String amt = m1.group(0);
                            amt = amt.replace("rs", "");
                            amt = amt.replace("inr", "");
                            amt = amt.replace(".", "");
                            amt = amt.replace(" ", "");
                            amt = amt.replace(",", "");
                            sms.setMsgAmt(amt);
                            flag = 1;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        c.moveToPrevious();
                        continue;
                    }

                    if (flag == 1) {
                        body = body.replaceFirst(pattern, "");
                        Matcher m2 = regex.matcher(body);
                        if (m2.find()) {
                            try {
                                String amt = (m2.group(0));
                                amt = amt.replace("rs", "");
                                amt = amt.replace("inr", "");
                                amt = amt.replace(".", "");
                                amt = amt.replace(" ", "");
                                amt = amt.replace(",", "");
                                sms.setMsgBal(amt);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            c.moveToPrevious();
                            continue;
                        }
                    }

                } else if (body.contains("credit") && body.contains("bal")) {
                    sms.setMsgType("cr");
                    int flag = 0;
                    Matcher m1 = regex.matcher(body);
                    if (m1.find()) {
                        try {
                            String amt = (m1.group(0));
                            amt = amt.replace("rs", "");
                            amt = amt.replace("inr", "");
                            amt = amt.replace(".", "");
                            amt = amt.replace(" ", "");
                            amt = amt.replace(",", "");
                            sms.setMsgAmt(amt);
                            flag = 1;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        c.moveToPrevious();
                        continue;
                    }
                    if (flag == 1) {
                        body = body.replaceFirst(pattern, "");
                        Matcher m2 = regex.matcher(body);
                        if (m2.find()) {
                            try {
                                String amt = (m2.group(0));
                                amt = amt.replace("rs", "");
                                amt = amt.replace("inr", "");
                                amt = amt.replace(".", "");
                                amt = amt.replace(" ", "");
                                amt = amt.replace(",", "");
                                sms.setMsgBal(amt);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            c.moveToPrevious();
                            continue;
                        }
                    }
                }
                smsList.add(0, sms);
                adapter.notifyItemInserted(0);
                c.moveToPrevious();
            }
        } else {
            Toast.makeText(MainActivity.this, "No new sms to read!!", Toast.LENGTH_SHORT).show();
        }
        c.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                final Dialog dialog = new Dialog(context);
                dialog.setContentView(R.layout.add_dialog);
                dialog.setTitle("Add Transaction");

                // radio button implementation for debit or credit
                final RadioGroup rg = (RadioGroup) dialog.findViewById(R.id.radioGroup);

                final EditText addAmt = (EditText) dialog.findViewById(R.id.addAmt);
                Button save = (Button) dialog.findViewById(R.id.save);

                dialog.show();

                // set onclick listener to add button in the custom dialog box to add transaction
                save.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        readMessages();
                        int index = smsList.size() - 1;
                        amt = addAmt.getText().toString();
                        int selected = rg.getCheckedRadioButtonId();
                        try {
                            if (selected == R.id.debit) {
                                type = "dr";
                                // if id is 0 that means its the first receipt so balance is equal to the amount
                                if (index >= 0) {
                                    // if not the first receipt get balance since last receipt and do appropriate operation
                                    balance = smsList.get(0).getMsgBal();
                                    bal = Integer.parseInt(balance) - Integer.parseInt(amt);
                                    balance = Integer.toString(bal);
                                } else {
                                    // negative in case of debit
                                    balance = "-" + amt;
                                }
                            } else if (selected == R.id.credit) {
                                type = "cr";
                                if (index >= 0) {
                                    balance = smsList.get(0).getMsgBal();
                                    bal = Integer.parseInt(balance) + Integer.parseInt(amt);
                                    balance = Integer.toString(bal);
                                } else {
                                    // positive in case of credit
                                    balance = amt;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            long time = System.currentTimeMillis();
                            Toast.makeText(MainActivity.this, "Transaction Added", Toast.LENGTH_SHORT).show();
                            smsList.add(0, new Sms(type, amt, Long.toString(time), balance));
                            adapter.notifyItemInserted(0);
                            dialog.dismiss();
                        }
                    }
                });
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
