package com.op.kclock.full;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AlarmsFragment extends Fragment {
	  
	@Override
	  public View onCreateView(LayoutInflater inflater, ViewGroup container,
	      Bundle savedInstanceState) {
	    View view = inflater.inflate(R.layout.alarmclock,
	        container, false);
	    return view;
	  }

	  public void setText(String item) {
	    //TextView view = (TextView) getView().findViewById(R.id.detailsText);
	    //view.setText(item);
	  }
} 