package com.jamie.picturestory;

import java.util.ArrayList;

import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.view.ViewGroup;

public class ListPagerAdapter extends FragmentStatePagerAdapter {
	private ArrayList<Uri> mPageUris;
	private boolean itemRemoved;
	
	ListPagerAdapter(FragmentManager fm) {
		super(fm);
		mPageUris = new ArrayList<Uri>();
	}
	
	ListPagerAdapter(ArrayList<Uri> pageUris, FragmentManager fm) {
		super(fm);
		mPageUris = pageUris;
	}
	
	@Override
	public Fragment getItem(int position) {
		return ImageFragment.newInstance(mPageUris.get(position));
	}
	
	@Override
	public int getCount() {
		return mPageUris.size();
	}
	
	public void addItem(int position, Uri pageUri) {
		mPageUris.add(position, pageUri);
		itemRemoved = false;
		notifyDataSetChanged();
	}
	
	public boolean removeItem(int position) {
		if (position < 0 || position >= mPageUris.size()) {
			return false;
		}
		
		mPageUris.remove(position);
		itemRemoved = true;
		notifyDataSetChanged();
		return true;
	}
	
	@Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        // Cancel any ongoing work on the fragment
		final ImageFragment fragment = (ImageFragment) object;
        fragment.cancelWork();
        
        super.destroyItem(container, position, object);
    }
	
	@Override
	public int getItemPosition(Object object) {
		if (itemRemoved) {
			return PagerAdapter.POSITION_NONE;
		} else {
			return super.getItemPosition(object);
		}
	}
	
	public ArrayList<Uri> getPageUris() {
		return mPageUris;
	}
}