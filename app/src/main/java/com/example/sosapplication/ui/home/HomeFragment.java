package com.example.sosapplication.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.sosapplication.R;
import com.example.sosapplication.databinding.FragmentHomeBinding;
import com.example.sosapplication.utils.LocaleHelper;
import com.example.sosapplication.utils.ThemeHelper;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupAnimations();
        setupClickListeners();
        updateThemeIcon();
        updateLanguageSelection();

        return root;
    }

    private void setupAnimations() {
        // Entrance animation for SOS button
        binding.btnSOS.setScaleX(0.8f);
        binding.btnSOS.setScaleY(0.8f);
        binding.btnSOS.setAlpha(0f);
        binding.btnSOS.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Entrance animation for Training button
        binding.btnTraining.setTranslationY(100f);
        binding.btnTraining.setAlpha(0f);
        binding.btnTraining.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void setupClickListeners() {
        // SOS Button
        binding.btnSOS.setOnClickListener(v -> {
            animateButtonPress(v);
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_dashboard);
        });

        // Training Button
        binding.btnTraining.setOnClickListener(v -> {
            animateButtonPress(v);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.navigation_training);
        });

        // Theme Toggle
        binding.btnTheme.setOnClickListener(v -> {
            animateButtonPress(v);
            ThemeHelper.toggleTheme(requireContext());
        });

        // Language Buttons
        binding.btnEN.setOnClickListener(v -> changeLang("en"));
        binding.btnSK.setOnClickListener(v -> changeLang("sk"));
        binding.btnUA.setOnClickListener(v -> changeLang("uk"));
    }

    private void animateButtonPress(View view) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start())
                .start();
    }

    private void updateThemeIcon() {
        boolean isDark = ThemeHelper.isDarkMode(requireContext());
        binding.btnTheme.setImageResource(
                isDark ? R.drawable.ic_theme_light : R.drawable.ic_theme_dark
        );
    }

    private void updateLanguageSelection() {
        String currentLang = LocaleHelper.getLanguage(requireContext());
        boolean isDark = ThemeHelper.isDarkMode(requireContext());
        
        updateLangButton(binding.btnEN, "en".equals(currentLang), isDark);
        updateLangButton(binding.btnSK, "sk".equals(currentLang), isDark);
        updateLangButton(binding.btnUA, "uk".equals(currentLang), isDark);
    }
    
    private void updateLangButton(TextView button, boolean isSelected, boolean isDarkTheme) {
        button.setSelected(isSelected);
        if (isSelected) {
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
            if (isDarkTheme) {
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            } else {
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary_light));
            }
        }
    }

    private void changeLang(String lang) {
        LocaleHelper.setLocale(requireContext(), lang);
        requireActivity().recreate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
