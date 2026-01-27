package com.example.sosapplication.ui.home;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.sosapplication.R;
import com.example.sosapplication.databinding.FragmentHomeBinding;

import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // ✅ SOS
        binding.btnSOS.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_dashboard);
        });

        // ✅ Training
        binding.btnTraining.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.navigation_training)
        );

        // ✅ Language buttons
        binding.btnEN.setOnClickListener(v -> setLocale("en"));
        binding.btnSK.setOnClickListener(v -> setLocale("sk"));
        binding.btnUA.setOnClickListener(v -> setLocale("uk"));

        return root;
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        requireActivity().getResources().updateConfiguration(
                config,
                requireActivity().getResources().getDisplayMetrics()
        );

        // 🔄 перерисовываем экран
        requireActivity().recreate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
