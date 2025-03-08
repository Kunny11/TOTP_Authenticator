package com.authenticator.totp;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class OtpAdapter extends RecyclerView.Adapter<OtpAdapter.ViewHolder> {

    private List<OtpInfo> otpInfoList;
    private OnOtpItemLongClickListener longClickListener;

    public interface OnOtpItemLongClickListener {
        void onItemLongClick(int position);
    }

    public void setOnOtpItemLongClickListener(OnOtpItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public OtpAdapter(List<OtpInfo> otpInfoList) {
        this.otpInfoList = otpInfoList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.otp_item, parent, false);
        return new ViewHolder(view, longClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OtpInfo otpInfo = otpInfoList.get(position);

        Log.d("v", "Position: " + position);
        Log.d("v", "Account Name: " + otpInfo.getAccountName());
        Log.d("v", "Issuer: " + otpInfo.getIssuer());
        Log.d("v", "Generated OTP: " + otpInfo.getGeneratedOTP());

        holder.accountNameTextView.setText(otpInfo.getAccountName());
        holder.issuerTextView.setText(otpInfo.getIssuer());
        holder.otpTextView.setText(otpInfo.getGeneratedOTP());
    }

    @Override
    public int getItemCount() {
        Log.d("RecyclerView_Debug", "Item Count: " + otpInfoList.size());
        return otpInfoList.size();
    }


    public void updateOtpList(List<OtpInfo> newOtpInfoList) {
        otpInfoList.clear();
        otpInfoList.addAll(newOtpInfoList);
        notifyDataSetChanged(); // Ensuring RecyclerView updates properly
    }

    public void removeItem(int position) {
        if (position >= 0 && position < otpInfoList.size()) {
            otpInfoList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, otpInfoList.size()); // Ensure smooth UI update
        }
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView accountNameTextView;
        public TextView issuerTextView;
        public TextView otpTextView;

        public ViewHolder(View view, final OnOtpItemLongClickListener longClickListener) {
            super(view);
            accountNameTextView = view.findViewById(R.id.account_name);
            issuerTextView = view.findViewById(R.id.issuer);
            otpTextView = view.findViewById(R.id.otp);

            view.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        longClickListener.onItemLongClick(position);
                    }
                }
                return true;
            });
        }
    }
}
