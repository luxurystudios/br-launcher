package com.byparad1st.launcher.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.byparad1st.game.R;
import com.byparad1st.launcher.activity.LoaderActivity;

public class LoaderFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_loader, container, false);
        startActivity(new Intent(getActivity(), LoaderActivity.class));
        return v;
    }
}
