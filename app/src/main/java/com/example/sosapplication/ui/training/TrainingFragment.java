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
                "Krok 1 – Skontroluj vedomie a dýchanie",
                "Skontroluj, či osoba reaguje. Jemne zatras ramenom a nahlas sa opýtaj: „Si v poriadku?“ " +
                        "Sleduj, či dýcha – pohyb hrudníka, zvuky dýchania."
        ));

        steps.add(new TrainingStep(
                R.drawable.step2_call,
                "Krok 2 – Zavolaj núdzové číslo",
                "Ak osoba nereaguje a nedýcha, okamžite zavolaj na číslo 112. " +
                        "Ak je niekto nablízku, požiadaj ho o pomoc."
        ));

        steps.add(new TrainingStep(
                R.drawable.step3_compression,
                "Krok 3 – Stláčaj hrudník",
                "Polož dlane na stred hrudníka. Stláčaj silno a rýchlo " +
                        "(100–120 stlačení za minútu) do hĺbky 5–6 cm."
        ));

        steps.add(new TrainingStep(
                R.drawable.step4_breath,
                "Krok 4 – Záchranné vdychy (ak si vyškolený)",
                "Po 30 stlačeniach daj 2 vdychy. Zakloň hlavu, zdvihni bradu " +
                        "a sleduj, či sa hrudník dvíha."
        ));

        steps.add(new TrainingStep(
                R.drawable.step5_continue,
                "Krok 5 – Pokračuj až do príchodu pomoci",
                "Pokračuj v cykle 30 stlačení a 2 vdychov, kým nepríde záchranka " +
                        "alebo osoba nezačne dýchať."
        ));

        TrainingPagerAdapter adapter = new TrainingPagerAdapter(steps);
        viewPager.setAdapter(adapter);
    }
}
