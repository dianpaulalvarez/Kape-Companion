package com.lu.coffeecompanion;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import com.lu.coffeecompanion.databinding.ActivityAdminDashboardBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboard";

    private ActivityAdminDashboardBinding binding;
    private DatabaseReference dbRef;
    private FirebaseFirestore firestore;

    private TextView tvTotalGcashPayments;
    private TextView tvTotalCodPayments;
    private TextView tvOverallTotal;
    private BarChart barChart;

    private Button btnStartDate, btnEndDate;
    private TextView tvChartGcashTotal, tvChartCodTotal, tvChartOverallTotal;
    private TextView tvDateRange;

    private Calendar startCalendar, endCalendar;
    private SimpleDateFormat dateFormat, displayDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();
        initializeDateFormatters();
        initializeCalendars();
        initializeViews();
        setupDateButtons();
        setupBarChart();
        setupCardClickListeners();
        loadInitialData();
    }

    private void initializeFirebase() {
        dbRef = FirebaseDatabase.getInstance().getReference();
        firestore = FirebaseFirestore.getInstance();
    }

    private void initializeDateFormatters() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    private void initializeCalendars() {
        startCalendar = Calendar.getInstance();
        startCalendar.add(Calendar.DAY_OF_YEAR, -6);
        endCalendar = Calendar.getInstance();
    }

    private void initializeViews() {
        tvTotalGcashPayments = findViewById(R.id.tvTotalGcashPayments);
        tvTotalCodPayments = findViewById(R.id.tvTotalCodPayments);
        tvOverallTotal = findViewById(R.id.tvOverallTotal);
        barChart = findViewById(R.id.barChart);

        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);
        tvChartGcashTotal = findViewById(R.id.tvChartGcashTotal);
        tvChartCodTotal = findViewById(R.id.tvChartCodTotal);
        tvChartOverallTotal = findViewById(R.id.tvChartOverallTotal);
        tvDateRange = findViewById(R.id.tvDateRange);
    }

    private void setupDateButtons() {
        updateDateButtons();
        btnStartDate.setOnClickListener(v -> showStartDatePicker());
        btnEndDate.setOnClickListener(v -> showEndDatePicker());
    }

    private void setupCardClickListeners() {
        // User Management
        CardView userManagementCard = findViewById(R.id.cardUserManagement);
        if (userManagementCard != null) {
            userManagementCard.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminManageUsersActivity.class);
                startActivity(intent);
            });
        }

        // Manage Orders
        CardView manageOrdersCard = findViewById(R.id.cardManageOrders);
        if (manageOrdersCard != null) {
            manageOrdersCard.setOnClickListener(v -> {
                Intent intent = new Intent(this, ManageOrdersActivity.class);
                startActivity(intent);
            });
        }

        // View All Payments
        CardView paymentsCard = findViewById(R.id.cardPayments);
        if (paymentsCard != null) {
            paymentsCard.setOnClickListener(v -> {
                Intent intent = new Intent(this, ViewAllPaymentsActivity.class);
                startActivity(intent);
            });
        }

        // Add COD Payment
        CardView addCodPaymentCard = findViewById(R.id.cardAddCodPayment);
        if (addCodPaymentCard != null) {
            addCodPaymentCard.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminAddCodPaymentActivity.class);
                startActivity(intent);
            });
        }

        // Inventory Management
        CardView inventoryCard = findViewById(R.id.cardInventory);
        if (inventoryCard != null) {
            inventoryCard.setOnClickListener(v -> {
                Intent intent = new Intent(this, InventoryManagementActivity.class);
                startActivity(intent);
            });
        }

        // Logout
        if (binding.btnLogout != null) {
            binding.btnLogout.setOnClickListener(v -> showLogoutDialog());
        }
    }

    private void loadInitialData() {
        loadUserStats();
        loadPaymentStatistics();
    }

    private void showStartDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    startCalendar.set(year, month, dayOfMonth);
                    if (startCalendar.after(endCalendar)) {
                        endCalendar.setTime(startCalendar.getTime());
                    }
                    updateDateButtons();
                    loadPaymentStatistics();
                },
                startCalendar.get(Calendar.YEAR),
                startCalendar.get(Calendar.MONTH),
                startCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showEndDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    endCalendar.set(year, month, dayOfMonth);
                    if (endCalendar.before(startCalendar)) {
                        startCalendar.setTime(endCalendar.getTime());
                    }
                    updateDateButtons();
                    loadPaymentStatistics();
                },
                endCalendar.get(Calendar.YEAR),
                endCalendar.get(Calendar.MONTH),
                endCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void updateDateButtons() {
        btnStartDate.setText(displayDateFormat.format(startCalendar.getTime()));
        btnEndDate.setText(displayDateFormat.format(endCalendar.getTime()));

        String rangeText = displayDateFormat.format(startCalendar.getTime()) +
                " - " + displayDateFormat.format(endCalendar.getTime());
        tvDateRange.setText(rangeText);
    }

    private void setupBarChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setHighlightFullBarEnabled(false);
        barChart.setPinchZoom(true);
        barChart.setScaleEnabled(true);
        barChart.setDrawValueAboveBar(true);
        barChart.setExtraBottomOffset(10f);
        barChart.setExtraTopOffset(10f);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#5D4037"));
        xAxis.setTextSize(10f);
        xAxis.setLabelRotationAngle(-45f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        leftAxis.setTextColor(Color.parseColor("#5D4037"));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "₱" + String.format("%.0f", value);
            }
        });

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);

        Legend legend = barChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(Color.parseColor("#5D4037"));
        legend.setTextSize(12f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);

        barChart.animateY(800);
    }

    private void loadUserStats() {
        dbRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    int totalUsers = (int) snapshot.getChildrenCount();
                    int activeUsers = 0;

                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        Boolean blocked = userSnapshot.child("blocked").getValue(Boolean.class);
                        if (blocked == null || !blocked) {
                            activeUsers++;
                        }
                    }

                    TextView tvTotalUsers = findViewById(R.id.tvTotalUsers);
                    TextView tvActiveUsers = findViewById(R.id.tvActiveUsers);

                    if (tvTotalUsers != null) {
                        tvTotalUsers.setText(String.valueOf(totalUsers));
                    }
                    if (tvActiveUsers != null) {
                        tvActiveUsers.setText(String.valueOf(activeUsers));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading user stats", error.toException());
            }
        });
    }

    private void loadPaymentStatistics() {
        Log.d(TAG, "Loading payment statistics...");

        Map<String, Double> gcashByDate = new TreeMap<>();
        Map<String, Double> codByDate = new TreeMap<>();

        Calendar current = (Calendar) startCalendar.clone();
        while (!current.after(endCalendar)) {
            String key = dateFormat.format(current.getTime());
            gcashByDate.put(key, 0.0);
            codByDate.put(key, 0.0);
            current.add(Calendar.DAY_OF_YEAR, 1);
        }

        Calendar startOfDay = (Calendar) startCalendar.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = (Calendar) endCalendar.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        Date startDate = startOfDay.getTime();
        Date endDate = endOfDay.getTime();

        loadAllPayments(gcashByDate, codByDate, startDate, endDate);
    }

    private void loadAllPayments(Map<String, Double> gcashByDate, Map<String, Double> codByDate,
                                 Date startDate, Date endDate) {
        final double[] allTimeGcash = {0.0};
        final double[] allTimeCod = {0.0};
        final double[] chartGcash = {0.0};
        final double[] chartCod = {0.0};

        firestore.collection("orders")
                .whereEqualTo("paymentMethod", "GCash")
                .get()
                .addOnSuccessListener(gcashSnapshot -> {
                    Log.d(TAG, "GCash orders found: " + gcashSnapshot.size());

                    for (QueryDocumentSnapshot doc : gcashSnapshot) {
                        Double totalPrice = doc.getDouble("totalPrice");
                        Double deliveryFee = doc.getDouble("deliveryFee");

                        if (totalPrice == null) {
                            continue;
                        }

                        double orderTotal = totalPrice + (deliveryFee != null ? deliveryFee : 0.0);
                        allTimeGcash[0] += orderTotal;

                        Timestamp ts = doc.getTimestamp("orderTimestamp");
                        if (ts == null) {
                            ts = doc.getTimestamp("timestamp");
                        }

                        if (ts != null) {
                            Date orderDate = ts.toDate();

                            if (!orderDate.before(startDate) && !orderDate.after(endDate)) {
                                chartGcash[0] += orderTotal;

                                String dateKey = dateFormat.format(orderDate);
                                if (gcashByDate.containsKey(dateKey)) {
                                    double current = gcashByDate.get(dateKey);
                                    gcashByDate.put(dateKey, current + orderTotal);
                                }
                            }
                        }
                    }

                    loadCodPayments(gcashByDate, codByDate, startDate, endDate,
                            allTimeGcash[0], chartGcash[0], allTimeCod, chartCod);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading GCash payments", e);
                    updateUI(gcashByDate, codByDate, 0.0, 0.0, 0.0, 0.0);
                });
    }

    private void loadCodPayments(Map<String, Double> gcashByDate, Map<String, Double> codByDate,
                                 Date startDate, Date endDate, double allTimeGcash,
                                 double chartGcash, double[] allTimeCod, double[] chartCod) {
        firestore.collection("cod_payments")
                .get()
                .addOnSuccessListener(codSnapshot -> {
                    Log.d(TAG, "COD payments found: " + codSnapshot.size());

                    for (QueryDocumentSnapshot doc : codSnapshot) {
                        Double amount = doc.getDouble("amount");
                        if (amount == null) {
                            continue;
                        }

                        allTimeCod[0] += amount;

                        Timestamp ts = doc.getTimestamp("timestamp");
                        if (ts != null) {
                            Date paymentDate = ts.toDate();

                            if (!paymentDate.before(startDate) && !paymentDate.after(endDate)) {
                                chartCod[0] += amount;

                                String dateKey = dateFormat.format(paymentDate);
                                if (codByDate.containsKey(dateKey)) {
                                    double current = codByDate.get(dateKey);
                                    codByDate.put(dateKey, current + amount);
                                }
                            }
                        }
                    }

                    loadCodFromOrders(gcashByDate, codByDate, startDate, endDate,
                            allTimeGcash, chartGcash, allTimeCod, chartCod);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading COD payments", e);
                    updateUI(gcashByDate, codByDate, allTimeGcash, chartGcash, 0.0, 0.0);
                });
    }

    private void loadCodFromOrders(Map<String, Double> gcashByDate, Map<String, Double> codByDate,
                                   Date startDate, Date endDate, double allTimeGcash,
                                   double chartGcash, double[] allTimeCod, double[] chartCod) {
        firestore.collection("orders")
                .whereEqualTo("paymentMethod", "Cash on Delivery")
                .get()
                .addOnSuccessListener(codOrdersSnapshot -> {
                    Log.d(TAG, "COD orders found: " + codOrdersSnapshot.size());

                    for (QueryDocumentSnapshot doc : codOrdersSnapshot) {
                        Double totalPrice = doc.getDouble("totalPrice");
                        Double deliveryFee = doc.getDouble("deliveryFee");

                        if (totalPrice == null) {
                            continue;
                        }

                        double orderTotal = totalPrice + (deliveryFee != null ? deliveryFee : 0.0);
                        allTimeCod[0] += orderTotal;

                        Timestamp ts = doc.getTimestamp("orderTimestamp");
                        if (ts == null) {
                            ts = doc.getTimestamp("timestamp");
                        }

                        if (ts != null) {
                            Date orderDate = ts.toDate();

                            if (!orderDate.before(startDate) && !orderDate.after(endDate)) {
                                chartCod[0] += orderTotal;

                                String dateKey = dateFormat.format(orderDate);
                                if (codByDate.containsKey(dateKey)) {
                                    double current = codByDate.get(dateKey);
                                    codByDate.put(dateKey, current + orderTotal);
                                }
                            }
                        }
                    }

                    runOnUiThread(() -> updateUI(gcashByDate, codByDate, allTimeGcash,
                            chartGcash, allTimeCod[0], chartCod[0]));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading COD orders", e);
                    runOnUiThread(() -> updateUI(gcashByDate, codByDate, allTimeGcash,
                            chartGcash, allTimeCod[0], chartCod[0]));
                });
    }

    private void updateUI(Map<String, Double> gcashByDate, Map<String, Double> codByDate,
                          double allTimeGcash, double chartGcash, double allTimeCod, double chartCod) {
        updatePaymentStatistics(allTimeGcash, allTimeCod);
        updateBarChart(gcashByDate, codByDate, chartGcash, chartCod);
    }

    private void updatePaymentStatistics(double gcashTotal, double codTotal) {
        double overallTotal = gcashTotal + codTotal;

        Log.d(TAG, "Updating payment stats - GCash: ₱" + gcashTotal + ", COD: ₱" + codTotal);

        if (tvTotalGcashPayments != null) {
            tvTotalGcashPayments.setText(String.format("₱%.2f", gcashTotal));
        }

        if (tvTotalCodPayments != null) {
            tvTotalCodPayments.setText(String.format("₱%.2f", codTotal));
        }

        if (tvOverallTotal != null) {
            tvOverallTotal.setText(String.format("₱%.2f", overallTotal));
        }
    }

    private void updateBarChart(Map<String, Double> gcashByDate, Map<String, Double> codByDate,
                                double gcashTotal, double codTotal) {
        Log.d(TAG, "Updating bar chart...");

        ArrayList<BarEntry> gcashEntries = new ArrayList<>();
        ArrayList<BarEntry> codEntries = new ArrayList<>();
        ArrayList<String> dateLabels = new ArrayList<>();

        SimpleDateFormat chartDateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

        int index = 0;
        for (String dateKey : gcashByDate.keySet()) {
            float gcashValue = gcashByDate.get(dateKey).floatValue();
            float codValue = codByDate.get(dateKey).floatValue();

            gcashEntries.add(new BarEntry(index, gcashValue));
            codEntries.add(new BarEntry(index, codValue));

            try {
                Date date = dateFormat.parse(dateKey);
                dateLabels.add(chartDateFormat.format(date));
            } catch (Exception e) {
                dateLabels.add(dateKey);
            }

            index++;
        }

        BarDataSet gcashDataSet = new BarDataSet(gcashEntries, "GCash");
        gcashDataSet.setColor(Color.parseColor("#1565C0"));
        gcashDataSet.setValueTextColor(Color.parseColor("#5D4037"));
        gcashDataSet.setValueTextSize(9f);
        gcashDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                return "₱" + String.format("%.0f", value);
            }
        });

        BarDataSet codDataSet = new BarDataSet(codEntries, "COD");
        codDataSet.setColor(Color.parseColor("#EF6C00"));
        codDataSet.setValueTextColor(Color.parseColor("#5D4037"));
        codDataSet.setValueTextSize(9f);
        codDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                return "₱" + String.format("%.0f", value);
            }
        });

        BarData barData = new BarData(gcashDataSet, codDataSet);

        float groupSpace = 0.12f;
        float barSpace = 0.02f;
        float barWidth = 0.42f;
        barData.setBarWidth(barWidth);

        barChart.setData(barData);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dateLabels));
        barChart.getXAxis().setLabelCount(dateLabels.size());
        barChart.getXAxis().setAxisMinimum(-0.5f);
        barChart.getXAxis().setAxisMaximum(dateLabels.size() - 0.5f);

        if (dateLabels.size() > 1) {
            barChart.groupBars(-0.5f, groupSpace, barSpace);
        }

        barChart.invalidate();

        updateChartTotals(gcashTotal, codTotal);
    }

    private void updateChartTotals(double gcashTotal, double codTotal) {
        double overallTotal = gcashTotal + codTotal;

        if (tvChartGcashTotal != null) {
            tvChartGcashTotal.setText(String.format("₱%.2f", gcashTotal));
        }

        if (tvChartCodTotal != null) {
            tvChartCodTotal.setText(String.format("₱%.2f", codTotal));
        }

        if (tvChartOverallTotal != null) {
            tvChartOverallTotal.setText(String.format("₱%.2f", overallTotal));
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserStats();
        loadPaymentStatistics();
    }
}