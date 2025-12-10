package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lu.coffeecompanion.databinding.ShopsFragmentBinding;

public class ShopsFragment extends Fragment {

    ShopsFragmentBinding binding;
    FirebaseFirestore db;
    FirebaseAuth auth;
    FirebaseUser currentUser;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopsFragmentBinding.inflate(inflater,container,false);
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if(currentUser == null){
            Intent intent = new Intent(requireContext(), MainActivity.class);
            startActivity(intent);
            requireActivity().finish();
        }
        binding.cart.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CartActivity.class);
            startActivity(intent);
        });
        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                binding.itemContainer3.removeAllViews();
                fetchVerticalScrollView(binding.itemContainer3, "shops");
            }
        });
        binding.itemContainer3.removeAllViews();
        fetchVerticalScrollView(binding.itemContainer3, "shops");

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void fetchVerticalScrollView(LinearLayout itemContainer, String collectionPath) {
        ProgressBar progressBar = new ProgressBar(requireContext());
        LinearLayout.LayoutParams progressBarParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        progressBar.setLayoutParams(progressBarParams);
        itemContainer.addView(progressBar);
        progressBar.setVisibility(View.VISIBLE);

        db.collection(collectionPath)
                .orderBy("order")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);

                    itemContainer.removeAllViews();

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String coffeeName = document.getString("name");
                        String imageUrl = document.getString("imageUrl");
                        String location = document.getString("location");

                        Long ratingValue = document.getLong("rating");
                        double doubleRatingValue = ratingValue.doubleValue();
                        String rating = String.format("%.1f", doubleRatingValue);

                        View coffeeLayout = LayoutInflater.from(requireContext())
                                .inflate(R.layout.item_bestwithcoffee, itemContainer, false);

                        ImageView coffeeImage = coffeeLayout.findViewById(R.id.coffee_image);
                        TextView coffeeText = coffeeLayout.findViewById(R.id.coffee_name);
                        TextView locationText = coffeeLayout.findViewById(R.id.location_text);

                        coffeeText.setText(coffeeName);
                        locationText.setText(location);
                        Glide.with(requireContext()).load(imageUrl).into(coffeeImage);

                        coffeeLayout.setOnClickListener(v -> {
                            Intent intent = new Intent(requireContext(), ShopMenuActivity.class);
                            intent.putExtra("documentId", document.getId());
                            startActivity(intent);
                        });

                        itemContainer.addView(coffeeLayout);
                    }
                });
    }
}