package com.example.sosapplication.ui.training;

import android.content.pm.ActivityInfo;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.sosapplication.R;

import java.util.ArrayList;
import java.util.List;

public class TrainingFragment extends Fragment {

    private ViewPager2 viewPager;
    private LinearLayout pageIndicator;
    private final List<View> dots = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_training,
                container,
                false
        );

        viewPager = view.findViewById(R.id.trainingViewPager);
        pageIndicator = view.findViewById(R.id.pageIndicator);
        
        setupTraining();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        );
    }

    @Override
    public void onPause() {
        super.onPause();
        requireActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        );
    }

    private void setupTraining() {
        List<TrainingStep> steps = new ArrayList<>();

        steps.add(new TrainingStep(
                R.drawable.step1_check,
                getString(R.string.training_step_1_title),
                getString(R.string.training_step_1_desc)
        ));

        steps.add(new TrainingStep(
                R.drawable.step2_call,
                getString(R.string.training_step_2_title),
                getString(R.string.training_step_2_desc)
        ));

        steps.add(new TrainingStep(
                R.drawable.step3_compression,
                getString(R.string.training_step_3_title),
                getString(R.string.training_step_3_desc)
        ));

        steps.add(new TrainingStep(
                R.drawable.step4_breath,
                getString(R.string.training_step_4_title),
                getString(R.string.training_step_4_desc)
        ));

        steps.add(new TrainingStep(
                R.drawable.step5_continue,
                getString(R.string.training_step_5_title),
                getString(R.string.training_step_5_desc)
        ));

        TrainingPagerAdapter adapter = new TrainingPagerAdapter(steps);
        viewPager.setAdapter(adapter);
        
        // Setup page indicator
        setupPageIndicator(steps.size());
        
        // Page change callback
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updatePageIndicator(position);
            }
        });
    }
    
    private void setupPageIndicator(int count) {
        dots.clear();
        pageIndicator.removeAllViews();
        
        for (int i = 0; i < count; i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(12, 12);
            params.setMargins(8, 0, 8, 0);
            dot.setLayoutParams(params);
            
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(ContextCompat.getColor(requireContext(), 
                    i == 0 ? R.color.accent_blue : R.color.text_secondary_light));
            dot.setBackground(drawable);
            
            pageIndicator.addView(dot);
            dots.add(dot);
        }
    }
    
    private void updatePageIndicator(int position) {
        for (int i = 0; i < dots.size(); i++) {
            View dot = dots.get(i);
            GradientDrawable drawable = (GradientDrawable) dot.getBackground();
            
            if (i == position) {
                drawable.setColor(ContextCompat.getColor(requireContext(), R.color.accent_blue));
                dot.animate().scaleX(1.5f).scaleY(1.5f).setDuration(200).start();
            } else {
                drawable.setColor(ContextCompat.getColor(requireContext(), R.color.text_secondary_light));
                dot.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
            }
        }
    }
}
