package com.manage.ak.moneyreport;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

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
    private List<Sms> bankList = new ArrayList<>();
    private List<Sms> cashList = new ArrayList<>();

    private String BALANCE = "0.0";

    // variables used to store and calculate balance in the action bar add button
    private String amt, type, balance;
    private double bal;
    private String cashSpent = "0.0";

    // output stream file in which sms are saved
    private String filename = "messages";
    private String filename1 = "Cash Transactions";

    private TextView bankBalance;
    private TextView estimateDate;
    private TextView spentAmount;
    private TextView cashBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("Activity Lifecycle", "OnCreate Started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences saveBal = getSharedPreferences("BAL", Context.MODE_PRIVATE);
        if (!saveBal.getString("BALANCE", "").equals(""))
            BALANCE = saveBal.getString("BALANCE", "");

        Log.e("LOG DEBUG", BALANCE);

        if (BALANCE.equals("0.0")) {
            final Dialog bal_dialog = new Dialog(context);
            bal_dialog.setContentView(R.layout.balance_dialog);
            bal_dialog.setTitle("Enter Bank Balance");

            final EditText etBalance = (EditText) bal_dialog.findViewById(R.id.Balance);
            Button saveBalance = (Button) bal_dialog.findViewById(R.id.saveBalance);
            bal_dialog.show();

            saveBalance.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BALANCE = etBalance.getText().toString();
                    if (BALANCE.trim().equals("")) {
                        BALANCE = "0.0";
                    }
                    bal_dialog.dismiss();
                }
            });
        }

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        try {
            // file input stream is used to open a file for reading
            FileInputStream f = context.openFileInput(filename);
            // object input stream is used to read object from the file opened
            ObjectInputStream i = new ObjectInputStream(f);
            // object read is in the form of list<Sms> so iterate over the list to extract all Sms objects.
            for (Sms r : (List<Sms>) i.readObject()) {
                bankList.add(r);
            }
            i.close();
            f.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // file input stream is used to open a file for reading
            FileInputStream f = context.openFileInput(filename1);
            // object input stream is used to read object from the file opened
            ObjectInputStream i = new ObjectInputStream(f);
            // object read is in the form of list<Sms> so iterate over the list to extract all Sms objects.
            for (Sms r : (List<Sms>) i.readObject()) {
                cashList.add(r);
            }
            i.close();
            f.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SharedPreferences saveSpent = getSharedPreferences("KEY", Context.MODE_PRIVATE);
        if (!saveSpent.getString("SPENT", "").equals(""))
            cashSpent = saveSpent.getString("SPENT", "");

        bankBalance = (TextView) findViewById(R.id.bankBalance);
        estimateDate = (TextView) findViewById(R.id.estimateDate);

        spentAmount = (TextView) findViewById(R.id.spendAmount);
        cashBalance = (TextView) findViewById(R.id.cashBalance);

        if (cashList.size() > 0) {
            cashBalance.setText(cashList.get(0).getMsgBal());
        } else {
            cashBalance.setText("₹ " + "0.0");
        }

        spentAmount.setText("₹ " + cashSpent);

        CardView bankCard = (CardView) findViewById(R.id.bankCard);
        bankCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bankList.size() > 0) {
                    Intent bank = new Intent(MainActivity.this, BankTransactions.class);
                    Bundle b = new Bundle();
                    b.putSerializable("SMS", (Serializable) bankList);
                    bank.putExtra("DATA", b);
                    startActivity(bank);
                } else {
                    Toast.makeText(MainActivity.this, "No Bank Transactions to display", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (bankList.size() > 0) {
            bankBalance.setText("₹ " + bankList.get(0).getMsgBal());
            estimateDate.setText(bankList.get(0).getFormatDate());
        } else {
            bankBalance.setText("₹ " + "0.0");
            estimateDate.setText(" ");
        }

        CardView cashCard = (CardView) findViewById(R.id.cashCard);
        cashCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cash = new Intent(MainActivity.this, CashTransactions.class);
                Bundle b = new Bundle();
                b.putSerializable("CASH", (Serializable) cashList);
                b.putString("Spent", cashSpent);
                cash.putExtra("DATA", b);
                startActivityForResult(cash, 1);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            cashList = (ArrayList<Sms>) data.getBundleExtra("Result").getSerializable("cash");
            cashSpent = data.getBundleExtra("Result").getString("SPENT");
            if (cashList.size() > 0) {
                cashBalance.setText("₹ " + cashList.get(0).getMsgBal());
            } else {
                cashBalance.setText("₹ " + "0.0");
            }

            spentAmount.setText("₹ " + cashSpent);
        }
    }

    private void readMessages() {

        // new sms object declared
        Sms sms;

        // this is used to filter already read messages
        String filter = null;

        // if list is greater than zero then the last date is read and a filter greater than that date is made
        if (bankList.size() > 0) {
            String lastDate = bankList.get(0).getMsgDate();
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
                if ((body.contains("debited") || body.contains("transactions")) && !body.contains("recharge")) {
                    t = "Personal Expenses";
                } else if ((body.contains("credited") || body.contains("deposited")) && !body.contains("recharge")) {
                    t = "Income";
                }

                // switched according to the type to extract information from the message
                switch (t) {
                    case "Personal Expenses":
                        sms.setMsgType(t);
                        String a = getAmount(body);
                        // getAmount is a method which gives the amount using pattern and matcher
                        if (a != null) {
                            sms.setMsgAmt(a);
                            sms.setMsgBal(Double.toString(Double.parseDouble(BALANCE) - Double.parseDouble(a)));
                            BALANCE = sms.getMsgBal();
                            bankList.add(0, sms);
                        } else {
                            c.moveToPrevious();
                            continue;
                        }
                        /*if (FLAG == 1) {
                            body = body.replaceFirst(pattern1, "");
                        } else if (FLAG == 2) {
                            body = body.replaceFirst(pattern2, "");
                        }
                        FLAG = 0;
                        if (getAmount(body) != null) {
                            sms.setMsgBal(getAmount(body));
                            bankList.add(0, sms);
                        } else {
                            c.moveToPrevious();
                            continue;

                        }*/
                        break;

                    // for type of transaction income first the amount is extracted and then the balance is extracted
                    case "Income":
                        sms.setMsgType(t);
                        String a1 = getAmount(body);
                        if (a1 != null) {
                            sms.setMsgAmt(a1);
                            sms.setMsgBal(Double.toString(Double.parseDouble(BALANCE) + Double.parseDouble(a1)));
                            BALANCE = sms.getMsgBal();
                            bankList.add(0, sms);
                        } else {
                            c.moveToPrevious();
                            continue;
                        }
                        /*if (FLAG == 1) {
                            body = body.replaceFirst(pattern1, "");
                        } else if (FLAG == 2) {
                            body = body.replaceFirst(pattern2, "");
                        }
                        FLAG = 0;
                        if (getAmount(body) != null) {
                            sms.setMsgBal(getAmount(body));
                            bankList.add(0, sms);
                        } else {
                            c.moveToPrevious();
                            continue;
                        }*/
                        break;
                }
                c.moveToPrevious();
            }
        } else {
            // if no messages to read than a toast is displayed
            Toast.makeText(MainActivity.this, "No sms to read!!", Toast.LENGTH_SHORT).show();
        }
        c.close();
        if (bankList.size() > 0) {
            bankBalance.setText("₹ " + bankList.get(0).getMsgBal());
            estimateDate.setText(bankList.get(0).getFormatDate());
        } else {
            bankBalance.setText("₹ " + "0.0");
            estimateDate.setText(" ");
        }
    }

    // getting amount by matching the pattern
    public String getAmount(String data) {
        String pattern1 = "(inr)+[\\s]?+[0-9]*+[\\\\,]*+[0-9]*+[\\\\.][0-9]{2}";
        Pattern regex1 = Pattern.compile(pattern1);
        String pattern2 = "(rs)+[\\\\.][\\s]*+[0-9]*+[\\\\,]*+[0-9]*+[\\\\.][0-9]{2}";
        Pattern regex2 = Pattern.compile(pattern2);
        Matcher matcher1 = regex1.matcher(data);
        Matcher matcher2 = regex2.matcher(data);
        if (matcher1.find()) {
            try {
                // searched for rs or inr preceding number in the form of inr **,***.**
                String a = (matcher1.group(0));
                a = a.replace("inr", "");
                a = a.replace(" ", "");
                a = a.replace(",", "");
//                FLAG = 1;
                return a;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (matcher2.find()) {
            try {
                // searched for rs or inr preceding number in the form of rs. **,***.**
                String a = (matcher2.group(0));
                a = a.replace("rs", "");
                a = a.replaceFirst(".", "");
                a = a.replace(" ", "");
                a = a.replace(",", "");
//                FLAG = 2;
                return a;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        FLAG = 0;
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
                dialog.setTitle("Add Cash Transaction");

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
                        // then according to the id of check button balance is calculated
                        int index = cashList.size() - 1;
                        amt = addAmt.getText().toString();
                        if (amt.trim().equals("")) {
                            amt = "0.0";
                        }

                        switch (type) {
                            case "Personal Expenses":
                                // index will be zero if there are no messages in the list so balance will be equal to the amount
                                if (index >= 0) {
                                    balance = cashList.get(0).getMsgBal();
                                    bal = Double.parseDouble(balance) - Double.parseDouble(amt);
                                    balance = Double.toString(bal);
                                } else {
                                    // negative in case of expenses
                                    balance = "-" + amt;
                                }
                                break;
                            case "Food":
                                // index will be zero if there are no messages in the list so balance will be equal to the amount
                                if (index >= 0) {
                                    balance = cashList.get(0).getMsgBal();
                                    bal = Double.parseDouble(balance) - Double.parseDouble(amt);
                                    balance = Double.toString(bal);
                                } else {
                                    // negative in case of expenses
                                    balance = "-" + amt;
                                }
                                break;
                            case "Transport":
                                // index will be zero if there are no messages in the list so balance will be equal to the amount
                                if (index >= 0) {
                                    balance = cashList.get(0).getMsgBal();
                                    bal = Double.parseDouble(balance) - Double.parseDouble(amt);
                                    balance = Double.toString(bal);
                                } else {
                                    // negative in case of expenses
                                    balance = "-" + amt;
                                }
                                break;
                            case "Salary":
                                if (index >= 0) {
                                    balance = cashList.get(0).getMsgBal();
                                    bal = Double.parseDouble(balance) + Double.parseDouble(amt);
                                    balance = Double.toString(bal);
                                } else {
                                    // positive in case of income
                                    balance = amt;
                                }
                                break;
                            case "Income":
                                if (index >= 0) {
                                    balance = cashList.get(0).getMsgBal();
                                    bal = Double.parseDouble(balance) + Double.parseDouble(amt);
                                    balance = Double.toString(bal);
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
                        cashList.add(0, s);
                        dialog.dismiss();
                        if (s.getMsgType().equals("Personal Expenses") || s.getMsgType().equals("Food") || s.getMsgType().equals("Transport")) {
                            cashSpent = Double.toString(Double.parseDouble(cashSpent) + Double.parseDouble(amt));
                            spentAmount.setText(cashSpent);
                        }
                        cashBalance.setText(cashList.get(0).getMsgBal());
                        Toast.makeText(MainActivity.this, "Transaction Added", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        save();
    }

    public void save() {
        try {
            // file output stream is used to open a file for writing
            FileOutputStream f = context.openFileOutput(filename, Context.MODE_PRIVATE);
            // object output stream is used to write object to the file opened
            ObjectOutputStream o = new ObjectOutputStream(f);
            o.writeObject(bankList);
            o.close();
            f.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            // file output stream is used to open a file for writing
            FileOutputStream f = context.openFileOutput(filename1, Context.MODE_PRIVATE);
            // object output stream is used to write object to the file opened
            ObjectOutputStream o = new ObjectOutputStream(f);
            o.writeObject(cashList);
            o.close();
            f.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SharedPreferences saveSpent = getSharedPreferences("KEY", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = saveSpent.edit();
        editor.putString("SPENT", cashSpent);
        editor.commit();

        SharedPreferences saveBal = getSharedPreferences("BAL", context.MODE_PRIVATE);
        SharedPreferences.Editor editor1 = saveBal.edit();
        editor1.putString("BALANCE", BALANCE);
        editor1.commit();
    }

    public String getDate(long milliSeconds) {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MMM/yy");
        return formatter.format(new Date(milliSeconds));
    }
}
