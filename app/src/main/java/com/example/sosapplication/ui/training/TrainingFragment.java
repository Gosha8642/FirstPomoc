package com.example.sosapplication.ui.training;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sosapplication.R;

import java.util.ArrayList;
import java.util.List;

public class TrainingFragment extends Fragment implements TrainingCategoryAdapter.OnCategoryClickListener {

    private RecyclerView trainingList;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_training, container, false);
        
        trainingList = view.findViewById(R.id.trainingList);
        trainingList.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        List<TrainingCategory> categories = new ArrayList<>();
        
        // CPR - No Breathing
        categories.add(new TrainingCategory(
                1,
                R.drawable.ic_training_cpr,
                getString(R.string.training_cpr_title),
                getString(R.string.training_cpr_desc)
        ));
        
        // Choking
        categories.add(new TrainingCategory(
                2,
                R.drawable.ic_training_choking,
                getString(R.string.training_choking_title),
                getString(R.string.training_choking_desc)
        ));
        
        // Bleeding
        categories.add(new TrainingCategory(
                3,
                R.drawable.ic_training_bleeding,
                getString(R.string.training_bleeding_title),
                getString(R.string.training_bleeding_desc)
        ));
        
        // Burns
        categories.add(new TrainingCategory(
                4,
                R.drawable.ic_training_burns,
                getString(R.string.training_burns_title),
                getString(R.string.training_burns_desc)
        ));
        
        // Fractures
        categories.add(new TrainingCategory(
                5,
                R.drawable.ic_training_fracture,
                getString(R.string.training_fracture_title),
                getString(R.string.training_fracture_desc)
        ));
        
        TrainingCategoryAdapter adapter = new TrainingCategoryAdapter(categories, this);
        trainingList.setAdapter(adapter);

        return view;
    }

    @Override
    public void onCategoryClick(TrainingCategory category) {
        Bundle args = new Bundle();
        args.putInt("training_id", category.id);
        args.putString("training_title", category.title);
        
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_training_to_detail, args);
    }
}
