package com.example.sosapplication.ui.training;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.sosapplication.R;

import java.util.ArrayList;
import java.util.List;

public class TrainingFragment extends Fragment {

    private ViewPager2 viewPager;

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
    }
}
