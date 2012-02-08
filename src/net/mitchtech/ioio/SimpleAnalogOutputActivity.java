package net.mitchtech.ioio;

import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.AbstractIOIOActivity;
import net.mitchtech.ioio.simpleanalogoutput.R;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SimpleAnalogOutputActivity extends AbstractIOIOActivity implements
		OnSeekBarChangeListener {

	private final int RED_PIN = 34;
	private final int GREEN_PIN = 35;
	private final int BLUE_PIN = 36;
	
	private final int PWM_FREQ = 100;
	private final int POLLING_DELAY = 150;

	private SeekBar mRedSeekBar, mGreenSeekBar, mBlueSeekBar;
	private View mColorIndicator;

	private int mRedState, mGreenState, mBlueState;
	private long mLastChange;

	private SharedPreferences mPrefs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mRedSeekBar = (SeekBar) findViewById(R.id.SeekBarRed);
		mGreenSeekBar = (SeekBar) findViewById(R.id.SeekBarGreen);
		mBlueSeekBar = (SeekBar) findViewById(R.id.SeekBarBlue);

		mColorIndicator = findViewById(R.id.ColorIndicator);

		mRedSeekBar.setOnSeekBarChangeListener(this);
		mGreenSeekBar.setOnSeekBarChangeListener(this);
		mBlueSeekBar.setOnSeekBarChangeListener(this);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mRedState = mPrefs.getInt("red", 0);
		mGreenState = mPrefs.getInt("green", 0);
		mBlueState = mPrefs.getInt("blue", 0);

		mRedSeekBar.setProgress(mRedState);
		mGreenSeekBar.setProgress(mGreenState);
		mBlueSeekBar.setProgress(mBlueState);
		mColorIndicator.setBackgroundColor(Color.rgb(mRedState, mGreenState, mBlueState));

		enableUi(false);
	}

	@Override
	protected void onStop() {
		super.onStop();
		mPrefs.edit().putInt("red", mRedState).putInt("green", mGreenState)
				.putInt("blue", mBlueState).commit();
	}

	class IOIOThread extends AbstractIOIOActivity.IOIOThread {
		private PwmOutput mRedLed;
		private PwmOutput mGreenLed;
		private PwmOutput mBlueLed;

		@Override
		public void setup() throws ConnectionLostException {
			try {
				mRedLed = ioio_.openPwmOutput(RED_PIN, PWM_FREQ);
				mGreenLed = ioio_.openPwmOutput(GREEN_PIN, PWM_FREQ);
				mBlueLed = ioio_.openPwmOutput(BLUE_PIN, PWM_FREQ);
				enableUi(true);
			} catch (ConnectionLostException e) {
				enableUi(false);
				throw e;
			}
		}

		@Override
		public void loop() throws ConnectionLostException {
			try {
				mRedLed.setPulseWidth(mRedState);
				mGreenLed.setPulseWidth(mGreenState);
				mBlueLed.setPulseWidth(mBlueState);
				sleep(10);
			} catch (InterruptedException e) {
				ioio_.disconnect();
			} catch (ConnectionLostException e) {
				enableUi(false);
				throw e;
			}
		}
	}

	@Override
	protected AbstractIOIOActivity.IOIOThread createIOIOThread() {
		return new IOIOThread();
	}

	private void enableUi(final boolean enable) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mRedSeekBar.setEnabled(enable);
				mGreenSeekBar.setEnabled(enable);
				mBlueSeekBar.setEnabled(enable);
			}
		});
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (System.currentTimeMillis() - mLastChange > POLLING_DELAY) {
			updateState(seekBar);
			mLastChange = System.currentTimeMillis();
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		mLastChange = System.currentTimeMillis();
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		updateState(seekBar);
	}

	private void updateState(final SeekBar seekBar) {

		switch (seekBar.getId()) {
		case R.id.SeekBarRed:
			mRedState = seekBar.getProgress();
			break;
		case R.id.SeekBarGreen:
			mGreenState = seekBar.getProgress();
			break;
		case R.id.SeekBarBlue:
			mBlueState = seekBar.getProgress();
			break;
		}

		mColorIndicator.setBackgroundColor(Color.rgb(mRedState, mGreenState, mBlueState));
	}
}