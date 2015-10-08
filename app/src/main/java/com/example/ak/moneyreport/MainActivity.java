package com.example.ak.moneyreport;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
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
    private Float bal;

    RecyclerView report;

    private String filename = "messages";
    private String TAG = "Log Debug";

    private String pattern = "(?:inr|rs)+[\\s]*+[0-9]*+[\\\\,]*+[0-9]*+[\\\\.]{1}+[0-9]{2}";

    MyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("Activity Lifecycle", "OnCreate Started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button readSms = (Button) findViewById(R.id.readInbox);
        report = (RecyclerView) findViewById(R.id.report);

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

                sms.setMsgDate(date);
                sms.setFormatDate(getDate(Long.parseLong(date)));

                body = body.toLowerCase();

                if (body.contains("debit") && (body.contains("bal") || body.contains("balance")) && !body.contains("recharge")) {
                    t = "DR";
                } else if (body.contains("credit card") && !body.contains("payment") && !body.contains("recharge")) {
                    t = "Net Bank";
                } else if (body.contains("credit") && ((body.contains("bal")) || (body.contains("balance"))) && !body.contains("recharge")) {
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
                            report.scrollToPosition(0);
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
                            sms.setMsgBal(Double.toString(smsList.get(0).getBalDouble() - Double.parseDouble(getAmount(body))));
                            smsList.add(0, sms);
                            report.scrollToPosition(0);
                            adapter.notifyItemInserted(0);
                        } else {
                            sms.setMsgBal(getAmount(body));
                            smsList.add(0, sms);
                            report.scrollToPosition(0);
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
                            report.scrollToPosition(0);
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
                                if (index >= 0) {
                                    balance = smsList.get(0).getMsgBal();
                                    bal = Float.parseFloat(balance) - Float.parseFloat(amt);
                                    balance = Float.toString(bal);
                                } else {
                                    // negative in case of debit
                                    balance = "-" + amt;
                                }
                            } else if (selected == R.id.credit) {
                                type = "CR";
                                if (index >= 0) {
                                    balance = smsList.get(0).getMsgBal();
                                    bal = Float.parseFloat(balance) + Float.parseFloat(amt);
                                    balance = Float.toString(bal);
                                } else {
                                    // positive in case of credit
                                    balance = amt;
                                }
                            } else if (selected == R.id.online) {
                                type = "Net Bank";
                                if (index >= 0) {
                                    balance = smsList.get(0).getMsgBal();
                                    bal = Float.parseFloat(balance) - Float.parseFloat(amt);
                                    balance = Float.toString(bal);
                                } else {
                                    // negative in case of net banking
                                    balance = "-" + amt;
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
                            report.scrollToPosition(0);
                            adapter.notifyItemInserted(0);
                            dialog.dismiss();
                        }
                    }
                });
                break;

            case R.id.forward:
                if (smsList.size() > 0) {

                    List<Double> newDebit = new ArrayList<>();
                    newDebit.add(0, 0.0);
                    List<Double> newOnline = new ArrayList<>();
                    newOnline.add(0, 0.0);
                    List<Double> newCredit = new ArrayList<>();
                    newCredit.add(0, 0.0);
                    List<String> newWeek = new ArrayList<>();

                    String week = getWeek(smsList.get(0).getDateLong());
                    newWeek.add(0, week);
                    int g = 0;
                    for (Sms sms : smsList) {
                        if (getWeek(sms.getDateLong()).equals(week)) {
                            if (sms.getMsgType().equals("DR")) {
                                newDebit.set(g, newDebit.get(g) + sms.getAmtDouble());
                            }
                            if (sms.getMsgType().equals("Net Bank")) {
                                newOnline.set(g, newOnline.get(g) + sms.getAmtDouble());
                            }
                            if (sms.getMsgType().equals("CR")) {
                                newCredit.set(g, newCredit.get(g) + sms.getAmtDouble());
                            }
                        } else {
                            g++;
                            newDebit.add(g, 0.0);
                            newOnline.add(g, 0.0);
                            newCredit.add(g, 0.0);
                            if (sms.getMsgType().equals("DR"))
                                newDebit.set(g, sms.getAmtDouble());
                            if (sms.getMsgType().equals("Net Bank"))
                                newOnline.set(g, sms.getAmtDouble());
                            if (sms.getMsgType().equals("CR"))
                                newCredit.set(g, sms.getAmtDouble());
                            newWeek.add(g, getWeek(sms.getDateLong()));
                            week = getWeek(sms.getDateLong());
                        }
                    }

                    double max = 0;

                    double[] d = new double[newDebit.size()];
                    for (int z = 0; z < newDebit.size(); z++) {
                        d[z] = newDebit.get(z);
                        if (d[z] > max)
                            max = d[z];
                    }


                    double[] n = new double[newOnline.size()];
                    for (int z = 0; z < newOnline.size(); z++) {
                        n[z] = newOnline.get(z);
                        if (n[z] > max)
                            max = n[z];
                    }


                    double[] c = new double[newCredit.size()];
                    for (int z = 0; z < newCredit.size(); z++) {
                        c[z] = newCredit.get(z);
                        if (c[z] > max)
                            max = c[z];
                    }


                    String[] w = new String[newWeek.size()];
                    for (int z = 0; z < newWeek.size(); z++) {
                        w[z] = newWeek.get(z);
                    }

                    getBarChart(c, d, n, (int) max, w);
                } else {
                    Toast.makeText(MainActivity.this, "Nothing to displaying", Toast.LENGTH_SHORT).show();
                }
                break;
        }

        return super.

                onOptionsItemSelected(item);

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

    public String getWeek(long milliSeconds) {
        return "Week-" + new SimpleDateFormat("W").format(new Date(milliSeconds)) + " of " + new SimpleDateFormat("MMM").format(new Date(milliSeconds));
    }

    public void getBarChart(double[] credit, double[] debit, double[] online, int max, String[] week) {
        XYMultipleSeriesRenderer barChartRenderer = getBarChartRenderer();
        setBarChartSettings(barChartRenderer, max, week);
        Intent intent = ChartFactory.getBarChartIntent(this, getBarDemoDataset(credit, debit, online), barChartRenderer, BarChart.Type.DEFAULT);
        startActivity(intent);
    }

    private XYMultipleSeriesDataset getBarDemoDataset(double[] credit, double[] debit, double[] online) {
        XYMultipleSeriesDataset barChartDataset = new XYMultipleSeriesDataset();
        CategorySeries firstSeries = new CategorySeries("Income");
        for (double i : credit)
            firstSeries.add(i);
        barChartDataset.addSeries(firstSeries.toXYSeries());

        CategorySeries secondSeries = new CategorySeries("Expenses");
        for (double i : debit)
            secondSeries.add(i);
        barChartDataset.addSeries(secondSeries.toXYSeries());

        CategorySeries thirdSeries = new CategorySeries("Credit Card");
        for (double i : online)
            thirdSeries.add(i);
        barChartDataset.addSeries(thirdSeries.toXYSeries());

        CategorySeries forthSeries = new CategorySeries("");
        for (double i : debit)
            forthSeries.add(i);
        barChartDataset.addSeries(forthSeries.toXYSeries());

        return barChartDataset;
    }

    public XYMultipleSeriesRenderer getBarChartRenderer() {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setLabelsTextSize(25);
        renderer.setLegendTextSize(30);
        renderer.setLegendHeight(120);
        renderer.setMargins(new int[]{30, 70, 30, 0});
        SimpleSeriesRenderer r = new SimpleSeriesRenderer();
        r.setColor(Color.parseColor("#00E676"));
        r.setDisplayChartValues(true);
        r.setChartValuesTextSize(23);
        r.setChartValuesTextAlign(Paint.Align.RIGHT);
        renderer.addSeriesRenderer(r);
        r = new SimpleSeriesRenderer();
        r.setColor(Color.parseColor("#FF3D00"));
        r.setDisplayChartValues(true);
        r.setChartValuesTextSize(23);
        r.setChartValuesTextAlign(Paint.Align.RIGHT);
        renderer.addSeriesRenderer(r);
        r = new SimpleSeriesRenderer();
        r.setColor(Color.parseColor("#00B0FF"));
        r.setDisplayChartValues(true);
        r.setChartValuesTextSize(23);
        r.setChartValuesTextAlign(Paint.Align.RIGHT);
        renderer.addSeriesRenderer(r);
        r = new SimpleSeriesRenderer();
        r.setColor(Color.parseColor("#ffffff"));
        renderer.addSeriesRenderer(r);
        return renderer;
    }

    private void setBarChartSettings(XYMultipleSeriesRenderer renderer, int max, String[] week) {
        renderer.setChartTitle("Transactions Chart");
        renderer.setChartTitleTextSize(40);
        renderer.setXTitle("");
        renderer.setYTitle("Sum of transactions (INR)");
        renderer.setAxisTitleTextSize(30);
        renderer.setApplyBackgroundColor(true);
        renderer.setAxesColor(Color.BLACK);
        renderer.setLabelsColor(Color.BLACK);
        renderer.setXLabelsColor(Color.BLACK);
        renderer.setYLabelsColor(0, Color.BLACK);
        renderer.setBackgroundColor(Color.WHITE);
        renderer.setMarginsColor(Color.WHITE);
        for (int i = 0; i < week.length; i++) {
            Log.e(TAG, "week no:" + week[i]);
            renderer.addXTextLabel(i + 1, week[i]);
        }
        renderer.setXLabels(0);
        renderer.setXAxisMin(0);
        renderer.setXAxisMax(2);
        renderer.setPanEnabled(true, false);
        renderer.setShowGrid(true);
        renderer.setGridColor(Color.LTGRAY);
        renderer.setBarWidth(80);
        renderer.setYAxisMin(0);
        renderer.setYAxisMax(max + 5000);
    }

}
