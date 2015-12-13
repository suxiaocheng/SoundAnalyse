package com.silicongo.george.soundanalyse;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RecordJob.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RecordJob#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RecordJob extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    /* TextView */
    private TextView tvSoundLevelVal;
    private TextView tvSettingSoundLevel;
    private TextView tvSettingSoundMuteTime;

    /* CheckBox */
    private CheckBox cbEnergyDetectEnable;

    /* SeekBar */
    private SeekBar sbSettingSoundLevel;
    private SeekBar sbSettingSoundMuteTime;

    /* Button */
    private Button btStartRecord;

    /* Background record service */
    private Intent intentRecordBackground;
    private RecordBackground mService;
    boolean mBound = false;

    /* AsyncTask to update display */
    private UpdateDisplayAsyncTask updateDisplayAsyncTask;
    private final double logValMax = Math.log10(0xffff);

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RecordJob.
     */
    // TODO: Rename and change types and number of parameters
    public static RecordJob newInstance(String param1, String param2) {
        RecordJob fragment = new RecordJob();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public RecordJob() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_record_job, container, false);

        tvSoundLevelVal = (TextView) view.findViewById(R.id.tvSoundLevelVal);
        cbEnergyDetectEnable = (CheckBox) view.findViewById(R.id.cbEnergyDetectEnable);
        tvSettingSoundLevel = (TextView) view.findViewById(R.id.tvSettingSoundLevel);
        sbSettingSoundLevel = (SeekBar) view.findViewById(R.id.sbSettingSoundLevel);
        tvSettingSoundMuteTime = (TextView) view.findViewById(R.id.tvSettingSoundMuteTime);
        sbSettingSoundMuteTime = (SeekBar) view.findViewById(R.id.sbSettingSoundMuteTime);
        btStartRecord = (Button) view.findViewById(R.id.btStartRecord);

        btStartRecord.setOnClickListener(this);
        btStartRecord.setEnabled(false);

        /* start the record background service */
        intentRecordBackground = new Intent(getActivity(), RecordBackground.class);
        getActivity().startService(intentRecordBackground);

        return view;
    }

    void initDisplayUI() {
        btStartRecord.setEnabled(true);
        btStartRecord.setText((mService.getRecordState() == true) ? "Stop" : "Start");
        cbEnergyDetectEnable.setChecked(mService.getRecordEnergyDetectEnable());
        int level = (int)(Math.log10(mService.getRecordSoundLevel()) * 100 / logValMax);
        String strTmp = String.format("%s[%d]",
                getContext().getResources().getText(R.string.SettingSoundLevel),
                level);
        tvSettingSoundLevel.setText(strTmp);
        sbSettingSoundLevel.setProgress(level);

        strTmp = String.format("%s[%d]",
                getContext().getResources().getText(R.string.SettingSounduteTime),
                mService.getRecordSoundMuteTime());
        tvSettingSoundMuteTime.setText(strTmp);
        sbSettingSoundMuteTime.setProgress(mService.getRecordSoundMuteTime());
    }

    @Override
    public void onResume() {
        getActivity().bindService(intentRecordBackground, mConnection, getContext().BIND_AUTO_CREATE);

        updateDisplayAsyncTask = new UpdateDisplayAsyncTask();
        updateDisplayAsyncTask.execute();

        super.onResume();
    }

    @Override
    public void onPause() {
        updateDisplayAsyncTask.cancel(true);
        updateDisplayAsyncTask = null;

        getActivity().unbindService(mConnection);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if(mService.getRecordState() == false) {
            mService.needQuitHandler = true;
            getActivity().stopService(intentRecordBackground);
        }
        super.onDestroy();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btStartRecord:
                int level = (int)Math.pow(10, (double) sbSettingSoundLevel.getProgress() * logValMax / 100);
                mService.setRecordParam(sbSettingSoundMuteTime.getProgress(),
                        cbEnergyDetectEnable.isEnabled(), level);
                mService.setRecordStart(!mService.getRecordState());
                initDisplayUI();
                break;
            default:
                break;
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            RecordBackground.LocalBinder binder = (RecordBackground.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            initDisplayUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            mService = null;
        }
    };


    private class UpdateDisplayAsyncTask extends AsyncTask<Void, Integer, Void> {
        private static final String TAG = "UpdateDisplayAsyncTask";
        private int lastEnergyLevel;
        private int maxEnergyLevel = 0x0;

        private int iUpdateCount = 0x0;

        public UpdateDisplayAsyncTask() {
        }

        /**
         * The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute()
         */
        protected Void doInBackground(Void... urls) {
            Integer[] progress = new Integer[1];
            while (isCancelled() == false) {
                if (mService != null) {
                    lastEnergyLevel = mService.getRecordCurrentEnergy();
                    if (maxEnergyLevel < lastEnergyLevel) {
                        maxEnergyLevel = lastEnergyLevel;
                    }
                    iUpdateCount++;
                    /* 500ms Max Update Time Used */
                    if (iUpdateCount > 5) {
                        iUpdateCount = 0x0;
                        progress[0] = new Integer(maxEnergyLevel);
                        publishProgress(progress);

                        maxEnergyLevel = 0x0;
                    }

                    synchronized (this) {
                        try {
                            wait(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return null;
        }

        protected void onPostExecute(Void result) {
        }

        protected void onProgressUpdate(Integer... progress) {
            int val = progress[0];
            double dPercent = Math.log10(val) / logValMax;
            int iPercent = (int) (dPercent * 100);
            tvSoundLevelVal.setText("[" + iPercent + "]");
        }
    }
}
