package com.manage.ak.moneyreport;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// activity to show cash transactions in the cash_transactions.xml layout
public class CashTransactions extends AppCompatActivity {

    private Context context = this;

    // A list of sms required
    private List<Sms> cashList = new ArrayList<>();

    // variables used to store and calculate balance in the action bar add button
    private String amt, type, balance;

    private String cashSpent = "0.0";

    RecyclerView readCash;

    DatabaseHandler databaseHandler = new DatabaseHandler(context);

    private MyAdapter myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cash_transactions);

        // To add a custom action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        readCash = (RecyclerView) findViewById(R.id.readCash);
        readCash.setHasFixedSize(true);
        readCash.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("DATA");
        cashSpent = bundle.getString("Spent");
        cashList = (ArrayList<Sms>) bundle.getSerializable("CASH");

        myAdapter = new MyAdapter(cashList, context);
        readCash.setAdapter(myAdapter);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    // setting the result cashList back to the main activity on back button press
    @Override
    public void onBackPressed() {
        sendDataBack();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cash_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // setting the result cashList back to the main activity on back button press
            case android.R.id.home:
                sendDataBack();
                break;
            case R.id.add:
                final AddDialog dialog = new AddDialog(context, cashList);
                dialog.show();
                dialog.setOnSaveClickListener(new AddDialog.OnSaveClickListener() {
                    @Override
                    public void OnSaveClick() {
                        Sms s = dialog.getTransaction();
                        cashList.add(0, s);
                        databaseHandler.addCashSms(s);

                        readCash.scrollToPosition(0);
                        myAdapter.notifyItemInserted(0);

                        if (s.getDrOrCr().equals("DR")) {
                            cashSpent = Double.toString(Double.parseDouble(cashSpent) + s.getAmtDouble());
                        }

                        Toast.makeText(CashTransactions.this, "Transaction Added", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case R.id.forward:
                List<Sms> smsList1 = new ArrayList<>();
                for (Sms s : cashList) {
                    if (s.getDrOrCr().equals("DR")) {
                        smsList1.add(s);
                    }
                }
                // when forward action button is clicked a bar chart is displayed whose values are calculated here
                if (smsList1.size() > 0) {
                    Intent i = new Intent(CashTransactions.this, report.class);
                    Bundle b = new Bundle();
                    b.putSerializable("SMS", (Serializable) smsList1);
                    // color is sent to the report activity depending on click of bank or cash card
                    b.putString("color", "#467fd9");
                    i.putExtra("DATA", b);
                    startActivity(i);
                } else {
                    // if no messages are there then a toast is displayed
                    Toast.makeText(CashTransactions.this, "You have not spent money", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendDataBack() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable("cash", (Serializable) cashList);
        bundle.putString("SPENT", cashSpent);
        intent.putExtra("Result", bundle);
        setResult(1, intent);
        finish();
    }
}
