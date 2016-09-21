package com.realsil.android.wristbanddemo.utility;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.realsil.android.wristbanddemo.R;
import com.realsil.android.wristbanddemo.WristbandDetailActivity;

/**
 * Created by Administrator on 2016/4/13.
 */
public class WristbandSplashFragment extends Fragment {
    ImageView mivSplashImageView;
    int mImageId;
    public void initial(int id) {
        mImageId = id;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_splash, container, false);

        mivSplashImageView = (ImageView) rootView.findViewById(R.id.ivSplashImageView);
        mivSplashImageView.setImageResource(mImageId);
        return rootView;
    }
}
