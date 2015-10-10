package com.example.ak.moneyreport;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class report extends Activity {

    GraphView graph;
    RecyclerView recyclerView;
    ReportAdapter reportAdapter;
    List<Sms> smsList = new ArrayList<>();
    List<ReportItem> adapterList = new ArrayList<>();
    List<ReportItem> reportList = new ArrayList<>();
    List<List<ReportItem>> itemList = new ArrayList<>();

    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report);

        graph = (GraphView) findViewById(R.id.graph);

        recyclerView = (RecyclerView) findViewById(R.id.report_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportAdapter = new ReportAdapter(adapterList, context);
        recyclerView.setAdapter(reportAdapter);

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("DATA");
        smsList = (ArrayList<Sms>) bundle.getSerializable("SMS");

        List<Double> expenses = new ArrayList<>();
        expenses.add(0, 0.0);
        List<Long> expenseDay = new ArrayList<>();
        String day = getDay(smsList.get(0).getDateLong());
        expenseDay.add(0, smsList.get(0).getDateLong());
        int i = 0;
        for (Sms sms : smsList) {
            if (getDay(sms.getDateLong()).equals(day)) {
                if (sms.getMsgType().equals("Personal Expenses") || sms.getMsgType().equals("Food") || sms.getMsgType().equals("Transport")) {
                    expenses.set(i, expenses.get(i) + sms.getAmtDouble());
                }
            } else {
                i++;
                expenses.add(i, 0.0);
                if (sms.getMsgType().equals("Personal Expenses") || sms.getMsgType().equals("Food") || sms.getMsgType().equals("Transport"))
                    expenses.set(i, sms.getAmtDouble());
                expenseDay.add(i, sms.getDateLong());
                day = getDay(sms.getDateLong());
            }
        }

        String day1 = getDay(smsList.get(0).getDateLong());
        int j = 0, l = 0;
        int size = 0;
        for (Sms sms : smsList) {
            if (getDay(sms.getDateLong()).equals(day1)) {
                if (sms.getMsgType().equals("Personal Expenses") || sms.getMsgType().equals("Food") || sms.getMsgType().equals("Transport")) {
                    reportList.add(j, new ReportItem(sms.getMsgType(), sms.getMsgAmt(), sms.getDateLong()));
                    j++;
                    if (size == smsList.size() - 1) {
                        itemList.add(l, reportList);
                    }
                }
            } else {
                itemList.add(l, reportList);
                l++;
                reportList = new ArrayList<>();
                j = 0;
                if (sms.getMsgType().equals("Personal Expenses") || sms.getMsgType().equals("Food") || sms.getMsgType().equals("Transport")) {
                    reportList.add(j, new ReportItem(sms.getMsgType(), sms.getMsgAmt(), sms.getDateLong()));
                    j++;
                    if (size == smsList.size() - 1) {
                        itemList.add(l, reportList);
                    }
                }
                Log.e("Log Debug", day1);
                day1 = getDay(sms.getDateLong());
                Log.e("Log Debug", day1);
            }
            size++;
        }

        double[] e = new double[expenses.size()];
        for (int z = 0; z < expenses.size(); z++) {
            e[z] = expenses.get(z);
        }

        String[] w = new String[expenseDay.size()];
        for (int z = 0; z < expenseDay.size(); z++) {
            w[z] = new SimpleDateFormat("dd/MM").format(new Date(expenseDay.get(z)));
        }

        if (w.length > 1) {
            DataPoint[] dataPoints = new DataPoint[expenses.size()];
            for (int k = 0; k < expenses.size(); k++) {
                dataPoints[k] = new DataPoint(k, e[k]);
            }

            BarGraphSeries<DataPoint> series = new BarGraphSeries<>(dataPoints);
            graph.addSeries(series);

            series.setOnDataPointTapListener(new OnDataPointTapListener() {
                @Override
                public void onTap(Series series, DataPointInterface dataPoint) {
                    adapterList.clear();
                    adapterList.addAll(itemList.get((int) dataPoint.getX()));
                    reportAdapter.notifyDataSetChanged();
                }
            });

            StaticLabelsFormatter s = new StaticLabelsFormatter(graph);
            s.setHorizontalLabels(w);
            graph.getGridLabelRenderer().setLabelFormatter(s);

            graph.getViewport().isScrollable();
        } else {
            Toast.makeText(report.this, "Not Enough data to display", Toast.LENGTH_SHORT).show();
        }

    }

    public String getDay(long milliSeconds) {
        return new SimpleDateFormat("dd/MM").format(new Date(milliSeconds));
    }
}
