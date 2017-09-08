package com.example.sfreyman.djam;

import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by marcf on 5/5/17.
 */

public class ProgBarRunnable implements Runnable {
    private int songDur;
    private ProgressBar pb;
    private Handler handler;
    private int elapsed;
    private TextView timeTV;
    private String durString;


    public ProgBarRunnable(int dur, ProgressBar pb, Handler handler, TextView timeTV) {
        this.songDur = dur;
        this.pb = pb;
        this.handler = handler;
        this.elapsed = 500;
        this.timeTV = timeTV;
        this.durString = parseMilliseconds(dur);
    }

    @Override
    public void run() {
        this.pb.setProgress(this.elapsed);
        this.timeTV.setText(parseMilliseconds(this.elapsed) + "/" + this.durString);
        this.elapsed += 500;

        if (this.elapsed + 500 < this.songDur) handler.postDelayed(this, 500);

    }

    public String parseMilliseconds(int millis) {
        int minutes = millis / 1000 / 60;
        int seconds = millis / 1000 % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }
}
