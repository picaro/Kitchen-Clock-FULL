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
package com.op.kclock.full.alarm;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.op.kclock.full.R;
import com.op.kclock.full.MainActivity;
import com.op.kclock.misc.Log;
import com.op.kclock.model.AlarmClock;

/**
 * Class that handles Putting alarm to file and to device. So that we can wake
 * our code on correct time.
 */
public class AlarmSingleServiceImpl implements Runnable {

	private final Context context;
	private static boolean running = false;
	private static boolean stateChanged = true;
	private List<AlarmClock> alarmList;
	private SharedPreferences mPrefs;
	private Integer ringerMode;

	Handler handler;

	public AlarmSingleServiceImpl(Context _context, Handler _handler,
			List<AlarmClock> _alarmList) {
		context = _context.getApplicationContext();
		handler = _handler;
		running = true;
		alarmList = _alarmList;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	@Override
	public void run() {
		boolean isRunning = false;
		do {
			// if (isRunning) stateChanged = true; else stateChanged = false;
			if (alarmList != null) {
				boolean runned = false;
				for (AlarmClock alarm : alarmList) {
					if (alarm.getState() == AlarmClock.TimerState.RUNNING) {
						runned = true;
						if (!isRunning) {
							stateChanged = true;
						} else {
							stateChanged = false;
						}
						isRunning = true;
						if (alarm.tick()) {
							alarm.updateElement();
						} else {
							alarm.updateElement();
							if (alarm.getState() == AlarmClock.TimerState.ALARMING)
								alarm.alarmNOW();
						}
					}
				}
				if (!runned) {
					if (isRunning) {
						stateChanged = true;
					} else {
						stateChanged = false;
					}

					isRunning = false;
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			updateLock(isRunning);
		} while (running);
	}

	private void updateLock(boolean isRunning) {
		// Log.v(MainActivity.TAG, "updateLock:" + isRunning + " - " +
		// stateChanged);
		if (mPrefs.getBoolean(context.getString(R.string.pref_disablelock_key),
				true)){
			if (stateChanged) {
				Log.d(MainActivity.TAG, ":" + isRunning);
				AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
				if (isRunning) {
					WakeUpLock.acquire(context);
				} else {
					if (ringerMode != null) am.setRingerMode(ringerMode);					
					WakeUpLock.release();
				}
			}
		} ;
	}

}
