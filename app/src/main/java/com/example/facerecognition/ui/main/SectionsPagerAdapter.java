package com.example.facerecognition.ui.main;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;



import java.util.List;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    private String[] TAB_TITLES;
    private final Context mContext;

    private List<List<Bitmap>> tabList;

    List<double[]> similarities;

    public SectionsPagerAdapter(Context context, FragmentManager fm, List<List<Bitmap>> tabList,List<double[]> similarities) {
        super(fm);
        mContext = context;
        this.tabList = tabList;
        this.similarities = similarities;
        initialTAB_TITLES(tabList);
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        return PlaceholderFragment.newInstance(position + 1,tabList.get(position),similarities.get(position));
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return  TAB_TITLES[position];
        //return mContext.getResources().getString(TAB_TITLES[position]);
    }


    @Override
    public int getCount() {
        // Show 2 total pages.
        return TAB_TITLES.length;
    }

    private void initialTAB_TITLES(List<List<Bitmap>> tabList){
        if(TAB_TITLES == null)
            TAB_TITLES = new String[tabList.size()];
        for(int i = 0;i<tabList.size();++i){
            TAB_TITLES[i] = "匹配结果"+(i+1);
        }
    }
}