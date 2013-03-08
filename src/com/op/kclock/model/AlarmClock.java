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
package com.op.kclock.model;

import java.util.ArrayList;

import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import java.util.Comparator;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.op.kclock.cookconst.SettingsConst;
import com.op.kclock.full.R;
import com.op.kclock.full.MainActivity;
import com.op.kclock.full.SettingsActivity;
import com.op.kclock.full.alarm.WakeUpLock;

import android.provider.*;

public class AlarmClock implements Parcelable {

	private static final int MAX_ALARM_TSIZE = 150;
	
	private static final int DEF_TEXT_SIZE = 66;
	
	private static final int MINUTE = 60;

	private static final int HOUR = 3600;

	private static final String TAG = null;

	private int id;

	private String name;
	
	private static String ns = Context.NOTIFICATION_SERVICE;

	private long seconds;

	private long initSeconds;
	
	private Context context;
	
	private boolean pinned;

	private boolean active;

	private int usageCnt;

	private int dateAdd;
	
	private String sound;
	
	private String sCode;
	
	//this alarm loaded from presets?
	private boolean preset = false;

	private LinearLayout element;

	private NotificationManager mNotificationManager;

	public static enum TimerState {
		STOPPED, PAUSED, RUNNING, ALARMING
		};

	private TimerState state = TimerState.STOPPED;
	
	public void setPreset(boolean preset)
	{
		this.preset = preset;
	}

	public boolean isPreset()
	{
		return preset;
	}
	
	public void setSCode(String sCode)
	{
		this.sCode = sCode;
	}

	public String getSCode()
	{
		return sCode;
	}

	public void setSound(String sound)
	{
		this.sound = sound;
	}

	public String getSound()
	{
		return sound;
	}

	
	
	public boolean isPinned() {
		return pinned;
	}

