package com.lu.coffeecompanion.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.lu.coffeecompanion.R;
import com.lu.coffeecompanion.models.OrderModel;

import java.util.List;

public class ToReviewAdapter extends RecyclerView.Adapter<ToReviewAdapter.ViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(OrderModel order);
    }

    private List<OrderModel> orderList;
    private OnOrderClickListener listener;

    public ToReviewAdapter(List<OrderModel> orderList, OnOrderClickListener listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_to_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        OrderModel order = orderList.get(position);

        holder.tvOrderId.setText("Order ID: " + order.getOrderId());
        holder.tvTotal.setText("₱" + order.getTotalPrice());

        holder.itemView.setOnClickListener(v -> listener.onOrderClick(order));
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvOrderId, tvTotal;

        public ViewHolder(View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvTotal = itemView.findViewById(R.id.tvTotalPrice);
        }
    }
}
