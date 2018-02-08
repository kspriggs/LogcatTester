package com.kspriggs.debug.logcattester;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Kevin Spriggs - 01/2018
 * A very simple application to simulate logging.
 * Used to help debug performance issues related to LOGD socket overflow.
 * UI controls allow user control of logging to simulate chatty apps.
 */
public class MainActivityFragment extends Fragment {

    final static String LOG_TAG = "LogcatTester: ";
    final static String LOG_TAG_CONTROL = "LogcatTesterControl: ";
    LogcatTask mLogcatAsyncTask = null;
    boolean mLoggingActive = false;
    int mLogAmount;
    int mLogDelay;
    static final int LOGAMOUNTDEFAULT = 0;
    static final int LOGDELAYDEFAULT = 1000;

    public MainActivityFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences pref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(getString(R.string.saved_logamount), mLogAmount);
        editor.putInt(getString(R.string.saved_logdelay), mLogDelay);
        editor.commit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        ToggleButton LoggingButton = v.findViewById(R.id.toggleButton2);
        final SeekBar seekBarAmount = v.findViewById(R.id.seekBar);
        final SeekBar seekBarDelay = v.findViewById(R.id.seekBar3);
        final EditText amountTextView = v.findViewById(R.id.editText_logcount);
        final EditText delayTextView = v.findViewById(R.id.editText_loopdelay);

        SharedPreferences pref = getActivity().getPreferences(Context.MODE_PRIVATE);
        mLogAmount = pref.getInt(getString(R.string.saved_logamount), LOGAMOUNTDEFAULT);
        mLogDelay = pref.getInt(getString(R.string.saved_logdelay), LOGDELAYDEFAULT);

        amountTextView.setText(Integer.toString(mLogAmount));
        amountTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                amountTextView.selectAll();
            }
        });
        delayTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delayTextView.selectAll();
            }
        });
        delayTextView.setText(Integer.toString(mLogDelay));
        amountTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    try {
                        int value = Integer.parseInt((amountTextView.getText().toString()));
                        seekBarAmount.setProgress(value);
                    } catch (NumberFormatException e) {
                        Log.e(LOG_TAG_CONTROL, e.toString());
                    }
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });
        delayTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    try {
                        int value = Integer.parseInt((delayTextView.getText().toString()));
                        seekBarDelay.setProgress(value);
                    } catch (NumberFormatException e) {
                        Log.e(LOG_TAG_CONTROL, e.toString());
                    }
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });


        seekBarAmount.setProgress(mLogAmount);
        seekBarDelay.setProgress(mLogDelay);

        seekBarDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mLogDelay = i;
                delayTextView.setText(Integer.toString(i));
                if(mLoggingActive) {
                    Log.i(LOG_TAG_CONTROL, "Starting logging with delay: "+Integer.toString(i));
                    if(mLogcatAsyncTask != null && !mLogcatAsyncTask.isCancelled());
                    {
                        mLogcatAsyncTask.cancel(true);
                    }
                    mLogcatAsyncTask = new LogcatTask();
                    mLogcatAsyncTask.execute(Integer.parseInt(amountTextView.getText().toString()), Integer.parseInt(delayTextView.getText().toString()));
                } else {
                    Log.i(LOG_TAG_CONTROL, "Stopping logging with delay: "+Integer.toString(i));
                    if(mLogcatAsyncTask != null && !mLogcatAsyncTask.isCancelled())
                    {
                        mLogcatAsyncTask.cancel(true);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekBarAmount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mLogAmount = i;
                amountTextView.setText(Integer.toString(i));
                if(mLoggingActive) {
                    Log.i(LOG_TAG_CONTROL, "Starting logging with amount: "+Integer.toString(i));
                    if(mLogcatAsyncTask != null && !mLogcatAsyncTask.isCancelled());
                    {
                        mLogcatAsyncTask.cancel(true);
                    }
                    mLogcatAsyncTask = new LogcatTask();
                    mLogcatAsyncTask.execute(Integer.parseInt(amountTextView.getText().toString()), Integer.parseInt(delayTextView.getText().toString()));
                } else {
                    Log.i(LOG_TAG_CONTROL, "Stopping logging with amount: "+Integer.toString(i));
                    if(mLogcatAsyncTask != null && !mLogcatAsyncTask.isCancelled())
                    {
                        mLogcatAsyncTask.cancel(true);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        LoggingButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean ischecked) {
                int i = seekBarAmount.getProgress();
                mLoggingActive = ischecked;
                if(ischecked) {
                    Log.i(LOG_TAG_CONTROL, "Starting logging with frequency: "+Integer.toString(i));
                    mLogcatAsyncTask = new LogcatTask();
                    mLogcatAsyncTask.execute(Integer.parseInt(amountTextView.getText().toString()), Integer.parseInt(delayTextView.getText().toString()));
                } else {
                    Log.i(LOG_TAG_CONTROL, "Stopping logging with frequency: "+Integer.toString(i));
                    if(mLogcatAsyncTask != null && !mLogcatAsyncTask.isCancelled());
                    {
                        mLogcatAsyncTask.cancel(true);
                    }
                }
            }
        });
        return v;
    }

    private class LogcatTask extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... params) {
            long count = 0;
            int log_amt = params[0];
            int delay_time = params[1];
            long system_time = System.currentTimeMillis();
            ActivityManager activityManager = (ActivityManager) getActivity().getSystemService(ACTIVITY_SERVICE);
            Log.i(LOG_TAG_CONTROL, "Starting a log loop with Log amount: " +Integer.toString(log_amt)+ " and delay_time: "+ Integer.toString(delay_time));
            while(!isCancelled()) {
                for(int k = 0; k<10*log_amt; ++k) {
                    //No particular reason for logging both an info and a debug....
                    Log.i(LOG_TAG, "Log info  level: " + Long.toString(system_time) + " " + Long.toString(count));
                    Log.d(LOG_TAG, "Log debug level: " + Long.toString(system_time) + " " + Long.toString(count));
                    count++;
                }
                try {
                    Thread.sleep(delay_time);
                } catch (InterruptedException e) {
                    Log.i(LOG_TAG_CONTROL, "Task Interrupted");
                }
            }
            return null;
        }
    }
}
