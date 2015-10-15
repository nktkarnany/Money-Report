package com.example.ak.moneyreport;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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

public class MainActivity extends AppCompatActivity {

    private Context context = this;

    // A list of sms required
    private List<Sms> smsList = new ArrayList<>();

    // variables used to store and calculate balance in the action bar add button
    private String amt, type, balance;
    private Float bal;

    // recycler view to display list of sms
    RecyclerView report;

    SwipeRefreshLayout swipeRefreshLayout;

    // output stream file in which sms are saved
    private String filename = "messages";

    // a pattern used to find amount from messages
    private String pattern = "(?:inr|rs)+[\\s]*+[0-9]*+[\\\\,]*+[0-9]*+[\\\\.]{1}+[0-9]{2}";

    // variables to store type of transaction
    private String INCOME = "Income";
    private String EXPENSES = "Personal Expenses";

    MyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("Activity Lifecycle", "OnCreate Started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);
        report = (RecyclerView) findViewById(R.id.report);

        report.setHasFixedSize(true);
        report.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyAdapter(smsList, context);
        report.setAdapter(adapter);

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

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Toast.makeText(MainActivity.this, "Reading Messages...", Toast.LENGTH_SHORT).show();
                readMessages();
            }
        });
    }


    private void readMessages() {

        // new sms object declared
        Sms sms;

        // this is used to filter already read messages
        String filter = null;

        // if list is greater than zero then the last date is read and a filter greater than that date  is made
        if (smsList.size() > 0) {
            String lastDate = smsList.get(0).getMsgDate();
            filter = "date>" + lastDate;
        }

        // read sms are stored in cursor
        Cursor c = getContentResolver().query(Uri.parse("content://sms/inbox"), new String[]{"date", "body"}, filter, null, null);
        int total = c.getCount();

        // all messages are read from bottom because when new sms gets inserted they are inserted in the position zero
        // thus to keep the latest messages up in the list
        if (c.moveToLast()) {
            for (int i = 0; i < total; i++) {
                sms = new Sms();

                // body and date read from cursor
                String date = c.getString(c.getColumnIndexOrThrow("date"));
                String body = c.getString(c.getColumnIndexOrThrow("body"));
                String t = "";

                // date is set to the sms object
                sms.setMsgDate(date);
                sms.setFormatDate(getDate(Long.parseLong(date)));

                body = body.toLowerCase();

                // some common keywords used in bank messages
                if (body.contains("debit") && (body.contains("bal") || body.contains("balance")) && !body.contains("recharge")) {
                    t = EXPENSES;
                } else if (body.contains("credit") && !(body.contains("card")) && !body.contains("recharge")) {
                    t = INCOME;
                }

                // switched according to the type to extract information from the message
                switch (t) {
                    case "Personal Expenses":
                        sms.setMsgType(t);
                        // getAmount is a method which gives the amount using pattern and matcher
                        if (getAmount(body) != null) {
                            sms.setMsgAmt(getAmount(body));
                        } else {
                            c.moveToPrevious();
                            continue;
                        }
                        // the first pattern is replaced because balance has same pattern and is after the transaction amount
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

                    // for type of transaction income first the amount is extracted and then the balance is extracted
                    case "Income":
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
            // if no messages to read than a toast is displayed
            Toast.makeText(MainActivity.this, "No sms to read!!", Toast.LENGTH_SHORT).show();
        }
        c.close();
        swipeRefreshLayout.setRefreshing(false);
    }

    // getting amount by matching the pattern
    public String getAmount(String data) {
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(data);
        if (matcher.find()) {
            try {
                // searched for rs or inr preceding number in the form of **,***.**
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
            case R.id.refresh:
                Toast.makeText(MainActivity.this, "Reading Messages...", Toast.LENGTH_SHORT).show();
                readMessages();
                break;
            // when action button add is clicked a dialog box appears
            case R.id.add:
                final Dialog dialog = new Dialog(context);
                dialog.setContentView(R.layout.add_dialog);
                dialog.setTitle("Add Transaction");

                final EditText addAmt = (EditText) dialog.findViewById(R.id.addAmt);

                final Spinner description = (Spinner) dialog.findViewById(R.id.description);
                ArrayAdapter<CharSequence> desc_adapter = ArrayAdapter.createFromResource(this, R.array.desc_array, android.R.layout.simple_spinner_item);
                desc_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                description.setAdapter(desc_adapter);
                description.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        type = parent.getItemAtPosition(position).toString();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        type = parent.getItemAtPosition(0).toString();
                    }
                });

                Button save = (Button) dialog.findViewById(R.id.save);

                dialog.show();

                // set onclick listener to add button in the custom dialog box to add transaction
                save.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // when save button is clicked then first the readMessages is read messages from inbox if not read
                        // this updates the list of sms and keeps the latest message on the top
                        readMessages();
                        // then according to the id of check button balance is calculated
                        int index = smsList.size() - 1;
                        amt = addAmt.getText().toString();
                        if (amt.trim().equals("")) {
                            amt = "0.0";
                        }

                        switch (type) {
                            case "Personal Expenses":
                                // index will be zero if there are no messages in the list so balance will be equal to the amount
                                if (index >= 0) {
                                    balance = smsList.get(0).getMsgBal();
                                    bal = Float.parseFloat(balance) - Float.parseFloat(amt);
                                    balance = Float.toString(bal);
                                } else {
                                    // negative in case of expenses
                                    balance = "-" + amt;
                                }
                                break;
                            case "Food":
                                // index will be zero if there are no messages in the list so balance will be equal to the amount
                                if (index >= 0) {
                                    balance = smsList.get(0).getMsgBal();
                                    bal = Float.parseFloat(balance) - Float.parseFloat(amt);
                                    balance = Float.toString(bal);
                                } else {
                                    // negative in case of expenses
                                    balance = "-" + amt;
                                }
                                break;
                            case "Transport":
                                // index will be zero if there are no messages in the list so balance will be equal to the amount
                                if (index >= 0) {
                                    balance = smsList.get(0).getMsgBal();
                                    bal = Float.parseFloat(balance) - Float.parseFloat(amt);
                                    balance = Float.toString(bal);
                                } else {
                                    // negative in case of expenses
                                    balance = "-" + amt;
                                }
                                break;
                            case "Salary":
                                if (index >= 0) {
                                    balance = smsList.get(0).getMsgBal();
                                    bal = Float.parseFloat(balance) + Float.parseFloat(amt);
                                    balance = Float.toString(bal);
                                } else {
                                    // positive in case of income
                                    balance = amt;
                                }
                                break;
                            case "Income":
                                if (index >= 0) {
                                    balance = smsList.get(0).getMsgBal();
                                    bal = Float.parseFloat(balance) + Float.parseFloat(amt);
                                    balance = Float.toString(bal);
                                } else {
                                    // positive in case of income
                                    balance = amt;
                                }
                                break;
                        }
                        // finally the sms object is added to the list
                        // the date in this sms object is set ot the current date of the system
                        long time = System.currentTimeMillis();
                        Sms s = new Sms(type, amt, Long.toString(time), balance);
                        s.setFormatDate(getDate(time));
                        smsList.add(0, s);
                        report.scrollToPosition(0);
                        adapter.notifyItemInserted(0);
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "Transaction Added", Toast.LENGTH_SHORT).show();
                    }
                });
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

                    Intent i = new Intent(MainActivity.this, report.class);
                    Bundle b = new Bundle();
                    b.putSerializable("SMS", (Serializable) smsList1);
                    i.putExtra("DATA", b);
                    startActivity(i);
                } else {
                    // if no messages are there then a toast is displayed
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
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy HH:mm");
        return formatter.format(new Date(milliSeconds));
    }
}
