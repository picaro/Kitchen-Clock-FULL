package com.op.kclock.full;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Intent;
import android.content.SharedPreferences;
//import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
//import android.preference.VolumePreference;

import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.MediaStore;

import com.op.kclock.cookconst.SettingsConst;
import com.op.kclock.full.R;
import com.op.kclock.full.settings.FileChooserActivity;
import com.op.kclock.misc.Log;
import com.op.kclock.utils.Utils;

import java.io.*;

public class SettingsActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {

	public static final String SYSTEM_SOUND_VALUE = "system";
	private static final String WHEEL_DIALOG_VALUE = "wheel";
	private SharedPreferences mPrefs;
	public static final String CUSTOM_SOUNDFILE_KEY = "pref_soundfile_key";
	public static final String CUSTOM_BGFILE_KEY = "pref_bgfile_key";
	private static final int REQUEST_PICK_SOUND_FILE = 4;
	private static final int REQUEST_PICK_BG_FILE = 5;
	public static final String v = "soundfile";


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setVolumeControlStream(AudioManager.STREAM_NOTIFICATION);
		addPreferencesFromResource(R.xml.settings);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this
				.getApplicationContext());
		String version = "0";
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			if (pi != null)
				version = pi.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(MainActivity.TAG, "Package name not found", e);
			version = getString(R.string.pref_info_version_error);
		}

		findPreference(getString(R.string.pref_info_version_key)).setSummary(
				version);

		Preference soundSource = findPreference(getString(R.string.pref_soundsource_key));
		if (soundSource != null) {
			soundSource.setOnPreferenceChangeListener(this);
			soundSourceChanged(((ListPreference) soundSource).getValue());
		}
		
		Preference bgSource = findPreference(getString(R.string.pref_bgsource_key));
		if (bgSource != null) {
			bgSource.setOnPreferenceChangeListener(this);
			bgSourceChanged(((ListPreference) bgSource).getValue());
		}

		Preference pickstyle = findPreference(getString(R.string.pref_pickstyle_key));
		if (pickstyle != null) {
			pickstyle.setOnPreferenceChangeListener(this);
			dialogTypeChanged(((ListPreference) pickstyle).getValue());
		}

		Preference syssoundPref = findPreference(getString(R.string.pref_notification_ringtone_key));
		if (syssoundPref != null) {
			syssoundPref.setOnPreferenceChangeListener(this);
		}

		final String soundFilePath = mPrefs.getString(
					getApplicationContext().getString(
						R.string.pref_soundfile_path_key), null);//findPreference(getString(R.string.pref_soundfile_path_key));
		// add onclick for select shader
		Preference customPref = findPreference(CUSTOM_SOUNDFILE_KEY);
		onClickSelectFile(soundFilePath, customPref, new ArrayList<String>(
				Arrays.asList("mp3", "wav", "ogg")), REQUEST_PICK_SOUND_FILE);

		//final Preference bgFilePath = findPreference(getString(R.string.pref_bgfile_path_key));
		/*if (bgFilePath != null
				&& ((EditTextPreference) bgFilePath).getText() != null
				&& ((EditTextPreference) bgFilePath).getText().length() > 0
				&& ((EditTextPreference) bgFilePath).getText().toCharArray()[0] == '/') {
			((EditTextPreference) bgFilePath)
					.setTitle(((EditTextPreference) bgFilePath).getText());
		}*/
		Preference bgPref = findPreference(CUSTOM_BGFILE_KEY);
		
		bgPref.setOnPreferenceClickListener(new OnPreferenceClickListener(){

				public boolean onPreferenceClick(Preference p1)
				{
					Intent i = new Intent(Intent.ACTION_PICK,
                		android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);                 
					startActivityForResult(i, REQUEST_PICK_BG_FILE);
					return true;
				}

			
		});

		//onClickSelectFile(bgFilePath, bgPref,
		//		new ArrayList<String>(Arrays.asList("png", "pix","bmp","jpg","jpeg")),
		//		REQUEST_PICK_BG_FILE);

	}
	
	public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

	private void onClickSelectFile(final String soundFilePath,
			Preference customPref, final ArrayList<String> extensions,
			final int type) {
		if (customPref != null) {
			customPref
					.setOnPreferenceClickListener(new OnPreferenceClickListener() {

						public boolean onPreferenceClick(Preference preference) {
							// spawn the file chooser
							Intent fileChooserI = new Intent(
									SettingsActivity.this,
									FileChooserActivity.class);
							if (soundFilePath != null
									&& (soundFilePath)
											 != null
									&& (soundFilePath)
											.length() > 0
									&& (soundFilePath)
											.toCharArray()[0] == '/') {
								fileChooserI
										.putExtra(
												FileChooserActivity.EXTRA_FILE_PATH,
												soundFilePath
														.substring(
																0,
																soundFilePath
																		.lastIndexOf(
																				"/")));
							}
							if (extensions != null)
								fileChooserI
										.putStringArrayListExtra(
												FileChooserActivity.EXTRA_ACCEPTED_FILE_EXTENSIONS,
												extensions);
							startActivityForResult(fileChooserI, type);
							return true;
						}
					});
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
				case REQUEST_PICK_SOUND_FILE: {
					if (data.hasExtra(FileChooserActivity.EXTRA_FILE_PATH)) {
						findViewById(R.string.pref_soundfile_key);
						String filePath = data
								.getStringExtra(FileChooserActivity.EXTRA_FILE_PATH);
						//EditTextPreference customPref2 = (EditTextPreference) findPreference(getString(R.string.pref_soundfile_path_key));
						//customPref2.setText(filePath);
						//customPref2.setTitle(filePath);
						mPrefs.edit().putString(getString(R.string.pref_soundfile_path_key), filePath).commit();
					}
					break;
				}
				case REQUEST_PICK_BG_FILE: {
					if (data != null){//}.hasExtra(FileChooserActivity.EXTRA_FILE_PATH)) {
						findViewById(R.string.pref_bgfile_key);
						String filePath = getRealPathFromURI(data.getData());
						Log.e(MainActivity.TAG,""+filePath);
						//EditTextPreference customPref2 = (EditTextPreference) findPreference(getString(R.string.pref_bgfile_path_key));
						//customPref2.setText(filePath);
						//customPref2.setTitle(filePath);
						mPrefs.edit().putString(getString(R.string.pref_bgfile_path_key), filePath).commit();
					}
					break;
				}
			}

		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public boolean onPreferenceChange(Preference pref, Object value) {
		if (pref.getKey().equals(getString(R.string.pref_soundsource_key)))
			soundSourceChanged(value);
		if (pref.getKey().equals(getString(R.string.pref_bgsource_key)))
			bgSourceChanged(value);		
		if (pref.getKey().equals(getString(R.string.pref_pickstyle_key)))
			dialogTypeChanged(value);
		if (pref.getKey().equals(
				getString(R.string.pref_notification_ringtone_key))) {
			// dialogTypeChanged(value);
		}

		return true;
	}

	private void dialogTypeChanged(Object value) {
		Preference simmetricpick = findPreference(getString(R.string.pref_simmetricpick_key));
		Preference cyclmins = findPreference(getString(R.string.pref_cyclicmins_key));
		if (((String) value).equals(WHEEL_DIALOG_VALUE)) {
			simmetricpick.setEnabled(true);
			cyclmins.setEnabled(true);
		} else {
			simmetricpick.setEnabled(false);
			cyclmins.setEnabled(false);
		}

	}

	public void soundSourceChanged(Object value) {
		//Preference customPref = findPreference(getString(R.string.pref_soundfile_path_key));
		Preference customPrefBtn = findPreference(getString(R.string.pref_soundfile_key));
		Preference syssoundPref = findPreference(getString(R.string.pref_notification_ringtone_key));
		//customPref.setEnabled(false);
		if (((String) value).equals(SYSTEM_SOUND_VALUE)) {
			customPrefBtn.setEnabled(false);
			syssoundPref.setEnabled(true);
		} else {
			// customPref.setEnabled(true);
			customPrefBtn.setEnabled(true);
			syssoundPref.setEnabled(false);
		}
	}
	
	public void bgSourceChanged(Object value) {
		//Preference customPref = findPreference(getString(R.string.pref_bgfile_path_key));
		Preference customPrefBtn = findPreference(getString(R.string.pref_bgfile_key));
		//customPref.setEnabled(false);
		if (((String) value).equals(SYSTEM_SOUND_VALUE)) {
			customPrefBtn.setEnabled(false);
		} else {
			customPrefBtn.setEnabled(true);
		}
	}

	public void volumeChanged(Object value) {
		// Preference volumePref =
		// (Preference)findPreference(getString(R.string.pref_volume_key));
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		File prefFile = new File(SettingsConst.SETTINGS_FILE);
		try {
			prefFile.mkdirs();
			prefFile.createNewFile();
			Utils.saveSharedPreferencesToFile(prefFile, getApplicationContext());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