	public void setPinned(boolean pinned) {
		this.pinned = pinned;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public int getUsageCnt() {
		return usageCnt;
	}

	public void setUsageCnt(int usageCnt) {
		this.usageCnt = usageCnt;
	}

	public int getDateAdd() {
		return dateAdd;
	}

	public void setDateAdd(int dateAdd) {
		this.dateAdd = dateAdd;
	}

	public long getInitSeconds() {
		return initSeconds;
	}

	public void setInitSeconds(long initSeconds) {
		this.initSeconds = initSeconds;
	}


	public TimerState getState() {
		return state;
	}

	public AlarmClock(LinearLayout _element) {
		element = _element;
	}

	public void setText( String text) {
		getWidget().setText(text);
	}

	public void setState( TimerState state) {

		if (element != null && this.state != state) {
			this.state = state;
			Animation in = AnimationUtils
					.loadAnimation(context, R.anim.fade_in);
			Animation out = AnimationUtils.loadAnimation(context,
					R.anim.fade_out);

			if (state == AlarmClock.TimerState.RUNNING) {
				getWidget().startAnimation(in);
				getWidget().setTextColor(
						context.getResources().getColor(R.color.white));
			} else if (getState() == AlarmClock.TimerState.PAUSED) {
				getWidget().startAnimation(out);
				getWidget().setTextColor(
						context.getResources().getColor(R.color.gray));
			} else if (getState() == AlarmClock.TimerState.STOPPED) {
				getWidget().startAnimation(out);
				getWidget().setTextColor(
						context.getResources().getColor(R.color.gray));
				setState(TimerState.PAUSED);
				restart();
				this.updateElement();
			}
		}
	}

	public AlarmClock(String _name, int _id, Context _context) {
		this(_context);
		name = _name;
		id = _id;
	}

	public AlarmClock(Context _context) {
		context = _context;
		mNotificationManager = (NotificationManager) context
				.getSystemService(ns);
	}
	
	public void setContext(Context _context) {
		context = _context;
		mNotificationManager = (NotificationManager) context
				.getSystemService(ns);
	}

	/**
	 * Change size of alarm element
	 * @param alarm
	 */
	public void updateAlarmSize() {

		final LinearLayout ll = ((LinearLayout)getElement().getParent().getParent());
		
		ll.getViewTreeObserver().addOnGlobalLayoutListener(
		        new OnGlobalLayoutListener() {

		            @Override
		            public void onGlobalLayout() {
		                int width = ll.getWidth();
//		        		Log.d(TAG, "updateAlarmSize()" + " width: " + width);
		        		final float densityMultiplier = context.getResources()
		        				.getDisplayMetrics().density;
		        		Paint paint = new Paint();
		        		final float scaledPx = DEF_TEXT_SIZE * densityMultiplier;
		        		paint.setTextSize(scaledPx);
		        		final float size = paint.measureText("00:00:00");
		        		int tsize = (int) ((DEF_TEXT_SIZE * (width / size) - 10)/1.3);
		        		if (tsize > MAX_ALARM_TSIZE)
		        			tsize = (int) (tsize - tsize * 0.3);
		        		getWidget().setTextSize(tsize);
		        		((TextView) getElement().getChildAt(0)).setTextSize(tsize / 3);
		            }
		        });
		
	}

	
	public void updateElement() {
		if (element != null) {
			getWidget().post(new Runnable() {
				public void run() {
					if (element != null)
						getWidget().setText(
							(CharSequence) (AlarmClock.this.toString()));
				}

			});

			// TODO - move to set text
			getLabelWidget().post(new Runnable() {
				public void run() {
					if (element != null)
						getLabelWidget().setText(getName());
				}
			});
		}
	}
	
	public String toString()
	{
		return	String.format("%02d:%02d:%02d",
							 getHour(), getMin(), getSec());
	}
	

	public void setElement(LinearLayout element) {
		this.element = element;
	}

	public LinearLayout getElement() {
		return element;
	}

	public long getSec() {
		return seconds - (getHour() * HOUR) - (getMin() * MINUTE);
	}

	public void setMin(int min) {
		seconds += min * MINUTE;
	}

	public long getMin() {
		return (seconds - (getHour() * HOUR)) / MINUTE;
	}

	public void setHour(int hour) {
		seconds += hour * HOUR;
	}

	public long getHour() {
		return seconds / HOUR;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel parcel, int flags) {
		Log.d(MainActivity.TAG, "writeToParcel");
		parcel.writeString(name);
		parcel.writeInt(id);
		parcel.writeLong(seconds);
		parcel.writeString(state.name());
		parcel.writeString(sCode);
		parcel.writeInt(preset?1:0);
		parcel.writeLong(initSeconds);
		
	}

	private AlarmClock(Parcel parcel) {
		Log.d(MainActivity.TAG, "AlarmClock(Parcel parcel)");
		name = parcel.readString();
		id = parcel.readInt();
		seconds = parcel.readLong();
		state = TimerState.valueOf( parcel.readString());
		sCode = parcel.readString();
		preset = (parcel.readInt()==1)? true : false;
		initSeconds = parcel.readLong();
		this.element = null;
	}

	public TextView getWidget() {
		if (element == null)	return null;
		final TextView widget = (TextView) element.getChildAt(1);
		return widget;
	}

	public TextView getLabelWidget() {
		if (element == null)
			return null;
		final TextView widget = (TextView) element.getChildAt(0);
		return widget;
	}

	public static final Parcelable.Creator<AlarmClock> CREATOR = new Parcelable.Creator<AlarmClock>() {
		public AlarmClock createFromParcel(Parcel in) {
			Log.d(MainActivity.TAG, "createFromParcel");
			return new AlarmClock(in);
		}

		public AlarmClock[] newArray(int size) {
			return new AlarmClock[size];
		}
	};


    /**
	 Delete one second and change timer state
	*/
	public boolean tick() {
		if (state == TimerState.RUNNING)
			seconds--;
		if (seconds > 0) {
			return true;
		} else {
			this.state = TimerState.ALARMING;
			return false;
		}
	}

	public void setTime(long i) {
		seconds = i;
		initSeconds = i;
	}

	// reinit timer
	public void restart() {
		seconds = initSeconds;
	}

	public long getTime() {
		return seconds;
	}

	public void alarmNOW() {
		// vibratePhone(context);
		sendTimeIsOverNotification(0, context, null);

		final Animation anim = AnimationUtils.loadAnimation(context,
				R.anim.fade_bwb);
		anim.setRepeatMode(Animation.RESTART);

		final LinearLayout element = getElement();

		getWidget().post(new Runnable() {
			public void run() {
				getWidget().setTextColor(
						context.getResources().getColor(R.color.indian_red_1));

			}
		});

		if (getWidget() != null)
			getWidget().post(new Runnable() {
				public void run() {
					anim.setAnimationListener(new Animation.AnimationListener() {
						@Override
						public void onAnimationEnd(Animation arg0) {
							Animation anim = AnimationUtils.loadAnimation(
									context, R.anim.fade_bwb);
							anim.setAnimationListener(this);
							element.startAnimation(anim);
						}

						@Override
						public void onAnimationRepeat(Animation arg0) {
						}

						@Override
						public void onAnimationStart(Animation arg0) {
						}
					});
					element.startAnimation(anim);

				}
			});

		Log.d("op", "a" + element);
		//WakeUpLock.acquire(context);
	}

	public void alarmSTOP() {
		mNotificationManager.cancel(SettingsConst.APP_NOTIF_ID + 1);

		if (this.state == TimerState.ALARMING) {
			element.setAnimation(null);
			element.clearAnimation();
			getWidget().clearAnimation();
			//WakeUpLock.release();
		}
		setState( AlarmClock.TimerState.STOPPED);
	}

	/**
	 * Parse the user provided custom vibrate pattern into a long[] Borrowed
	 * from SMSPopup
	 */
	public static long[] parseVibratePattern(String stringPattern) {
		ArrayList<Long> arrayListPattern = new ArrayList<Long>();
		Long l;
		String[] splitPattern = stringPattern.split(",");
		int VIBRATE_PATTERN_MAX_SECONDS = 60000;
		int VIBRATE_PATTERN_MAX_PATTERN = 100;

		for (int i = 0; i < splitPattern.length; i++) {
			try {
				l = Long.parseLong(splitPattern[i].trim());
			} catch (NumberFormatException e) {
				return null;
			}
			if (l > VIBRATE_PATTERN_MAX_SECONDS) {
				return null;
			}
			arrayListPattern.add(l);
		}

		int size = arrayListPattern.size();
		if (size > 0 && size < VIBRATE_PATTERN_MAX_PATTERN) {
			long[] pattern = new long[size];
			for (int i = 0; i < pattern.length; i++) {
				pattern[i] = arrayListPattern.get(i);
			}
			return pattern;
		}

		return null;
	}

	/**
	 * 
	 * @param timer
	 * @param tickerText
	 * @param contentTitle
	 * @param contentText
	 */
	public static void sendTimeIsOverNotification(int timer, Context context, Long _when) {

		int icon;
		SharedPreferences mPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		icon = R.drawable.stat_notify_alarm;
		CharSequence mTickerText = " - "
				+ context.getResources().getString(R.string.app_name);
		long when = System.currentTimeMillis();
        if(_when != null) {
			when += _when*1000;
		}
		
		timer = (int) when + 4000;
		Notification notification = new Notification(icon, mTickerText, when);
		notification.number = timer + 1;

		CharSequence mContentTitle = context.getResources().getString(
				R.string.countdown_ended);
		CharSequence mContentText = context.getResources().getString(
				R.string.countdown_ended);

		Intent clickIntent = new Intent(context, MainActivity.class);
		clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(context, mContentTitle, mContentText,
				contentIntent);

		String customNotification = mPrefs.getString(
				context.getString(R.string.pref_notification_ringtone_key),
				null);
		String customSound = mPrefs.getString(
				context.getString(R.string.pref_soundfile_path_key), null);
		String soundSRC = mPrefs.getString(
				context.getString(R.string.pref_soundsource_key),
				SettingsActivity.SYSTEM_SOUND_VALUE);

		if (!soundSRC.equals("system")
				&& customSound != null && customSound.length() > 0) {
			notification.sound = Uri.parse("file://" + customSound);
		} else {
			if (customNotification != null)
				notification.sound = Uri.parse(customNotification);
		}

		if (mPrefs.getBoolean(
				context.getString(R.string.pref_notification_insistent_key),
				true))
			notification.flags |= Notification.FLAG_INSISTENT;
		if (mPrefs
				.getBoolean(context
						.getString(R.string.pref_notification_vibrate_key),
						true)) {
			String mVibratePattern = mPrefs.getString(context
					.getString(R.string.pref_notification_vibrate_pattern_key),
					"");
			if (!mVibratePattern.equals("")) {
				notification.vibrate = AlarmClock
						.parseVibratePattern(mVibratePattern);
			} else {
				notification.defaults |= Notification.DEFAULT_VIBRATE;
			}
		}
		if (mPrefs.getBoolean(
				context.getString(R.string.pref_notification_led_key), true)) {
			notification.flags |= Notification.FLAG_SHOW_LIGHTS;
			notification.ledARGB = Color
					.parseColor(mPrefs.getString(
							context.getString(R.string.pref_notification_led_color_key),
							"red"));
			int mLedBlinkRate = Integer.parseInt(mPrefs.getString(context
					.getString(R.string.pref_notification_led_blink_rate_key),
					"2"));
			notification.ledOnMS = 500;
			notification.ledOffMS = mLedBlinkRate * 1000;
		}

		notification.flags |= Notification.FLAG_AUTO_CANCEL;

	    NotificationManager mNotificationManager;
		mNotificationManager = (NotificationManager) context
				.getSystemService(ns);
		mNotificationManager.notify(SettingsConst.APP_NOTIF_ID + 1,
				notification);
	}

	public void updateState() {
		this.setState( state);

	}
	
	public static class ActiveFirstComparator implements Comparator<AlarmClock>{
		@Override
		public int compare(AlarmClock arg0, AlarmClock arg1) {
			if (arg0.state == TimerState.RUNNING && arg1.state != TimerState.RUNNING) return -1;
			if (arg1.state == TimerState.RUNNING && arg0.state != TimerState.RUNNING) return 1;
			return 0;
		}
	}
	
	public static class NearestActiveFirstComparator implements Comparator<AlarmClock>{
		@Override
		public int compare(AlarmClock arg0, AlarmClock arg1) {
			if (arg0.state == TimerState.RUNNING && arg1.state != TimerState.RUNNING) return -1;
			if (arg1.state == TimerState.RUNNING && arg0.state != TimerState.RUNNING) return 1;
			return (int)(arg0.getTime() - arg1.getTime());
		}
	}

}
