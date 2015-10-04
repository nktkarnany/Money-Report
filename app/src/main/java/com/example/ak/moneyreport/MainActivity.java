package com.example.ak.moneyreport;

import android.app.Activity;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private Context context = this;

    private Button readInbox;
    private RecyclerView graph;

    private List<Sms> smsList = new ArrayList<>();

    MyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        readInbox = (Button) findViewById(R.id.readInbox);
        graph = (RecyclerView) findViewById(R.id.graph);

        graph.setHasFixedSize(true);
        graph.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyAdapter(smsList, context);
        graph.setAdapter(adapter);

        readInbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sms sms;
                Uri message = Uri.parse("content://sms/draft");

                Cursor c = getContentResolver().query(message, null, null, null, null);
                startManagingCursor(c);
                int total = c.getCount();

                if (c.moveToFirst()) {
                    for (int i = 0; i < total; i++) {
                        sms = new Sms();
                        String body = c.getString(c.getColumnIndexOrThrow("body"));
                        String date = c.getString(c.getColumnIndexOrThrow("date"));

                        body = body.toLowerCase();

                        if ((body.contains("debit") || body.contains("withdraw")) && body.contains("bal")) {
                            sms.setMsgDate("dr");
                            Pattern regex = Pattern.compile("([r][s]|[i][n][r])(\\s*.\\s*\\d*)");
                            Matcher m = regex.matcher(body);
                            if (m.find()) {
                                try {
                                    Log.e("match yes", m.group(0));
                                    String amt = (m.group(0));
                                    amt = amt.replace("rs", "");
                                    amt = amt.replace("inr", "");
                                    amt = amt.replace(".", "");
                                    amt = amt.replace(" ", "");
                                    amt = amt.replace(",", "");
                                    Log.e("match saved", amt);
                                    sms.setMsgBody(amt);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            } else {
                                Log.e("match no", "No matched value");
                                c.moveToNext();
                                continue;
                            }
                        } else if (body.contains("credit") && body.contains("bal")) {
                            int flag = 0;
                            Pattern regex = Pattern.compile("([r][s]|[i][n][r])(\\s*.\\s*\\d*)");
                            Matcher m1 = regex.matcher(body);
                            if (m1.find()) {
                                try {
                                    Log.e("match yes1", m1.group(0));
                                    String amt = (m1.group(0));
                                    amt = amt.replace("rs", "");
                                    amt = amt.replace("inr", "");
                                    amt = amt.replace(".", "");
                                    amt = amt.replace(" ", "");
                                    amt = amt.replace(",", "");
                                    Log.e("match saved1", amt);
                                    sms.setMsgBody(amt);
                                    flag = 1;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            } else {
                                Log.e("match no", "No matched value");
                                c.moveToNext();
                                continue;
                            }
                            if (flag == 1) {
                                body = body.replaceFirst("([r][s]|[i][n][r])(\\s*.\\s*\\d*)", "");
                                Matcher m2 = regex.matcher(body);
                                if (m2.find()) {
                                    try {
                                        Log.e("match yes2", m2.group(0));
                                        String amt = (m2.group(0));
                                        amt = amt.replace("rs", "");
                                        amt = amt.replace("inr", "");
                                        amt = amt.replace(".", "");
                                        amt = amt.replace(" ", "");
                                        amt = amt.replace(",", "");
                                        Log.e("match saved2", amt);
                                        sms.setMsgDate(amt);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                } else {
                                    Log.e("match no", "No matched value");
                                    c.moveToNext();
                                    continue;
                                }
                            }
                        }

                        smsList.add(sms);
                        c.moveToNext();
                    }

                } else {
                    throw new RuntimeException("No sms to read!!");
                }
                c.close();
                adapter.notifyDataSetChanged();
            }
        });

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

                final EditText addAmt = (EditText) dialog.findViewById(R.id.addAmt);

                // radio button implementation for debit or credit
                RadioGroup rg = (RadioGroup) dialog.findViewById(R.id.radioGroup);
                final int selected = rg.getCheckedRadioButtonId();

                Button save = (Button) dialog.findViewById(R.id.save);
                dialog.show();

                // set onclick listener to add button in the custom dialog box to add transaction
                save.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
