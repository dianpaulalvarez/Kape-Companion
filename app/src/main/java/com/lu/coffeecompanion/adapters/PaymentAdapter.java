package com.lu.coffeecompanion.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lu.coffeecompanion.databinding.ItemPaymentBinding;
import com.lu.coffeecompanion.models.Payment;

import java.util.List;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder> {

    private List<Payment> payments;
    private OnPaymentClickListener listener;

    public interface OnPaymentClickListener {
        void onPaymentClick(Payment payment);
    }

    public PaymentAdapter(List<Payment> payments, OnPaymentClickListener listener) {
        this.payments = payments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPaymentBinding binding = ItemPaymentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PaymentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        Payment payment = payments.get(position);
        holder.bind(payment, listener);
    }

    @Override
    public int getItemCount() {
        return payments.size();
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {
        private ItemPaymentBinding binding;

        public PaymentViewHolder(ItemPaymentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Payment payment, OnPaymentClickListener listener) {
            binding.receiptNumber.setText(payment.getReceiptNumber() != null
                    ? payment.getReceiptNumber() : "N/A");
            binding.customerName.setText(payment.getCustomerName() != null
                    ? payment.getCustomerName() : "Unknown");
            binding.contactNumber.setText(payment.getContactNumber() != null
                    ? payment.getContactNumber() : "N/A");
            binding.paymentMethod.setText(payment.getPaymentMethod() != null
                    ? payment.getPaymentMethod() : "N/A");
            binding.totalAmount.setText(String.format("₱%.2f",
                    payment.getTotalAmount() + payment.getDeliveryFee()));
            binding.orderDate.setText(payment.getFormattedDate());

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPaymentClick(payment);
                }
            });
        }
    }
}