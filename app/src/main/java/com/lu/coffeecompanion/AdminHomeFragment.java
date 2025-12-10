package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.lu.coffeecompanion.databinding.FragmentAdminHomeBinding;

public class AdminHomeFragment extends Fragment {

    private FragmentAdminHomeBinding binding;

    public AdminHomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAdminHomeBinding.inflate(inflater, container, false);

        // Setup User Management card click listener
        CardView userManagementCard = binding.getRoot().findViewById(R.id.cardUserManagement);
        if (userManagementCard != null) {
            userManagementCard.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), AdminManageUsersActivity.class);
                startActivity(intent);
            });
        }

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}