// PaymentProofAdapter.java
package com.lu.coffeecompanion.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lu.coffeecompanion.PaymentProofViewerActivity;
import com.lu.coffeecompanion.R;

import java.util.List;
import java.util.Map;

public class PaymentProofAdapter extends RecyclerView.Adapter<PaymentProofAdapter.PaymentViewHolder> {

    private List<Map<String, Object>> payments;
    private OnDeleteClickListener onDeleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }

    public PaymentProofAdapter(List<Map<String, Object>> payments, OnDeleteClickListener listener) {
        this.payments = payments;
        this.onDeleteClickListener = listener;
    }

    public void setDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_proof, parent, false);
        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        Map<String, Object> payment = payments.get(position);

        holder.txtName.setText(String.valueOf(payment.get("name")));

        Object totalPrice = payment.get("totalPrice");
        if (totalPrice instanceof Double) {
            holder.txtAmount.setText(String.format("₱%.2f", (Double) totalPrice));
        } else if (totalPrice instanceof Long) {
            holder.txtAmount.setText(String.format("₱%.2f", ((Long) totalPrice).doubleValue()));
        } else {
            holder.txtAmount.setText(String.format("₱%.2f", 0.0));
        }

        String imageUrl = (String) payment.get("proofImageUrl");

        // View proof button
        holder.btnViewProof.setOnClickListener(v -> {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Intent intent = new Intent(holder.itemView.getContext(), PaymentProofViewerActivity.class);
                intent.putExtra("img", imageUrl);
                holder.itemView.getContext().startActivity(intent);
            }
        });

        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return payments.size();
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtAmount;
        Button btnViewProof, btnDelete;

        public PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            btnViewProof = itemView.findViewById(R.id.btnViewProof);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}