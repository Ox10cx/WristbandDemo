package com.realsil.android.wristbanddemo.utility;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import java.util.ArrayList;

public class RealsilFragmentPagerAdapter extends FragmentPagerAdapter {
    ArrayList<Fragment> list;
    FragmentManager fm;
    public RealsilFragmentPagerAdapter(FragmentManager fm, ArrayList<Fragment> list){
        super(fm);
        this.fm=fm;
        this.list=list;
    }
    public void clear() {
        for(int i = 0; i < list.size(); i ++) {
            fm.beginTransaction().remove(list.get(i)).commit();
        }

        list.clear();
        notifyDataSetChanged();
    }
    @Override
    public Fragment getItem(int position) {
        return list.get(position);
    }

    @Override
    public int getCount() {
        return list.size();
    }
}
