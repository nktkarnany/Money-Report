package com.example.ak.moneyreport;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BankTransactions extends AppCompatActivity {

    private Context context = this;

    // A list of sms required
    private List<Sms> smsList = new ArrayList<>();

    RecyclerView readMessages;

    private MyAdapter myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bank_transactions);

        readMessages = (RecyclerView) findViewById(R.id.readMessages);

        readMessages.setHasFixedSize(true);
        readMessages.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("DATA");
        smsList = (ArrayList<Sms>) bundle.getSerializable("SMS");

        myAdapter = new MyAdapter(smsList, context);
        readMessages.setAdapter(myAdapter);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bank_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.forward:
                // when forward action button is clicked a bar chart is displayed whose values are calculated here
                if (smsList.size() > 0) {

                    List<Sms> smsList1 = new ArrayList<>();

                    for (Sms s : smsList) {
                        if (s.getMsgType().equals("Personal Expenses") || s.getMsgType().equals("Food") || s.getMsgType().equals("Transport")) {
                            smsList1.add(s);
                        }
                    }

                    Intent i = new Intent(BankTransactions.this, report.class);
                    Bundle b = new Bundle();
                    b.putSerializable("SMS", (Serializable) smsList1);
                    i.putExtra("DATA", b);
                    startActivity(i);
                } else {
                    // if no messages are there then a toast is displayed
                    Toast.makeText(BankTransactions.this, "Nothing to displaying", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
