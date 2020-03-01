package com.builtclean.android.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.builtclean.android.livewallpapers.powersmileys.PowerSmileysLiveWallpaperSettings;

public class SeekBarPreference extends Preference implements
		OnSeekBarChangeListener {

	public static int maximum = 95;
	public static int interval = 5;
	
	private TextView monitorBox;
	private int originalValue;

	public SeekBarPreference(Context context) {
		super(context);
	}

	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected View onCreateView(ViewGroup parent) {

		this.originalValue = getSharedPreferences().getInt(getKey(), 50);

		LinearLayout layout = new LinearLayout(getContext());

		LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		params1.gravity = Gravity.LEFT;
		params1.weight = 1.0f;

		LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(
				parent.getWidth() - 200, LinearLayout.LayoutParams.WRAP_CONTENT);
		params2.gravity = Gravity.RIGHT;

		LinearLayout.LayoutParams params3 = new LinearLayout.LayoutParams(30,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		params3.gravity = Gravity.CENTER;

		layout.setPadding(5, 5, 5, 5);
		layout.setOrientation(LinearLayout.HORIZONTAL);

		TextView view = new TextView(getContext());
		view.setText(getTitle());
		view.setTextSize(18);
		view.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
		view.setGravity(Gravity.LEFT);
		view.setLayoutParams(params1);

		SeekBar bar = new SeekBar(getContext());
		bar.setMax(maximum);
		bar.setProgress((int) this.originalValue);
		bar.setLayoutParams(params2);
		bar.setOnSeekBarChangeListener(this);

		this.monitorBox = new TextView(getContext());
		this.monitorBox.setTextSize(12);
		this.monitorBox.setTypeface(Typeface.MONOSPACE, Typeface.ITALIC);
		this.monitorBox.setLayoutParams(params3);
		this.monitorBox.setPadding(2, 5, 0, 0);
		this.monitorBox.setText(bar.getProgress() + "");

		layout.addView(view);
		layout.addView(bar);
		layout.addView(this.monitorBox);
		layout.setId(android.R.id.widget_frame);

		return layout;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {

		progress = validateValue(progress);

		if (!callChangeListener(progress)) {
			seekBar.setProgress((int) this.originalValue);
			return;
		}

		seekBar.setProgress(progress);
		this.originalValue = progress;
		this.monitorBox.setText(progress + "");
		updatePreference(progress);

		notifyChanged();
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	@Override
	protected Object onGetDefaultValue(TypedArray ta, int index) {

		int dValue = (int) ta.getInt(index, 50);

		return validateValue(dValue);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

		int temp = restoreValue ? getPersistedInt(50) : (Integer) defaultValue;

		if (!restoreValue)
			persistInt(temp);

		this.originalValue = temp;
	}

	private int validateValue(int value) {

		if (getKey() == null)
			return value;

		if (value > maximum)
			value = maximum;
		else if (value < 0)
			value = 0;
		else if (value % interval != 0)
			value = Math.round(((float) value) / interval) * interval;
		
		if (getKey()
				.equals(PowerSmileysLiveWallpaperSettings.LOW_BATTERY_LEVEL_PREFERENCE_KEY)) {
			value = Math
					.min(value,
							getSharedPreferences()
									.getInt(PowerSmileysLiveWallpaperSettings.MEDIUM_BATTERY_LEVEL_PREFERENCE_KEY,
											0) - 1);
		} else if (getKey()
				.equals(PowerSmileysLiveWallpaperSettings.MEDIUM_BATTERY_LEVEL_PREFERENCE_KEY)) {
			value = Math
					.min(value,
							getSharedPreferences()
									.getInt(PowerSmileysLiveWallpaperSettings.HIGH_BATTERY_LEVEL_PREFERENCE_KEY,
											0) - 1);
			value = Math
					.max(value,
							getSharedPreferences()
									.getInt(PowerSmileysLiveWallpaperSettings.LOW_BATTERY_LEVEL_PREFERENCE_KEY,
											0) + 1);
		} else if (getKey()
				.equals(PowerSmileysLiveWallpaperSettings.HIGH_BATTERY_LEVEL_PREFERENCE_KEY)) {
			value = Math
					.max(value,
							getSharedPreferences()
									.getInt(PowerSmileysLiveWallpaperSettings.MEDIUM_BATTERY_LEVEL_PREFERENCE_KEY,
											0) + 1);
		}

		return value;
	}

	private void updatePreference(int newValue) {

		SharedPreferences.Editor editor = getEditor();
		editor.putInt(getKey(), newValue);
		editor.commit();
	}

}