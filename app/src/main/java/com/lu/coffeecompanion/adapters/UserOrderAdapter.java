package com.lu.coffeecompanion.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.lu.coffeecompanion.OrderDetailsActivity;
import com.lu.coffeecompanion.R;
import com.lu.coffeecompanion.models.Order;  // ✅ FIXED IMPORT

import java.util.List;
import java.util.Locale;

public class UserOrderAdapter extends RecyclerView.Adapter<UserOrderAdapter.OrderViewHolder> {

    private Context context;
    private List<Order> orderList;

    public UserOrderAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_user_card, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);

        // Receipt Number
        holder.tvReceiptNumber.setText(order.getReceiptNumber() != null ?
                order.getReceiptNumber() :
                "Order #" + order.getOrderId().substring(0, Math.min(8, order.getOrderId().length())).toUpperCase());

        // Date
        holder.tvOrderDate.setText(order.getFormattedDate());

        // Total Amount - FIXED: Added Locale
        holder.tvTotalAmount.setText(String.format(Locale.getDefault(), "₱%.2f", order.getTotalAmount()));

        // Status
        holder.tvStatus.setText(order.getStatus() != null ? order.getStatus().toUpperCase() : "PENDING");
        setStatusStyle(holder.tvStatus, order.getStatus());

        // Click listener
        holder.cardOrder.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailsActivity.class);
            intent.putExtra("orderId", order.getOrderId());
            context.startActivity(intent);
        });
    }

    private void setStatusStyle(TextView tvStatus, String status) {
        if (status == null) status = "pending";

        int backgroundColor;
        switch (status.toLowerCase()) {
            case "pending":
                backgroundColor = 0xFFFFC107; // Yellow
                break;
            case "preparing":
            case "confirmed":
                backgroundColor = 0xFF2196F3; // Blue
                break;
            case "out for delivery":
                backgroundColor = 0xFFFF9800; // Orange
                break;
            case "delivered":
            case "completed":
                backgroundColor = 0xFF4CAF50; // Green
                break;
            case "cancelled":
                backgroundColor = 0xFFF44336; // Red
                break;
            default:
                backgroundColor = 0xFFFFC107;
        }

        tvStatus.setBackgroundColor(backgroundColor);
        tvStatus.setTextColor(0xFFFFFFFF); // White text
        tvStatus.setPadding(24, 12, 24, 12);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        CardView cardOrder;
        TextView tvReceiptNumber, tvOrderDate, tvTotalAmount, tvStatus;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardOrder = itemView.findViewById(R.id.cardOrder);
            tvReceiptNumber = itemView.findViewById(R.id.tvReceiptNumber);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}