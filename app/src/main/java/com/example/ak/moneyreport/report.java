package com.example.ak.moneyreport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
    List<Sms> smsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report);

        graph = (GraphView) findViewById(R.id.graph);

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
        Log.e("Log Debug", Integer.toString(expenseDay.size()));

        double[] e = new double[expenses.size()];
        for (int z = 0; z < expenses.size(); z++) {
            e[z] = expenses.get(expenses.size() - 1 - z);
        }

        String[] w = new String[expenseDay.size()];
        for (int z = 0; z < expenseDay.size(); z++) {
            w[z] = new SimpleDateFormat("dd/MM").format(new Date(expenseDay.get(expenseDay.size() - 1 - z)));
        }

        if (w.length > 1) {
            DataPoint[] dataPoints = new DataPoint[expenses.size()];
            for (int k = 0; k < expenses.size(); k++) {
                dataPoints[k] = new DataPoint(k, e[k]);
                Log.e("Log Debug", w[k].toString());
            }

            BarGraphSeries<DataPoint> series = new BarGraphSeries<>(dataPoints);
            graph.addSeries(series);

            series.setOnDataPointTapListener(new OnDataPointTapListener() {
                @Override
                public void onTap(Series series, DataPointInterface dataPoint) {
                    Toast.makeText(report.this, "Tapped on:" + dataPoint, Toast.LENGTH_SHORT).show();
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
