package com.example.ak.moneyreport;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

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
    TextView itemDate;
    Spinner options;
    String optionSelected;

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

        itemDate = (TextView) findViewById(R.id.itemDate);
        options = (Spinner) findViewById(R.id.graphOptions);
        ArrayAdapter<CharSequence> options_adapter = ArrayAdapter.createFromResource(this, R.array.options, android.R.layout.simple_spinner_item);
        options_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        options.setAdapter(options_adapter);

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("DATA");
        smsList = (ArrayList<Sms>) bundle.getSerializable("SMS");

        options.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (parent.getItemAtPosition(position).toString()) {
                    case "Daily":
                        Toast.makeText(report.this, "Daily Selected", Toast.LENGTH_SHORT).show();
                        break;
                    case "Weekly":
                        Toast.makeText(report.this, "Weekly Selected", Toast.LENGTH_SHORT).show();
                        break;
                    case "Monthly":
                        Toast.makeText(report.this, "Monthly Selected", Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                optionSelected = parent.getItemAtPosition(0).toString();
            }
        });


        List<Double> expenses = new ArrayList<>();
        expenses.add(0, 0.0);
        List<Long> expenseDay = new ArrayList<>();
        String day = smsList.get(0).getDay();
        expenseDay.add(0, smsList.get(0).getDateLong());
        int i = 0;
        int j = 0, l = 0;
        int size = 0;
        for (Sms sms : smsList) {
            if (sms.getDay().equals(day)) {
                expenses.set(i, expenses.get(i) + sms.getAmtDouble());
                reportList.add(j, new ReportItem(sms.getMsgType(), sms.getMsgAmt(), sms.getDateLong()));
                j++;
                if (size == smsList.size() - 1) {
                    itemList.add(l, reportList);
                }
            } else {
                i++;
                expenses.add(i, sms.getAmtDouble());
                expenseDay.add(i, sms.getDateLong());

                itemList.add(l, reportList);
                l++;
                reportList = new ArrayList<>();
                j = 0;
                reportList.add(j, new ReportItem(sms.getMsgType(), sms.getMsgAmt(), sms.getDateLong()));
                j++;
                if (size == smsList.size() - 1) {
                    itemList.add(l, reportList);
                }
                day = sms.getDay();
            }
            size++;
        }

        double max = 0;
        double[] e = new double[expenses.size()];
        for (int z = 0; z < expenses.size(); z++) {
            e[z] = expenses.get(z);
        }

        int lenDay = expenseDay.size();
        if (lenDay > 6) {
            lenDay = 6;
        }
        String[] w = new String[lenDay];
        for (int z = 0; z < lenDay; z++) {
            w[z] = new SimpleDateFormat("dd/MM").format(new Date(expenseDay.get(z)));
        }

        if (w.length > 1) {
            DrawGraph(w.length, max, e, w);
        } else {
            Toast.makeText(report.this, "Not Enough data to display", Toast.LENGTH_SHORT).show();
        }

    }

    private void DrawGraph(int length, double max, double[] e, String[] w) {
        if (length > 6) {
            length = 6;
        }
        DataPoint[] dataPoints = new DataPoint[length];
        for (int k = 0; k < length; k++) {
            dataPoints[k] = new DataPoint(k, e[k]);
            if (e[k] > max) {
                max = e[k];
            }
        }

        BarGraphSeries<DataPoint> series = new BarGraphSeries<>(dataPoints);
        graph.addSeries(series);
        series.setColor(Color.parseColor("#e51c23"));
        series.setSpacing(10);
        series.setValuesOnTopColor(Color.RED);
        series.setDrawValuesOnTop(true);

        series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                adapterList.clear();
                adapterList.addAll(itemList.get((int) dataPoint.getX()));
                itemDate.setText(itemList.get((int) dataPoint.getX()).get(0).getReportDate());
                reportAdapter.notifyDataSetChanged();
            }
        });

        graph.setTitle("Expense Chart");
        graph.setTitleTextSize(40);
        graph.setTitleColor(Color.parseColor("#e51c23"));

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(length - 1);

        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(max + 5000);

        StaticLabelsFormatter s = new StaticLabelsFormatter(graph);
        s.setHorizontalLabels(w);
        graph.getGridLabelRenderer().setLabelFormatter(s);
    }

    public String getWeek(long milliSeconds) {
        return "Week:" + new SimpleDateFormat("W").format(new Date(milliSeconds)) + " of " + new SimpleDateFormat("MMM").format(new Date(milliSeconds));
    }

    public String getMonth(long milliSeconds) {
        return new SimpleDateFormat("MMM").format(new Date(milliSeconds)) + "'" + new SimpleDateFormat("yy").format(new Date(milliSeconds));
    }
}
