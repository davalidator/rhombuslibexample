package com.chumpchange.android.rhombuslibexample;

import java.util.List;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.chumpchange.android.rhombuslibexample.audio.AudioMonitor;
import com.chumpchange.android.rhombuslibexample.audio.AudioMonitorActivity;
import com.chumpchange.android.rhombuslibexample.audio.HeadsetStateReceiver;
import com.chumpchange.android.rhombuslibexample.audio.MessageType;
import com.chumpchange.android.rhombuslibexample.audio.decoder.AudioDecoder;
import com.chumpchange.android.rhombuslibexample.audio.decoder.SwipeData;

public class MainActivity extends ActionBarActivity implements
		AudioMonitorActivity {

	private HeadsetStateReceiver mHeadsetStateReceiver;
	private IntentFilter mIntentFilter;

	private Thread mAudioMonitorThread;
	private Runnable mRunnable;
	private Handler mAudioMonitorCallbackHandler;
	private AudioMonitor mAudioMonitor;
	private AudioDecoder mAudioDecoder;

	private TextView mDongleStateTextView;
	private View mMessageView;
	private TextView mMessageTextView;
	private View mTrackDataView;
	private TextView mTrackDataTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mHeadsetStateReceiver = new HeadsetStateReceiver(this);
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(Intent.ACTION_HEADSET_PLUG);

		mAudioMonitorThread = new Thread();

		mRunnable = new Runnable() {
			@Override
			public void run() {
				mAudioMonitor.monitor();
				System.out.println("At the end");
			}
		};

		mAudioMonitorCallbackHandler = new Handler(new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				if (msg.what == MessageType.NO_DATA_PRESENT.ordinal()) {
					System.out.println("No data present");
					return true;
				} else if (msg.what == MessageType.DATA_PRESENT.ordinal()) {
					System.out.println("data present");
					hideMessage();
					hideTrackData();
					return true;
				} else if (msg.what == MessageType.RECORDING_ERROR.ordinal()) {
					showErrorMessage("Recording error");
					return true;
				} else if (msg.what == MessageType.INVALID_SAMPLE_RATE
						.ordinal()) {
					showErrorMessage("Invalid sample rate");
					return true;
				} else if (msg.what == MessageType.DATA.ordinal()) {
					System.out.println("Data received");
					onNewTrackData((List<Integer>) msg.obj);
					return true;
				} else {
					return false;
				}
			}
		});

		mAudioMonitor = new AudioMonitor(mAudioMonitorCallbackHandler);
		mAudioDecoder = new AudioDecoder();

		mDongleStateTextView = (TextView) findViewById(R.id.dongle_state);
		mMessageView = findViewById(R.id.message_view);
		mMessageTextView = (TextView) findViewById(R.id.message);
		mTrackDataView = findViewById(R.id.track_data_view);
		mTrackDataTextView = (TextView) findViewById(R.id.track_data);
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mHeadsetStateReceiver, mIntentFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mHeadsetStateReceiver);
		stopAudioMonitor();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void startAudioMonitor() {
		System.out.println("Loc. 1");

		try {
			mAudioMonitorThread.join();
		} catch (InterruptedException e) {
		}

		mAudioMonitorThread = new Thread(mRunnable);
		mAudioMonitorThread.start();
	}

	private void stopAudioMonitor() {
		System.out.println("Loc. 3");
		if (mAudioMonitor.isRecording()) {
			mAudioMonitor.stopRecording();
		}
		try {
			mAudioMonitorThread.join();
		} catch (InterruptedException e) {
		}
	}

	@Override
	public void setDongleReady(boolean state) {
		hideMessage();
		hideTrackData();

		if (state) {
			mDongleStateTextView.setTextColor(Color.GREEN);
			mDongleStateTextView.setText("connected");

			startAudioMonitor();
		} else {
			mDongleStateTextView.setText("disconnected");
			mDongleStateTextView.setTextColor(Color.RED);

			stopAudioMonitor();
		}
	}

	private void showMessage(String msg, int color) {
		hideTrackData();
		mMessageView.setVisibility(View.VISIBLE);
		mMessageTextView.setText(msg);
		mMessageTextView.setTextColor(color);
	}

	private void showStatusMessage(String msg) {
		showMessage(msg, Color.GREEN);
	}

	private void showErrorMessage(String msg) {
		showMessage(msg, Color.RED);
	}

	private void showTrackData(String data) {
		hideMessage();
		mTrackDataView.setVisibility(View.VISIBLE);
		mTrackDataTextView.setText(data);
	}

	private void hideMessage() {
		mMessageView.setVisibility(View.INVISIBLE);
		mMessageTextView.setText("");
	}

	private void hideTrackData() {
		mTrackDataView.setVisibility(View.INVISIBLE);
		mTrackDataTextView.setText("");
	}

	private void onNewTrackData(List<Integer> samples) {
		stopAudioMonitor();
		SwipeData data = mAudioDecoder.processData(samples);
		if (data.isBadRead()) {
			showErrorMessage("Bad read");
		} else {
			showTrackData(data.content);
		}
		startAudioMonitor();
	}
}
