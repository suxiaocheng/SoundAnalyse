package com.silicongo.george.soundanalyse;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.widget.Toast;

import com.silicongo.george.soundanalyse.RecordUtils.ExtAudioRecorder;

import java.io.File;
import java.text.SimpleDateFormat;

public class RecordBackground extends Service {
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();


    /* Name of the record file */
    public String last_record_file;
    /* Record file directory */
    private static final String RECORD_DIR = Environment.getExternalStorageDirectory() + "/Record";
    private ExtAudioRecorder recorder;

    public boolean needQuitHandler;

    public RecordBackground() {
    }


    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            // Normally we would do some work here, like download a file.

            while (needQuitHandler == true) {
                try {
                    wait(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        /* start the record operation */
        if(recordMedia() == true){
            setRecordParam(4, false, 1024);
        }

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        // Send a null msg to the services
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = 0x0;
        mServiceHandler.sendMessage(msg);

        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopRecord();
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        RecordBackground getService() {
            // Return this instance of LocalService so clients can call public methods
            return RecordBackground.this;
        }
    }


    /**
     * Stop Record File to an fix file
     *
     * @return true if sucessfully, false if fail
     */
    public boolean stopRecord() {
        if (recorder != null) {
            recorder.stopRecord();
            recorder = null;
        }
        return true;
    }

    /**
     * start to prepare record media
     *
     * @return
     */
    public boolean recordMedia() {
        if (recorder == null) {
            SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd hh-mm-ss");
            String filename = "Record " + sDateFormat.format(new java.util.Date()) + ".wav";
            last_record_file = RECORD_DIR + File.separator + filename;
            recorder = ExtAudioRecorder.getInstanse(true); // 未压缩的录音（WAV）
            recorder.recordChat(RECORD_DIR + File.separator, filename);
            if (recorder.getState() != ExtAudioRecorder.State.ERROR) {
                return true;
            } else {
                recorder = null;
                return false;
            }
        } else {
            if (recorder.getState() != ExtAudioRecorder.State.ERROR) {
                return true;
            }
            return false;
        }
    }

    public int getRecordCurrentEnergy() {
        if (recorder != null) {
            return recorder.current_energy;
        }
        return 0x0;
    }

    public boolean getRecordState(){
        if(recorder != null){
            if(recorder.getState() == ExtAudioRecorder.State.RECORDING){
                return recorder.uncompress_recording;
            }
        }
        return false;
    }

    public boolean getRecordEnergyDetectEnable(){
        return recorder.setting_energy_check_enable;
    }

    public int getRecordSoundLevel(){
        return recorder.setting_energy_record_level;
    }

    public int getRecordSoundMuteTime(){
        return recorder.setting_energy_invalid_time;
    }

    public boolean setRecordParam(int energy_invalid_time, boolean energy_check, int energy_level){
        if(energy_level == 0){
            energy_level++;
        }
        recorder.settingRecordParam(energy_invalid_time, energy_check, energy_level);
        return true;
    }

    public boolean setRecordStart(boolean start){
        if(recorder != null) {
            if(recorder.getState() == ExtAudioRecorder.State.RECORDING) {
                recorder.setStartFlag(start);
                return true;
            }
        }
        return false;
    }
}
