/**
 *  Kitchen Clock
 *  Copyright (C) 2012 Alexander Pastukhov
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */
package com.op.kclock.full;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;
import com.op.kclock.cookconst.SettingsConst;
import com.op.kclock.full.R;
import com.op.kclock.full.alarm.AlarmSingleServiceImpl;
import com.op.kclock.full.alarm.WakeUpLock;
import com.op.kclock.full.dialogs.NameDialog;
import com.op.kclock.full.dialogs.TimePickDialog;
import com.op.kclock.misc.*;
import com.op.kclock.model.AlarmClock;
import com.op.kclock.model.AlarmClock.TimerState;
import com.op.kclock.ui.TextViewWithMenu;
import com.op.kclock.utils.*;
import java.util.*;
import com.op.kclock.full.alarm.*;

public class MainActivity extends Activity implements OnClickListener,
		OnSharedPreferenceChangeListener {

	private static final String SAVE_SELECTED = "SAVE_SELECTED";
	private static final String SCANER_ACTIVITY = "com.google.zxing.client.android.SCAN";
	private static final int ALPHA_CLOCK = 80;
	public static final String SMALLFIRST = "smallfirst";
	public static final String UNSORTED = "unsorted";
	public static final String RUNNEDFIRST = "runnedfirst";
	private TimePickDialog timePickDialog = null;
	public final static String TAG = "AlarmaClockActivity";
	private static NotificationManager mNotificationManager;
	private SharedPreferences mPrefs;
	private ActionBar actionBar;
	private List<AlarmClock> alarmList = new ArrayList<AlarmClock>();

	private AlarmManager alarmManager;
	
	public final static String ID = "alarm_id";
	public final static String TIME = "alarm_time";
	public final static String LABEL = "alarm_label";
	private static String prefName = "com.op.kclock.full";

	// fling
	public static final int SWIPE_MIN_DISTANCE = 120;
	public static final int SWIPE_MAX_OFF_PATH = 250;
	public static final int SWIPE_THRESHOLD_VELOCITY = 200;
	private GestureDetector gestureDetector;

	private long lastPressTime;
	private static final long DOUBLE_PRESS_INTERVAL = 3000000000l; // value in
																	// ns. (3
																	// sek.)

	// ACTIONBAR actions
	private Action settingsButtonAction;
	private Action delallAction;
	private Action addButtonAction;
	private Action refreshButtonAction;
	private Action presetsButtonAction;
	private Action runAllButtonAction;

	private Handler handler;
	private static Thread thread;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alarmclock_tab);

		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this
				.getApplicationContext());
		if (!mPrefs.getBoolean(SettingsConst.PREF_EULA_ACCEPTED, false)
				&& Utils.isSdPresent()) {
			File prefFile = new File(SettingsConst.SETTINGS_FILE);
			Utils.loadSharedPreferencesFromFile(prefFile,
					this.getApplicationContext());
			mPrefs = PreferenceManager.getDefaultSharedPreferences(this
					.getApplicationContext());
		}

		mPrefs.registerOnSharedPreferenceChangeListener(this);

		initActionBar();

		if (alarmList == null) {
			alarmList = new ArrayList<AlarmClock>();
		}

		Log.d(TAG, "start");
		
		alarmManager = 
            (AlarmManager)getSystemService(Activity.ALARM_SERVICE);
		
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notification();

		if (thread != null ) {
			return;
		}
		
		if (this.getIntent() != null) {
			AlarmClock alarm = (AlarmClock) this.getIntent()
					.getParcelableExtra("alarm_extra");
			if (alarm != null) {

				boolean needAdd = true;
				for (AlarmClock calarm : alarmList) {
					if (calarm.getId() == alarm.getId()) {
						needAdd = false;
						break;
					}
				}
				if (needAdd) {
					alarm.setElement(null);
					alarm.setContext(getApplicationContext());
					deleteAllAlarms(false);
					addAlarm(alarm);
				}
			}
		}

		boolean openedDialogs = false;
		if (!mPrefs.getBoolean(SettingsConst.PREF_EULA_ACCEPTED, false)) {
			Eula.show(this);
			openedDialogs = true;
		}
		if (!mPrefs.getBoolean(SettingsConst.PREF_CHANGELOG, false)) {
			Changelog.show(this);
			openedDialogs = true;
		}

		if (alarmList.size() > 0) {
			drawAlarms();
		} else {
			if (mPrefs.getBoolean(
					getApplicationContext().getString(
							R.string.pref_savesession_key), false)) {
				Log.d(TAG, "db read true!!");
				DBHelper dbHelper = new DBHelper(getApplicationContext());
				alarmList = dbHelper.getAlarmsList();
				// dbHelper.truncateAlarms();
				drawAlarms();
				dbHelper.close();
			}

			if (!openedDialogs
					&& alarmList.size() == 0
					&& mPrefs.getBoolean(
							getApplicationContext().getString(
									R.string.pref_addalarmonstart_key), true)) {
				addAlarmDialog();
			}
		}

		AlarmSingleServiceImpl alarmService = new AlarmSingleServiceImpl(this,
				handler, alarmList);
		if (thread == null
				|| (thread != null && thread.getState() != Thread.State.RUNNABLE)) {
			thread = new Thread(alarmService);
			thread.start();
		}

		gestureDetector = new GestureDetector(new MyGestureDetector());
		View mainview = findViewById(R.id.alarm_layout_main);
		// Set the touch listener for the main view to be our custom gesture
		// listener
		mainview.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				Log.d(TAG, "touch");
				if (gestureDetector.onTouchEvent(event)) {
					return true;
				}
				Log.d(TAG, "no touch");
				return false;
			}
		});

		updateBackGround();
		Log.e(TAG,
				"--"
						+ mPrefs.getBoolean(SettingsConst.PREF_EULA_ACCEPTED,
								false));

	}
	
	

	private void updateBackGround() {
		boolean vLay = getWindowManager().getDefaultDisplay().getHeight() > getWindowManager()
				.getDefaultDisplay().getWidth();

		String bgSRC = mPrefs.getString(
				getApplicationContext().getString(R.string.pref_bgsource_key),
				SettingsActivity.SYSTEM_SOUND_VALUE);
		View mainV = findViewById(R.id.mainScroll);
		if (bgSRC.equals("custom")) {
			String customBG = mPrefs.getString(getApplicationContext()
					.getString(R.string.pref_bgfile_path_key), null);

			if (customBG != null && customBG.length() > 2) {
				BitmapDrawable bitmap = new BitmapDrawable(getResources(),
						customBG);
				mainV.setBackgroundDrawable(bitmap);
			}
		} else if (bgSRC.equals("system")) {
			mainV.setBackgroundResource(R.drawable.bg_wood);
		} else if (bgSRC.equals("strawberry")) {
			if (vLay) {
				mainV.setBackgroundResource(R.drawable.bg_strawberry_v);
			} else {
				mainV.setBackgroundResource(R.drawable.bg_strawberry_h);
			}
		} else if (bgSRC.equals("coffee")) {
			if (vLay) {
				mainV.setBackgroundResource(R.drawable.bg_coffee_v);
			} else {
				mainV.setBackgroundResource(R.drawable.bg_coffee_h);
			}
		} else if (bgSRC.equals("garlic")) {
			if (vLay) {
				mainV.setBackgroundResource(R.drawable.bg_garlic_v);
			} else {
				mainV.setBackgroundResource(R.drawable.bg_garlic_h);
			}
		} else if (bgSRC.equals("greenpeas")) {
			if (vLay) {
				mainV.setBackgroundResource(R.drawable.bg_greenpeas_v);
			} else {
				mainV.setBackgroundResource(R.drawable.bg_greenpeas_h);
			}
		} else if (bgSRC.equals("spaghetti")) {
			if (vLay) {
				mainV.setBackgroundResource(R.drawable.bg_spaghetti_v);
			} else {
				mainV.setBackgroundResource(R.drawable.bg_spaghetti_h);
			}
		}
	}

	private void initActionBar() {
		actionBar = (ActionBar) findViewById(R.id.actionbar);
		actionBar
				.setTitle(getApplicationContext().getString(R.string.app_name));

		settingsButtonAction = new IntentAction(this, new Intent(this,
				SettingsActivity.class), R.drawable.ic_menu_preferences);
		presetsButtonAction = new IntentAction(this, new Intent(this,
				PresetsActivity.class), R.drawable.ic_menu_list);
		delallAction = new Action() {
			@Override
			public void performAction(View view) {
				deleteAllAlarms(true);
			}

			@Override
			public int getDrawable() {
				return R.drawable.ic_menu_delete;
			}
		};
		addButtonAction = new Action() {
			@Override
			public void performAction(View view) {
				addAlarmDialog();
			}

			@Override
			public int getDrawable() {
				return R.drawable.ic_menu_add;
			}
		};
		refreshButtonAction = new Action() {
			@Override
			public void performAction(View view) {
				refreshAllAlarms();
			}

			@Override
			public int getDrawable() {
				return R.drawable.ic_menu_refresh;
			}
		};
		runAllButtonAction = new Action() {
			@Override
			public void performAction(View view) {
				runAllTimers();
			}

			@Override
			public int getDrawable() {
				return R.drawable.ic_menu_play;
			}
		};

		if (mPrefs
				.getBoolean(
						getApplicationContext().getString(
								R.string.pref_showaddbtn_key), true)) {
			actionBar.addAction(addButtonAction);
		}
		if (mPrefs.getBoolean(
				getApplicationContext()
						.getString(R.string.pref_showsettbtn_key), false)) {
			actionBar.addAction(settingsButtonAction);
		}
		if (mPrefs
				.getBoolean(
						getApplicationContext().getString(
								R.string.pref_showdelall_key), false)) {
			actionBar.addAction(delallAction);
		}
		if (mPrefs.getBoolean(
				getApplicationContext().getString(
						R.string.pref_showrefreshbtn_key), false)) {
			actionBar.addAction(refreshButtonAction);
		}
		if (mPrefs.getBoolean(
				getApplicationContext().getString(
						R.string.pref_showpresetsbtn_key), true)) {
			actionBar.addAction(presetsButtonAction);
		}
		if (mPrefs.getBoolean(
				getApplicationContext()
						.getString(R.string.pref_showplaybtn_key), true)) {
			actionBar.addAction(runAllButtonAction);
		}

	}

	/**
	 * Initial icon
	 */
	public void notification() {
		int icon = R.drawable.stat_notify_alarm;
		CharSequence mTickerText = getString(R.string.timer_started);
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, mTickerText, when);
		CharSequence mContentTitle = getString(R.string.app_name);
		CharSequence mContentText = getString(R.string.click_to_open);
		Intent clickIntent = new Intent(this, MainActivity.class);
		clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(getApplicationContext(), mContentTitle,
				mContentText, contentIntent);
		notification.ledARGB = 0x00000000;
		notification.ledOnMS = 0;
		notification.ledOffMS = 0;
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;

		mNotificationManager.notify(SettingsConst.APP_NOTIF_ID, notification);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		updateBackGround();
		int height = getWindowManager().getDefaultDisplay().getHeight();
		for (AlarmClock alarm : alarmList) {
			alarm.updateAlarmSize();
		}
		if (timePickDialog != null && timePickDialog.isShowing()) {
			LinearLayout subscr = (LinearLayout) timePickDialog
					.findViewById(R.id.pick_text);
			if (height < 500) {
				subscr.setVisibility(View.GONE);
			} else {
				subscr.setVisibility(View.VISIBLE);
			}
		}

		stopAllActiveAlarms();
	}

	public void stopAllActiveAlarms() {
		if (mPrefs.getBoolean(
				getApplicationContext().getString(
						R.string.pref_stoponrotate_key), true)) {
			for (AlarmClock alarm : alarmList) {
				if (alarm.getState() == AlarmClock.TimerState.ALARMING) {
					alarm.alarmSTOP();
				}
			}
		}
	}

	protected void runAllTimers() {
		for (AlarmClock alarm : alarmList) {
			if (alarm.getState() != AlarmClock.TimerState.ALARMING
					&& alarm.getTime() > 0) {
				alarm.setState(TimerState.RUNNING);
			}
		}
	}

	// Store the instance of an object
	@Override
	public Object onRetainNonConfigurationInstance() {

		Log.d(TAG, "retain");

		if (alarmList != null) // Check that the object exists
			return (alarmList);
		return super.onRetainNonConfigurationInstance();
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "MainActivity: onStart()");
		turnOnValue();
	}



	private void turnOnValue() {
		//turn on sound
		if (mPrefs.getBoolean(
			   getApplicationContext().getString(
						R.string.pref_overridevolume_key), true)) {
			AudioManager audioManager = 
				    (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < 2){
				audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
				                             10, 0);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		turnOnValue();


		if (alarmList.size() == 0) {
			if (mPrefs.getBoolean(SettingsConst.PREF_EULA_ACCEPTED, false)
					&&

					mPrefs.getBoolean(
							getApplicationContext().getString(
									R.string.pref_addalarmonstart_key), true)) {

				addAlarmDialog();
			}
		}
		
		for (AlarmClock alarm : alarmList) {
			alarm.updateAlarmSize();
		}
		Log.d(TAG, "MainActivity: onResume()");
	}

	private boolean isTimerActive() {
		for (AlarmClock alarm : alarmList) {
			if (alarm.getState() == AlarmClock.TimerState.RUNNING
					&& alarm.getTime() > 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (isTimerActive()) {
			Log.e(TAG, "pause() active");
		}

		if (mPrefs.getBoolean(
				getApplicationContext()
						.getString(R.string.pref_savesession_key), false)) {
			DBHelper dbHelper = new DBHelper(getApplicationContext());
			dbHelper.open();
			dbHelper.truncateAlarms();
			for (AlarmClock alarm : alarmList) {
				dbHelper.insertAlarm(alarm);
			}
			dbHelper.close();
		}

		Log.d(TAG, "MainActivity: onPause()");
	}

	@Override
	protected void onStop() {
		super.onStop();
		// save to db
		Log.d(TAG, "MainActivity: onStop()");
	}

	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.d(TAG, "onrestore " + " alarmList: " + alarmList);
		super.onRestoreInstanceState(savedInstanceState);
		alarmList = savedInstanceState.getParcelableArrayList(SAVE_SELECTED);
		for(AlarmClock alarm : alarmList){
			alarm.setElement(null);
		}
		drawAlarms();
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		Log.d(TAG, "onsave" + " alarmList: " + alarmList);
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putParcelableArrayList(SAVE_SELECTED,(ArrayList<? extends Parcelable>) alarmList);
	}

	private void drawAlarms() {
		// sort
		Log.d(TAG, "draw" + " alarmList: " + alarmList);
		
		
		String sortType = mPrefs.getString(
				getApplicationContext().getString(R.string.pref_sortlist_key),
				UNSORTED);
		if (sortType.equals(RUNNEDFIRST)) {
			AlarmClock.ActiveFirstComparator comparator = new AlarmClock.ActiveFirstComparator();
			Collections.sort(alarmList, comparator);
		} else if (sortType.equals(SMALLFIRST)) {
			AlarmClock.NearestActiveFirstComparator comparator = new AlarmClock.NearestActiveFirstComparator();
			Collections.sort(alarmList, comparator);
		}

		Log.d(TAG, "SORTED" + " alarmList: " + alarmList);
		for (AlarmClock alarm : alarmList) {
			drawAlarm(alarm);
		}
	}

	private void addAlarm(AlarmClock newAlarm) {

		//Create an offset from the current time in which the alarm will go off.
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, (int) newAlarm.getTime());

        //Create a new PendingIntent and add it to the AlarmManager
        Intent intent = this.getIntent();//new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
											12345, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
			   pendingIntent);
		newAlarm.setPendingIntent(pendingIntent);
		
		if (newAlarm.getElement() == null) {
			drawAlarm(newAlarm);
			if (newAlarm.getElement() != null) {

				alarmList.add(newAlarm);
				String sortType = mPrefs.getString(getApplicationContext()
						.getString(R.string.pref_sortlist_key), UNSORTED);
				if (sortType != UNSORTED) {
					LinearLayout mainL = (LinearLayout) findViewById(R.id.alarm_layout);
					mainL.removeAllViews();
					drawAlarms();
				}
			}

		} else {
			updatePreset(newAlarm);
		}
	}

	private void updatePreset(AlarmClock newAlarm) {
		Log.e(TAG, "updatepreset");
		if (newAlarm.isPreset()) {
			Log.e(TAG, "is preset");
			DBHelper dbHelper = new DBHelper(getApplicationContext());
			dbHelper.updatePreset(newAlarm);
		}
	}

	private LinearLayout drawAlarm(AlarmClock alarm) {
		LinearLayout mainL = (LinearLayout) findViewById(R.id.alarm_layout);
		// boolean isnew = false;
		Log.d(TAG, "drawalarm");
		if (alarm.getElement() == null) {
			// isnew = true;
			LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			LinearLayout itemView = (LinearLayout) inflater.inflate(
					R.layout.alarm_incl, null);
			alarm.setElement(itemView);

			mainL.addView(alarm.getElement(),// mainL.getChildCount() - 1
					new TableLayout.LayoutParams(LayoutParams.FILL_PARENT,
							LayoutParams.WRAP_CONTENT));
			TextViewWithMenu textView = (TextViewWithMenu) (alarm.getWidget());
			textView.setAlarm(alarm);
		} else {
			if (alarm.getElement().getParent() == null) {
				mainL.addView(alarm.getElement(), new TableLayout.LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			}
		}
		alarm.updateElement();
		alarm.getElement().setOnClickListener(this);
		alarm.getWidget().getBackground().setAlpha(ALPHA_CLOCK);

		if (!mPrefs.getBoolean(
				getApplicationContext().getString(R.string.pref_shownames_key),
				false)) {
			final TextView widgetLbl = (TextView) alarm.getElement()
					.getChildAt(0);
			widgetLbl.setVisibility(View.INVISIBLE);
		}

		if (alarm.getState() == AlarmClock.TimerState.STOPPED) {
			if (mPrefs.getBoolean(
					getApplicationContext().getString(
							R.string.pref_autostart_key), true)) {
				if (alarm.getState() != AlarmClock.TimerState.PAUSED
						&& alarm.getTime() > 0) {
					alarm.setState(AlarmClock.TimerState.RUNNING);
				}
			} else {
				alarm.setState(AlarmClock.TimerState.PAUSED);
			}
		}

		registerForContextMenu(alarm.getWidget());
		addListenerForName(alarm);
		// add the itemView
		alarm.updateState();

		if (alarm.getElement() == null)
			throw new IllegalArgumentException();
		
		alarm.updateAlarmSize();

		return alarm.getElement();
	}

	private void addListenerForName(final AlarmClock alarm) {
		final TextView childAt = (TextView) alarm.getElement().getChildAt(0);
		// On long click on preset - show edit dialog
		childAt.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				NameDialog nameDialog = null;
				nameDialog = new NameDialog(MainActivity.this, alarm);
				nameDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				nameDialog.setDialogResult(new NameDialog.OnMyDialogResult() {
					public void finish(AlarmClock newAlarm) {
						childAt.setText(alarm.getName());
						DBHelper dbHelper = new DBHelper(
								getApplicationContext());
						dbHelper.updatePreset(alarm);
					}
				});
				nameDialog.show();
				return true;
			}
		});

		childAt.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				Log.d(TAG, "touch2");
				if (gestureDetector.onTouchEvent(event)) {
					return false;
				}
				Log.d(TAG, "no touch2");
				return false;
			}
		});
	}


	// ON-CLICK

	public void onClick(View v) {

		for (AlarmClock alarm : alarmList) {
			TextViewWithMenu tvTimer = (TextViewWithMenu) alarm.getElement()
					.getChildAt(1);
			if (tvTimer == v) {
				if (alarm.getState() == AlarmClock.TimerState.RUNNING) {
					alarm.setState(AlarmClock.TimerState.PAUSED);
				} else if (alarm.getState() == AlarmClock.TimerState.PAUSED) {
					alarm.setState(AlarmClock.TimerState.RUNNING);
				} else if (alarm.getState() == AlarmClock.TimerState.ALARMING) {
					alarm.alarmSTOP();
				}
				break;
			}
		}

		String sortType = mPrefs.getString(
				getApplicationContext().getString(R.string.pref_sortlist_key),
				UNSORTED);
		if (sortType != UNSORTED) {
			LinearLayout mainL = (LinearLayout) findViewById(R.id.alarm_layout);
			mainL.removeAllViews();
			drawAlarms();
		}
	}

	// ============================================================
	// ==================== MENUS ===============================
	// ============================================================

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(this).inflate(R.menu.alarm_clock_options, menu);
		return (super.onCreateOptionsMenu(menu));
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();

		inflater.inflate(R.menu.alarm_context, menu);
		super.onCreateContextMenu(menu, v, menuInfo);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings: {
			goSettings();
			return true;

		}
		case R.id.menu_lookcode: {
			lookForBarcode();
			return true;
		}
		case R.id.menu_add: {
			addAlarmDialog();
			return true;
		}
		case R.id.menu_delete_all: {
			deleteAllAlarms(true);
			return true;
		}
		case R.id.menu_exit: {
			exit();
		}

		}
		return false;
	}

	private void exit() {
		mNotificationManager.cancelAll();
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
		finish();
	}

	private Intent goSettings() {
		Intent i3 = new Intent(this, SettingsActivity.class);
		startActivity(i3);
		return i3;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		TextViewWithMenu.TextViewMenuInfo menuInfo = (TextViewWithMenu.TextViewMenuInfo) item
				.getMenuInfo();
		TextViewWithMenu text = (TextViewWithMenu) menuInfo.targetView;

		switch (item.getItemId()) {
		case R.id.settime: {
			setAlarmDialog(text.getAlarm());
			return true;
		}
		case R.id.addpreset: {
			addPreset(text);
			Toast toast = Toast.makeText(getApplicationContext(),
					getString(R.string.presets_saved), Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
			toast.show();
			return true;
		}
		case R.id.assigncode: {
			assignCode(text);
			return true;
		}
		case R.id.remove: {
			deleteAlarm(text);
			return true;
		}
		}
		return super.onContextItemSelected(item);
	}

	private void assignCode(TextViewWithMenu text) {
		Intent intent2 = new Intent(SCANER_ACTIVITY);
		intent2.putExtra("SCAN_MODE", "ONE_D_MODE");
		for (final AlarmClock alarm : alarmList) {
			if (alarm.getElement() != null
					&& alarm.getElement().getChildAt(1) == (TextViewWithMenu) text) {
				try {
					startActivityForResult(intent2, alarm.hashCode());
				} catch (Exception e) {
					Toast toast = Toast.makeText(getApplicationContext(),
							getString(R.string.installzxingscan),
							Toast.LENGTH_LONG);
					toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
					toast.show();
				}
				break;
			}
		}
	}

	private void addPreset(TextViewWithMenu text) {
		DBHelper dbHelper = new DBHelper(getApplicationContext());
		if (SettingsConst.APP_FULL
				|| dbHelper.getPresetsList().size() < SettingsConst.MAX_PRESETS) {

			for (final AlarmClock alarm : alarmList) {
				if (alarm.getElement() != null
						&& alarm.getElement().getChildAt(1) == (TextViewWithMenu) text) {
					dbHelper.insertPreset(alarm);
					alarm.setPreset(true);
				}
			}
		} else {
			Toast toast = Toast.makeText(this, getString(R.string.liteallow),
					Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
			toast.show();
		}
		
        refreshPresets();
		
        
	}



	private void refreshPresets() {
		PresetsFragment fragment = (PresetsFragment) getFragmentManager()
                .findFragmentById(R.id.lpresetsf);
        final LinearLayout presetsList = (LinearLayout) fragment.getView().findViewById(R.id.presets_list);
        presetsList.refreshDrawableState();
        fragment.getActivity().finish();
        fragment.startActivity(fragment.getActivity().getIntent());
	}

	// ============================================================
	// ==================== END MENUS ===============================
	// ============================================================
	private void deleteAllAlarms(boolean allowDialog) {

			Log.e(TAG,"del all" );
		DBHelper dbHelper = new DBHelper(getApplicationContext());
		dbHelper.open();
		Log.e(TAG,"del all 2" );
		for (final AlarmClock alarm : alarmList) {

			if (alarm.getElement() != null)
				alarm.getElement().setVisibility(View.GONE);
			if (alarm.getInitSeconds() > 0)
				dbHelper.insertHistory(alarm);
			if (alarm.getId() > 0) {
				dbHelper.deleteAlarm(alarm.getId());
			}
			
			alarm.alarmSTOP();
			alarm.setElement(null); // TODO clean!
			alarm.setTime(-1);// used for debug. ensure thay deleted
			
			Log.e(TAG,"aaa" + alarm.getPendingIntent() );
			if (alarm.getPendingIntent() != null) alarmManager.cancel(alarm.getPendingIntent());
		}
		dbHelper.close();
		alarmList.clear();
		

		if (allowDialog
				&& mPrefs.getBoolean(
						getApplicationContext().getString(
								R.string.pref_addalarmonstart_key), true)) {
			addAlarmDialog();
		}

	}

	/**
	 */
	private void refreshAllAlarms() {
		for (final AlarmClock alarm : alarmList) {
			if (alarm.getState().equals(AlarmClock.TimerState.ALARMING)) {
				alarm.alarmSTOP();
			}
			alarm.setState(AlarmClock.TimerState.PAUSED);
			alarm.restart();
			alarm.updateElement();
		}

	}

	private void deleteAlarm(final TextViewWithMenu text) {
		// AlarmClock nalarm = null;
		for (final AlarmClock alarm : alarmList) {
			if (alarm.getElement() != null
					&& alarm.getElement().getChildAt(1) == (TextViewWithMenu) text) {
				// final AlarmClock falarm = alarm;
				Animation hyperspaceJump = AnimationUtils.loadAnimation(this,
						R.anim.hsjump);
				hyperspaceJump
						.setAnimationListener(new Animation.AnimationListener() {
							@Override
							public void onAnimationEnd(Animation arg0) {
								alarmList.remove(alarm);
								DBHelper dbHelper = new DBHelper(
										getApplicationContext());
								dbHelper.open();
								if (alarm.getInitSeconds() > 0)
									dbHelper.insertHistory(alarm);
								dbHelper.close();

								if (alarm.getState().equals(
										AlarmClock.TimerState.ALARMING))
									alarm.alarmSTOP();
								alarm.setState(AlarmClock.TimerState.STOPPED);
								if (alarm.getElement() != null)
									alarm.getElement().setVisibility(View.GONE);
								if (alarm.getId() > 0) {
									DBHelper alarmClockDAO = new DBHelper(
											getApplicationContext());
									alarmClockDAO.open();
									alarmClockDAO.deleteAlarm(alarm.getId());
									alarmClockDAO.close();

								}
								alarm.setTime(-1);
								alarm.setElement(null); // TODO clean!
								// mainL.removeView(((LinearLayout)alarm.getElement()

							}

							@Override
							public void onAnimationStart(Animation animation) {
							}

							public void onAnimationRepeat(Animation animation) {
							}

						});

				alarm.getElement().startAnimation(hyperspaceJump);

				break;
			}
		}
	}

	// ************************** GET SET *************************
	public TimePickDialog getTimePickDialog() {
		return timePickDialog;
	}

	public List<AlarmClock> getAlarmList() {
		return alarmList;
	}

	// ************************* end Getters Setters ************

	private void addAlarmDialog() {
		if (SettingsConst.APP_FULL
				|| alarmList.size() < SettingsConst.MAX_TIMERS) {
			if (timePickDialog == null
					|| (timePickDialog != null && !timePickDialog.isShowing())) {
				setAlarmDialog(null);
			}
		} else {
			Toast toast = Toast.makeText(this, getString(R.string.liteallow),
					Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
			toast.show();
		}
	}

	private void setAlarmDialog(AlarmClock alarm) {

		timePickDialog = new TimePickDialog(MainActivity.this);
		timePickDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		timePickDialog.setAlarm(alarm);

		timePickDialog.setDialogResult(new TimePickDialog.OnMyDialogResult() {
			public void finish(AlarmClock newAlarm) {
				addAlarm(newAlarm);
				newAlarm.updateElement();
				if (newAlarm.getState() == AlarmClock.TimerState.STOPPED) {
					drawAlarm(newAlarm);
				}
			}
		});
		timePickDialog.show();
	}

	/*	*//**
	 * Creates new MusicHandler and starts playing, Also creates
	 * ShowStopper.
	 */
	/*
	 * private void playMusic() { music = new MusicHandler();
	 * music.setMusic(this); music.play(true);
	 * 
	 * ShowStopper stopper = new
	 * ShowStopper(PreferenceService.getAlarmLength(this), music, vibrator);
	 * showStopperThread = new Thread(stopper); showStopperThread.start();
	 * 
	 * }
	 */

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (key.equals("pref_shownames_key")) {
			for (AlarmClock alarm : alarmList) {
				final TextView widgetLbl = (TextView) alarm.getElement()
						.getChildAt(0);
				if (!mPrefs.getBoolean(
						getApplicationContext().getString(
								R.string.pref_shownames_key), false)) {
					widgetLbl.setVisibility(View.INVISIBLE);
				} else {
					widgetLbl.setVisibility(View.VISIBLE);
				}
			}
		} else if (key.equals("pref_showsettbtn_key")) {
			if (mPrefs.getBoolean(
					getApplicationContext().getString(
							R.string.pref_showsettbtn_key), false)) {
				actionBar.addAction(settingsButtonAction);
			} else {
				actionBar.removeAction(settingsButtonAction);
			}
		} else if (key.equals("pref_showdelall_key")) {
			if (mPrefs.getBoolean(
					getApplicationContext().getString(
							R.string.pref_showdelall_key), false)) {
				actionBar.addAction(delallAction);
			} else {
				actionBar.removeAction(delallAction);
			}
		} else if (key.equals("pref_showaddbtn_key")) {
			if (mPrefs.getBoolean(
					getApplicationContext().getString(
							R.string.pref_showaddbtn_key), true)) {
				actionBar.removeAction(addButtonAction);
				actionBar.addAction(addButtonAction, 0);
			} else {
				actionBar.removeAction(addButtonAction);
			}
		} else if (key.equals("pref_showrefreshbtn_key")) {
			if (mPrefs.getBoolean(
					getApplicationContext().getString(
							R.string.pref_showrefreshbtn_key), false)) {
				actionBar.addAction(refreshButtonAction);
			} else {
				actionBar.removeAction(refreshButtonAction);
			}
		} else if (key.equals("pref_showpresetsbtn_key")) {
			if (mPrefs.getBoolean(
					getApplicationContext().getString(
							R.string.pref_showpresetsbtn_key), false)) {
				actionBar.addAction(presetsButtonAction);
			} else {
				actionBar.removeAction(presetsButtonAction);
			}
		} else if (key.equals("pref_showplaybtn_key")) {
			if (mPrefs.getBoolean(
					getApplicationContext().getString(
							R.string.pref_showplaybtn_key), false)) {
				actionBar.addAction(runAllButtonAction);
			} else {
				actionBar.removeAction(runAllButtonAction);
			}
		} else if (key.equals("pref_sortlist_key")) {
			String sortType = mPrefs.getString(getApplicationContext()
					.getString(R.string.pref_sortlist_key), UNSORTED);
			if (sortType != UNSORTED) {
				LinearLayout mainL = (LinearLayout) findViewById(R.id.alarm_layout);
				mainL.removeAllViews();
				drawAlarms();
			}

		} else if (key.equals("pref_disablelock_key")) {
			if (mPrefs.getBoolean(
					getApplicationContext().getString(
							R.string.pref_disablelock_key), false)) {
				WakeUpLock.acquire(getApplicationContext());
			} else {
				WakeUpLock.release();
			}
		} else if (key.equals("pref_bgfile_path_key")
				|| key.equals("pref_bgsource_key")) {
			updateBackGround();
		}
	}

	/**
	 * Finishes the activity, also closes the various things started by
	 * onCreate.
	 */
	@Override
	public void finish() {
		Log.v(TAG, "finish");
	}

	private void lookForBarcode() {
		Log.v(TAG, "fling 5");
		Intent intent2 = new Intent(SCANER_ACTIVITY);
		intent2.putExtra("SCAN_MODE", "ONE_D_MODE");
		try {
			startActivityForResult(intent2, 0);
		} catch (Exception e) {
			Toast toast = Toast.makeText(getApplicationContext(),
					getString(R.string.installzxingscan), Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
			toast.show();
		}
		MainActivity.this.overridePendingTransition(R.anim.slide_in_left,
				R.anim.slide_out_right);
	}

	class MyGestureDetector extends SimpleOnGestureListener {

//        @Override
//        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//            if(Math.abs(distanceY) > Math.abs(distanceX)) {
//                 return false;
//            }
//            return true;
//        }
	    
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			Intent intent = new Intent(MainActivity.this.getBaseContext(),
					PresetsActivity.class);

			try {
				if (e1 == null || e2 == null
						|| Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
					return false;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}

			// left to right swipe
			if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				Log.v(TAG, "fling 3");
				startActivity(intent);
				MainActivity.this.overridePendingTransition(
						R.anim.slide_in_right, R.anim.slide_out_left);
				// right to left swipe
			} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				Log.v(TAG, "fling 4");
				lookForBarcode();
			}

			return false;
		}

		// It is necessary to return true from onDown for the onFling event to
		// register
		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}

	}

	public boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(
				intent, PackageManager.MATCH_DEFAULT_ONLY);
		if (resolveInfo.size() > 0) {
			ResolveInfo ri = resolveInfo.iterator().next();
			if (ri.activityInfo.applicationInfo.className != null)
				return true;
		}
		return false;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode != 670) {
			if (resultCode == RESULT_OK) {
				String contents = intent.getStringExtra("SCAN_RESULT");
				// Handle successful scan
				Log.d(TAG, "extra" + requestCode);
				if (requestCode != 0) {
					for (AlarmClock alarm : alarmList) {
						if (alarm.hashCode() == requestCode) {
							alarm.setSCode(contents);
							// if its preset
							updatePreset(alarm);
							DBHelper dbHelper = new DBHelper(
									getApplicationContext());
							dbHelper.open();
							dbHelper.insertPreset(alarm);
							dbHelper.close();
							alarm.setPreset(true);
							return;
						}
					}
					Toast toast = Toast.makeText(this,
							getString(R.string.dcodenotfound),
							Toast.LENGTH_LONG);
					toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
					toast.show();
					return;
				} else {
//					try {
//						getProductFromGlobalDB(contents);
//					} catch (ClientProtocolException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					} catch (JSONException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}

					DBHelper dbHelper = new DBHelper(getApplicationContext());
					// select min alarm and make caller
					AlarmClock alarm = dbHelper.presetBySCode(contents);
					if (alarm != null) {
						deleteAllAlarms(false);
						addAlarm(alarm);
						alarm.setState(AlarmClock.TimerState.PAUSED);

					} else {
						Toast toast = Toast.makeText(this,
								getString(R.string.dcodenotfound),
								Toast.LENGTH_LONG);
						toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
						toast.show();
					}
				}
				Log.d(TAG, "ct." + contents);
			} else if (resultCode == RESULT_CANCELED) {
				// Handle cancel
			}
		}
	}

	final String URL_GET_PRODUCT_BY_BARCODE = 
			//"http://api.flickr.com/services/rest/?method=flickr.test.echo&api_key=7bd34ffecaa99671dd679e6a4dd71c29&format=json&nojsoncallback=1&api_sig=93bd49f3f340bf4772050e1d85f35b9c";
			
			"http://cookapp-cloud.cloudfoundry.com/barcode/";

	private JSONObject getProductFromGlobalDB(String contents)
			throws ClientProtocolException, IOException, JSONException {
		StringBuilder fullUrl = new StringBuilder(URL_GET_PRODUCT_BY_BARCODE);
		fullUrl.append(contents);
		HttpClient client = new DefaultHttpClient();
		
		Log.e(TAG,"getProd");

		HttpContext localContext = new BasicHttpContext();
		
		HttpGet get = new HttpGet(fullUrl.toString());
		HttpResponse response = client.execute(get, localContext);
		//HttpResponse response = client.execute(get);
		//client.
		int statusCode = response.getStatusLine().getStatusCode();
		Log.e(TAG,"getProdstatus" + statusCode);
		if (statusCode == 200) {
			HttpEntity entity = response.getEntity();
			String json = EntityUtils.toString(entity);
			//JSONArray bunchOfTweets = new JSONArray(json);
			JSONObject mostRecentTweet = new JSONObject(json);//bunchOfTweets.getJSONObject(0);
			return mostRecentTweet;
		} else {
			String reason = response.getStatusLine().getReasonPhrase();
			throw new RuntimeException("Trouble reading status(code="
					+ statusCode + "):" + reason);
		}

	}

	@Override
	public void onBackPressed() {
		Log.v(TAG, "onBackPressed() called");
		long pressTime = System.nanoTime();
		if (pressTime - lastPressTime <= DOUBLE_PRESS_INTERVAL) {
			// this is a double click event
			super.onBackPressed();
			exit();
		} else {
			Toast.makeText(this, getString(R.string.toast_press_back),
					Toast.LENGTH_SHORT).show();
		}
		lastPressTime = pressTime;
	}

}
