package com.example.facerecognition.ui.main;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.facerecognition.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


import java.io.Serializable;
import java.util.List;

import static java.lang.Math.abs;


/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_CURRENTTAB = "currentTab";

    private static final String ARG_SECTION_SIMILARITY = "section_similarity";


    private int CURRENTPICTURE;

    private List<Bitmap> currentTab;

    private double[] similarity;

    public static PlaceholderFragment newInstance(int index,List<Bitmap> currentTab,double[] similarity) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();

        bundle.putSerializable(ARG_CURRENTTAB, (Serializable)currentTab);
        bundle.putSerializable(ARG_SECTION_SIMILARITY,similarity);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            currentTab = (List<Bitmap>) getArguments().getSerializable(ARG_CURRENTTAB);
            similarity = (double[]) getArguments().getSerializable(ARG_SECTION_SIMILARITY);
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_match, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CURRENTPICTURE = 0;

        final ImageView imageView = getView().findViewById(R.id.face_imageView);

        TextView facesimilarity = getView().findViewById(R.id.face_similarity);
        TextView eyesimilarity = getView().findViewById(R.id.eyes_similarity);
        TextView nosesimilarity = getView().findViewById(R.id.nose_similarity);
        TextView mouthsimilarity = getView().findViewById(R.id.mouth_similarity);

        imageView.setImageBitmap(currentTab.get(0));


        facesimilarity.append(String.format("%.5f",abs(similarity[0])));
        eyesimilarity.append(String.format("%.5f",abs(similarity[1])));
        nosesimilarity.append(String.format("%.5f",abs(similarity[2])));
        mouthsimilarity.append(String.format("%.5f",abs(similarity[3])));

        FloatingActionButton fab_exchange = getView().findViewById(R.id.fab_exchange);

        fab_exchange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CURRENTPICTURE = CURRENTPICTURE^0^1;
                imageView.setImageBitmap(currentTab.get(CURRENTPICTURE));
            }
        });
    }
}