package com.example.ak.moneyreport;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
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

import org.achartengine.ChartFactory;
import org.achartengine.chart.BarChart;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private Context context = this;

    private List<Sms> smsList = new ArrayList<>();

    private String amt, type, balance;
    private int bal;

    private int length_debit = 0;
    private int length_online = 0;
    private int length_credit = 0;

    private String filename = "messages";
    private String TAG = "Log Debug";

    private String pattern = "(?:inr|rs)+[\\s]*+[0-9]*+[\\\\,]*+[0-9]*";

    MyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("Activity Lifecycle", "OnCreate Started");
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

        try {
            // file input stream is used to open a file for reading
            FileInputStream f = context.openFileInput(filename);
            // object input stream is used to read object from the file opened
            ObjectInputStream i = new ObjectInputStream(f);
            // object read is in the form of list<Sms> so iterate over the list to extract all Sms objects.
            for (Sms r : (List<Sms>) i.readObject()) {
                smsList.add(r);
                switch (r.getMsgType()) {
                    case "DR":
                        length_debit += 1;
                        break;
                    case "Net Bank":
                        length_online += 1;
                        break;
                    case "CR":
                        length_credit += 1;
                        break;
                }
            }
            i.close();
            f.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void readMessages() {

        Sms sms;
        Uri uri = Uri.parse("content://sms/inbox");
        String filter = null;


        if (smsList.size() > 0) {
            String lastDate = smsList.get(0).getMsgDate();
            filter = "date>" + lastDate;
        }

        Cursor c = getContentResolver().query(uri, new String[]{"date", "body"}, filter, null, null);
        int total = c.getCount();

        if (c.moveToLast()) {
            for (int i = 0; i < total; i++) {
                sms = new Sms();

                String date = c.getString(c.getColumnIndexOrThrow("date"));
                String body = c.getString(c.getColumnIndexOrThrow("body"));
                String t = "";

                sms.setMsgAddress(body);
                sms.setMsgDate(date);
                sms.setFormatDate(getDate(Long.parseLong(date)));

                body = body.toLowerCase();

                if (body.contains("debit") && body.contains("bal")) {
                    t = "DR";
                } else if (body.contains("online") && body.contains("payment") && !body.contains("bal")) {
                    t = "Net Bank";
                } else if (body.contains("credit") && (body.contains("net") || (body.contains("tot")) || (body.contains("total"))) && ((body.contains("avbl")) || body.contains("available")) && ((body.contains("bal")) || (body.contains("balance")))) {
                    t = "CR";
                }

                switch (t) {
                    case "DR":
                        sms.setMsgType(t);
                        if (getAmount(body) != null) {
                            sms.setMsgAmt(getAmount(body));
                        } else {
                            c.moveToPrevious();
                            continue;
                        }
                        body = body.replaceFirst(pattern, "");
                        if (getAmount(body) != null) {
                            sms.setMsgBal(getAmount(body));
                            smsList.add(0, sms);
                            length_debit += 1;
                            adapter.notifyItemInserted(0);
                        } else {
                            c.moveToPrevious();
                            continue;
                        }
                        break;

                    case "Net Bank":
                        sms.setMsgType(t);
                        if (getAmount(body) != null) {
                            sms.setMsgAmt(getAmount(body));
                        } else {
                            c.moveToPrevious();
                            continue;
                        }
                        if (smsList.size() > 0) {
                            sms.setMsgBal(Integer.toString(smsList.get(0).getBalInt() - Integer.parseInt(getAmount(body))));
                            smsList.add(0, sms);
                            length_online += 1;
                            adapter.notifyItemInserted(0);
                        } else {
                            sms.setMsgBal(getAmount(body));
                            smsList.add(0, sms);
                            length_online += 1;
                            adapter.notifyItemInserted(0);
                        }
                        break;

                    case "CR":
                        sms.setMsgType(t);
                        if (getAmount(body) != null) {
                            sms.setMsgAmt(getAmount(body));
                        } else {
                            c.moveToPrevious();
                            continue;
                        }
                        body = body.replaceFirst(pattern, "");
                        if (getAmount(body) != null) {
                            sms.setMsgBal(getAmount(body));
                            smsList.add(0, sms);
                            length_credit += 1;
                            adapter.notifyItemInserted(0);
                        } else {
                            c.moveToPrevious();
                            continue;
                        }

                        break;
                }
                c.moveToPrevious();
            }
        } else {
            Toast.makeText(MainActivity.this, "No sms to read!!", Toast.LENGTH_SHORT).show();
        }
        c.close();
    }

    public String getAmount(String data) {
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(data);
        if (matcher.find()) {
            try {
                String a = (matcher.group(0));
                a = a.replace("rs", "");
                a = a.replace("inr", "");
                a = a.replace(".", "");
                a = a.replace(" ", "");
                a = a.replace(",", "");
                return a;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
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
                                type = "DR";
                                length_debit += 1;
                                if (index >= 0) {
                                    balance = smsList.get(0).getMsgBal();
                                    bal = Integer.parseInt(balance) - Integer.parseInt(amt);
                                    balance = Integer.toString(bal);
                                } else {
                                    // negative in case of debit
                                    balance = "-" + amt;
                                }
                            } else if (selected == R.id.credit) {
                                type = "CR";
                                length_credit += 1;
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
                            Sms s = new Sms(type, amt, Long.toString(time), balance);
                            s.setFormatDate(getDate(time));
                            smsList.add(0, s);
                            adapter.notifyItemInserted(0);
                            dialog.dismiss();
                        }
                    }
                });
                break;

            case R.id.forward:
                if (smsList.size() > 0) {
                    int debit[] = new int[length_debit];
                    int online[] = new int[length_online];
                    int credit[] = new int[length_credit];

                    int i = 0, j = 0, k = 0;
                    for (Sms s : smsList) {
                        switch (s.getMsgType()) {
                            case "DR":
                                debit[i] = s.getBalInt();
                                i++;
                                break;
                            case "Net Bank":
                                online[j] = s.getBalInt();
                                j++;
                                break;
                            case "CR":
                                credit[k] = s.getBalInt();
                                k++;
                        }
                    }
                    getBarChart(credit, debit, online);
                } else {
                    Toast.makeText(MainActivity.this, "Nothing to displaying", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("Activity Lifecycle", "OnPause Started");
        save();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("Activity Lifecycle", "OnDestroy Started");
        save();
    }

    public void save() {
        try {
            // file output stream is used to open a file for writing
            FileOutputStream f = context.openFileOutput(filename, Context.MODE_PRIVATE);
            // object output stream is used to write object to the file opened
            ObjectOutputStream o = new ObjectOutputStream(f);
            o.writeObject(smsList);
            o.close();
            f.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getDate(long milliSeconds) {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        return formatter.format(new Date(milliSeconds));
    }

    public void getBarChart(int[] credit, int[] debit, int[] online) {
        XYMultipleSeriesRenderer barChartRenderer = getBarChartRenderer();
        setBarChartSettings(barChartRenderer);
        Intent intent = ChartFactory.getBarChartIntent(this, getBarDemoDataset(credit, debit, online), barChartRenderer, BarChart.Type.DEFAULT);
        startActivity(intent);
    }

    private XYMultipleSeriesDataset getBarDemoDataset(int[] credit, int[] debit, int[] online) {
        XYMultipleSeriesDataset barChartDataset = new XYMultipleSeriesDataset();
        CategorySeries firstSeries = new CategorySeries("Income");
        for (int i = 0; i < length_credit; i++)
            firstSeries.add(credit[i]);
        barChartDataset.addSeries(firstSeries.toXYSeries());

        CategorySeries secondSeries = new CategorySeries("Card Payment");
        for (int i = 0; i < length_debit; i++)
            secondSeries.add(debit[i]);
        barChartDataset.addSeries(secondSeries.toXYSeries());

        CategorySeries thirdSeries = new CategorySeries("Net Banking");
        for (int i = 0; i < length_online; i++)
            thirdSeries.add(online[i]);
        barChartDataset.addSeries(thirdSeries.toXYSeries());

        return barChartDataset;
    }

    public XYMultipleSeriesRenderer getBarChartRenderer() {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setAxisTitleTextSize(20);
        renderer.setChartTitleTextSize(20);
        renderer.setLabelsTextSize(20);
        renderer.setLegendTextSize(20);
        renderer.setMargins(new int[]{30, 40, 30, 0});
        SimpleSeriesRenderer r = new SimpleSeriesRenderer();
        r.setColor(Color.parseColor("#00ff00"));
        renderer.addSeriesRenderer(r);
        r = new SimpleSeriesRenderer();
        r.setColor(Color.parseColor("#ff0000"));
        renderer.addSeriesRenderer(r);
        r = new SimpleSeriesRenderer();
        r.setColor(Color.parseColor("#0000ff"));
        renderer.addSeriesRenderer(r);
        return renderer;
    }

    private void setBarChartSettings(XYMultipleSeriesRenderer renderer) {
        renderer.setChartTitle("Balance Bar Chart");
        renderer.setXTitle("Date -->");
        renderer.setYTitle("Balance(INR)");
        renderer.setXAxisMin(0);
        renderer.setXAxisMax(30);
        renderer.setYAxisMin(0);
        renderer.setYAxisMax(60000);
    }

}
