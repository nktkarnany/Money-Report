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
        public TextView smsBody;
        public TextView smsDate;

        public ViewHolder(View v) {
            super(v);

            smsBody = (TextView) v.findViewById(R.id.smsBody);
            smsDate = (TextView) v.findViewById(R.id.smsDate);
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
        holder.smsBody.setText(smsList.get(position).getMsgBody());
        holder.smsDate.setText(smsList.get(position).getMsgDate());
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
