package com.mullis.clay.simplehlsstream;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    private String[] urls = new String[] {
            "https://devimages.apple.com.edgekey.net/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8", "Beeps",
            "http://vevoplaylist-live.hls.adaptive.level3.net/vevo/ch1/appleman.m3u8", "Pop Radio Station",
            "http://playertest.longtailvideo.com/adaptive/bbbfull/bbbfull.m3u8", "Whimsical Music",
            "http://playertest.longtailvideo.com/adaptive/oceans_aes/oceans_aes.m3u8", "Disney Documentary",
            "http://qthttp.apple.com.edgesuite.net/1010qwoeiuryfg/sl.m3u8", "Steve Jobs Presentation"
    };

    private float bufferEndPercent = 0;
    private long durationSeconds = 0;
    private long lastDurationSeconds = 0;
    private long positionSeconds = 0;
    private long lastPositionSeconds = -1;
    private float positionPercent = 0;
    private int lastSeekProgress = -1;
    private int lastSecondaryProgress = -1;
    private boolean lastPlaying = false;
    private boolean playing = false;
    private Handler mHandler;
    private TextView currentTime;
    private TextView duration;
    private SeekBar seekBar;
    private Button playPause;
    private ArrayList<String> urlData = new ArrayList<>();
    private int selectedRow = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String sampleRate = null, bufferSize = null;
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        sampleRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        bufferSize = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);

        System.loadLibrary("SimpleHLS");
        SetTempFolder(getCacheDir().getAbsolutePath());
        StartAudio(Integer.parseInt(sampleRate), Integer.parseInt(bufferSize));

        // Set up the user interface
        currentTime = (TextView)findViewById(R.id.currentTime);
        currentTime.setText("");
        duration = (TextView)findViewById(R.id.duration);
        seekBar = (SeekBar)findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Seek((float) (seekBar.getProgress()) / 100.0f);
            }
        });
        seekBar.setVisibility(View.INVISIBLE);
        playPause = (Button)findViewById(R.id.playPause);
        ListView urlList = (ListView)findViewById(R.id.urlList);
        for (int n = 1; n < urls.length; n += 2) urlData.add(urls[n]);
        ArrayAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, urlData);
        urlList.setAdapter(adapter);
        urlList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long arg3) {
                view.setSelected(true);
                selectedRow = position;
                Open(urls[position * 2]);
            }
        });

        // Update the UI every 50 ms
        Runnable mRunnable = new Runnable() {
            @Override
            public void run() {
                UpdateStatus();
                if (durationSeconds >= 4294967295L) durationSeconds = -1;
                if (lastDurationSeconds != durationSeconds) {
                    lastDurationSeconds = durationSeconds;
                    if (durationSeconds > 0) {
                        duration.setText(String.format("%02d", durationSeconds / 60) + ":" + String.format("%02d", durationSeconds % 60));
                        seekBar.setVisibility(View.VISIBLE);
                    } else if (durationSeconds == 0) {
                        duration.setText("Loading...");
                        currentTime.setText("");
                        seekBar.setVisibility(View.INVISIBLE);
                    } else {
                        duration.setText("LIVE");
                        seekBar.setVisibility(View.INVISIBLE);
                    }
                }
                if ((durationSeconds > 0) && (lastPositionSeconds != positionSeconds)) {
                    lastPositionSeconds = positionSeconds;
                    currentTime.setText(String.format("%02d", positionSeconds / 60) + ":" + String.format("%02d", positionSeconds % 60));
                }
                int secondaryProgress = (int)(bufferEndPercent * 100.0f), seekProgress = (int)(positionPercent * 100.0f);
                if ((lastSecondaryProgress != secondaryProgress) || (lastSeekProgress != seekProgress)) {
                    lastSecondaryProgress = secondaryProgress;
                    lastSeekProgress = seekProgress;
                    seekBar.setProgress(seekProgress);
                    seekBar.setSecondaryProgress(secondaryProgress);
                }
                if (lastPlaying != playing) {
                    lastPlaying = playing;
                    playPause.setText(lastPlaying ? "PAUSE" : "PLAY");
                }
                mHandler.postDelayed(this, 50);
            }
        };
        mHandler = new Handler();
        mHandler.postDelayed(mRunnable, 50);

    }

    public void onPlayPause(View view) {
        Play();
    }


    @Override
    public void onResume() {
        super.onResume();
        onForeground();
    }

    @Override
    public void onPause() {
        super.onPause();
        onBackground();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Cleanup();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private native void SetTempFolder(String path);
    private native void StartAudio(long samplerate, long buffersize);
    private native void onForeground();
    private native void onBackground();
    private native void Open(String url);
    private native void Seek(float percent);
    private native void Play();
    private native void Pause();
    private native void UpdateStatus();
    private native void Cleanup();
}
