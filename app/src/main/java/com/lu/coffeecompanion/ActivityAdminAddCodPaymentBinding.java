package com.lu.coffeecompanion;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public final class ActivityAdminAddCodPaymentBinding implements ViewBinding {
    @NonNull
    private final LinearLayout rootView;

    @NonNull
    public final ImageButton btnBack;

    @NonNull
    public final MaterialButton btnAddPayment;

    @NonNull
    public final TextInputEditText etCustomerName;

    @NonNull
    public final TextInputEditText etAmount;

    @NonNull
    public final TextInputEditText etNotes;

    @NonNull
    public final ProgressBar progressBar;

    private ActivityAdminAddCodPaymentBinding(@NonNull LinearLayout rootView,
                                              @NonNull ImageButton btnBack,
                                              @NonNull MaterialButton btnAddPayment,
                                              @NonNull TextInputEditText etCustomerName,
                                              @NonNull TextInputEditText etAmount,
                                              @NonNull TextInputEditText etNotes,
                                              @NonNull ProgressBar progressBar) {
        this.rootView = rootView;
        this.btnBack = btnBack;
        this.btnAddPayment = btnAddPayment;
        this.etCustomerName = etCustomerName;
        this.etAmount = etAmount;
        this.etNotes = etNotes;
        this.progressBar = progressBar;
    }

    @NonNull
    @Override
    public LinearLayout getRoot() {
        return rootView;
    }

    @NonNull
    public static ActivityAdminAddCodPaymentBinding inflate(@NonNull LayoutInflater inflater) {
        return inflate(inflater, null, false);
    }

    @NonNull
    public static ActivityAdminAddCodPaymentBinding inflate(@NonNull LayoutInflater inflater,
                                                            @Nullable ViewGroup parent,
                                                            boolean attachToParent) {
        View root = inflater.inflate(R.layout.activity_admin_add_cod_payment, parent, false);
        if (attachToParent) {
            parent.addView(root);
        }
        return bind(root);
    }

    @NonNull
    public static ActivityAdminAddCodPaymentBinding bind(@NonNull View rootView) {
        ImageButton btnBack = rootView.findViewById(R.id.btnBack);
        MaterialButton btnAddPayment = rootView.findViewById(R.id.btnAddPayment);
        TextInputEditText etCustomerName = rootView.findViewById(R.id.etCustomerName);
        TextInputEditText etAmount = rootView.findViewById(R.id.etAmount);
        TextInputEditText etNotes = rootView.findViewById(R.id.etNotes);
        ProgressBar progressBar = rootView.findViewById(R.id.progressBar);

        return new ActivityAdminAddCodPaymentBinding((LinearLayout) rootView, btnBack,
                btnAddPayment, etCustomerName, etAmount, etNotes, progressBar);
    }
}