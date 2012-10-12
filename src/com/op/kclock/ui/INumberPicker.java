package com.op.kclock.ui;

public interface INumberPicker {
	public int getCurrentItem();

	public void setCurrentItem(int index, boolean animated);

	public void setCurrentItem(int index);

	public int getVisibility();

	public void setVisibility(int visibility);

	public void setLayoutParams(android.view.ViewGroup.LayoutParams params);

	public android.view.ViewGroup.LayoutParams getLayoutParams();

}