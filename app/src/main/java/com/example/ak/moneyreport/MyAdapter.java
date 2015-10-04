package com.example.ak.moneyreport;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    private Context context;
    List<Sms> smsList;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView smsType;
        public TextView smsAmt;
        public TextView smsDate;
        public TextView smsBal;

        public ViewHolder(View v) {
            super(v);

            smsType = (TextView) v.findViewById(R.id.smsType);
            smsAmt = (TextView) v.findViewById(R.id.smsAmt);
            smsDate = (TextView) v.findViewById(R.id.smsDate);
            smsBal = (TextView) v.findViewById(R.id.smsBal);
        }
    }

    public MyAdapter(List<Sms> smsList, Context context) {
        this.smsList = smsList;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.smsType.setText(smsList.get(position).getMsgType());
        holder.smsAmt.setText("INR " + smsList.get(position).getMsgAmt());
        holder.smsDate.setText("Date: " + smsList.get(position).getMsgDate());
        holder.smsBal.setText(smsList.get(position).getMsgBal());
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public int getItemCount() {
        return smsList.size();
    }

}
