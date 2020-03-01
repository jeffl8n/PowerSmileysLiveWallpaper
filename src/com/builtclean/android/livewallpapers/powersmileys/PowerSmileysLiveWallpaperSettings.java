package com.builtclean.android.livewallpapers.powersmileys;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class PowerSmileysLiveWallpaperSettings extends PreferenceActivity
		implements ColorPickerDialog.OnColorChangedListener,
		SharedPreferences.OnSharedPreferenceChangeListener {
	public static final String BACKGROUND_COLOR_PREFERENCE_KEY = "background_color";
	public static final int BACKGROUND_COLOR_DEFAULT = Color.BLACK;
	public static final String FULL_SMILEY_COLOR_PREFERENCE_KEY = "high_smiley_color";
	public static final int FULL_SMILEY_COLOR_DEFAULT = Color.GREEN;
	public static final String HALF_SMILEY_COLOR_PREFERENCE_KEY = "medium_smiley_color";
	public static final int HALF_SMILEY_COLOR_DEFAULT = Color.YELLOW;
	public static final String QUARTER_SMILEY_COLOR_PREFERENCE_KEY = "low_smiley_color";
	public static final int QUARTER_SMILEY_COLOR_DEFAULT = Color.rgb(255, 165,
			0);
	public static final String EMPTY_SMILEY_COLOR_PREFERENCE_KEY = "empty_smiley_color";
	public static final int EMPTY_SMILEY_COLOR_DEFAULT = Color.RED;
	public static final String DOWNLOAD_PRO_PREFERENCE_KEY = "download_pro";

	public static final String HIGH_BATTERY_LEVEL_PREFERENCE_KEY = "high_smiley_level";
	public static final int HIGH_BATTERY_LEVEL_DEFAULT = 75;
	public static final String MEDIUM_BATTERY_LEVEL_PREFERENCE_KEY = "medium_smiley_level";
	public static final int MEDIUM_BATTERY_LEVEL_DEFAULT = 40;
	public static final String LOW_BATTERY_LEVEL_PREFERENCE_KEY = "low_smiley_level";
	public static final int LOW_BATTERY_LEVEL_DEFAULT = 30;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		getPreferenceManager().setSharedPreferencesName(
				PowerSmileysLiveWallpaper.SHARED_PREFS_NAME);
		addPreferencesFromResource(R.xml.wallpaper_settings);
		getPreferenceManager().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		getPreferenceManager().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
	}

	@Override
	public void colorChanged(String preferenceKey, int color) {
		getPreferenceManager().getSharedPreferences().edit()
				.putInt(preferenceKey, color).commit();
	}

	public boolean onPreferenceClick(Preference preference) {

		if (preference.getKey().equals(DOWNLOAD_PRO_PREFERENCE_KEY)) {

			new AlertDialog.Builder(this)
					.setMessage("Download the pro version to remove ads.")
					.setTitle("Download Pro Version")
					.setPositiveButton("Download Pro", new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(
									Intent.ACTION_VIEW,
									Uri.parse("market://details?id=com.builtclean.android.livewallpapers.powersmileys"));

							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
							startActivity(intent);
						}
					}).show();

		} else if (preference.getKey().equals(FULL_SMILEY_COLOR_PREFERENCE_KEY)) {
			new ColorPickerDialog(this, this, FULL_SMILEY_COLOR_PREFERENCE_KEY,
					getPreferenceManager().getSharedPreferences().getInt(
							FULL_SMILEY_COLOR_PREFERENCE_KEY,
							FULL_SMILEY_COLOR_DEFAULT),
					FULL_SMILEY_COLOR_DEFAULT, "Pick a High Smiley color")
					.show();
		} else if (preference.getKey().equals(HALF_SMILEY_COLOR_PREFERENCE_KEY)) {
			new ColorPickerDialog(this, this, HALF_SMILEY_COLOR_PREFERENCE_KEY,
					getPreferenceManager().getSharedPreferences().getInt(
							HALF_SMILEY_COLOR_PREFERENCE_KEY,
							HALF_SMILEY_COLOR_DEFAULT),
					HALF_SMILEY_COLOR_DEFAULT, "Pick a Medium Smiley color")
					.show();
		} else if (preference.getKey().equals(
				QUARTER_SMILEY_COLOR_PREFERENCE_KEY)) {
			new ColorPickerDialog(this, this,
					QUARTER_SMILEY_COLOR_PREFERENCE_KEY, getPreferenceManager()
							.getSharedPreferences().getInt(
									QUARTER_SMILEY_COLOR_PREFERENCE_KEY,
									QUARTER_SMILEY_COLOR_DEFAULT),
					QUARTER_SMILEY_COLOR_DEFAULT, "Pick a Low Smiley color")
					.show();
		} else if (preference.getKey()
				.equals(EMPTY_SMILEY_COLOR_PREFERENCE_KEY)) {
			new ColorPickerDialog(this, this,
					EMPTY_SMILEY_COLOR_PREFERENCE_KEY, getPreferenceManager()
							.getSharedPreferences().getInt(
									EMPTY_SMILEY_COLOR_PREFERENCE_KEY,
									EMPTY_SMILEY_COLOR_DEFAULT),
					EMPTY_SMILEY_COLOR_DEFAULT, "Pick a Very Low Smiley color")
					.show();
		} else if (preference.getKey().equals(BACKGROUND_COLOR_PREFERENCE_KEY)) {
			new ColorPickerDialog(this, this, BACKGROUND_COLOR_PREFERENCE_KEY,
					getPreferenceManager().getSharedPreferences().getInt(
							BACKGROUND_COLOR_PREFERENCE_KEY,
							BACKGROUND_COLOR_DEFAULT),
					BACKGROUND_COLOR_DEFAULT, "Pick a Background color").show();
		}
		return true;
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		return onPreferenceClick(preference);
	}

}
