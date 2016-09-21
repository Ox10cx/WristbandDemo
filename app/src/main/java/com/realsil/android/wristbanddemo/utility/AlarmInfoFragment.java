package com.realsil.android.wristbanddemo.utility;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

import com.realsil.android.wristbanddemo.R;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayer;

import java.util.Calendar;

public class AlarmInfoFragment extends DialogFragment {
    private OnSaveListener mListener;

    //private Button mbtnSave;
    //private Button mbtnCancel;
    private RelativeLayout mrlSave;
    private RelativeLayout mrlCancel;

    TimePicker mtpWristSetAlarmTime;

    CheckBox mcbWristSetAlarmRepMon,
            mcbWristSetAlarmRepTues,
            mcbWristSetAlarmRepWed,
            mcbWristSetAlarmRepThu,
            mcbWristSetAlarmRepFri,
            mcbWristSetAlarmRepSat,
            mcbWristSetAlarmRepSun;

    public static final String EXTRAS_DEFAULT_HOUR = "DEFAULT_HOUR";
    public static final String EXTRAS_DEFAULT_MINUTE = "DEFAULT_MINUTE";
    public static final String EXTRAS_DEFAULT_DAY_FLAG = "DEFAULT_DAY_FLAG";
    public static final String EXTRAS_VALUE_POSITION = "VALUE_POSITION";

    private static Context mContext;
    private int mPosition;
    private int mDefHour;
    private int mDefMinute;
    private byte mDefDayFlag;

    private boolean isChanged;
    private boolean mFirstInitialFlag;

    /**
     * Static implementation of fragment so that it keeps data when phone orientation is changed.
     * For standard BLE Service UUID, we can filter devices using normal android provided command
     * startScanLe() with required BLE Service UUID
     * For custom BLE Service UUID, we will use class ScannerServiceParser to filter out required
     * device.
     */
    public static AlarmInfoFragment getInstance(Context context) {
        final AlarmInfoFragment fragment = new AlarmInfoFragment();
        mContext = context;
        return fragment;
    }
    /**
     * When dialog is created then set AlertDialog with list and button views
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_wristband_set_alarm, null);
        final AlertDialog dialog = builder.setView(dialogView).create();
        mPosition = getArguments().getInt(EXTRAS_VALUE_POSITION);
        mDefHour = getArguments().getInt(EXTRAS_DEFAULT_HOUR);
        mDefMinute = getArguments().getInt(EXTRAS_DEFAULT_MINUTE);
        mDefDayFlag = getArguments().getByte(EXTRAS_DEFAULT_DAY_FLAG);
        Log.i("123", ", mPosition: " + mPosition
                + ", mDefHour: " + mDefHour
                + ", mDefMinute: " + mDefMinute
                + ", mDefDayFlag: " + mDefDayFlag);

        isChanged = false;
        mFirstInitialFlag = true;

        // initial UI
        mtpWristSetAlarmTime = (TimePicker)dialogView.findViewById(R.id.tpWristSetAlarmTime);

        mrlSave = (RelativeLayout) dialogView.findViewById(R.id.rlWristAlarmSure);
        mrlSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.rlWristAlarmSure) {
                    int hour,minute;
                    hour = mtpWristSetAlarmTime.getCurrentHour();
                    minute = mtpWristSetAlarmTime.getCurrentMinute();

                    byte dayFlag = (byte) (0x00
                            | (mcbWristSetAlarmRepMon.isChecked()? ApplicationLayer.REPETITION_MON : 0x00)
                            | (mcbWristSetAlarmRepTues.isChecked()? ApplicationLayer.REPETITION_TUES : 0x00)
                            | (mcbWristSetAlarmRepWed.isChecked()? ApplicationLayer.REPETITION_WED : 0x00)
                            | (mcbWristSetAlarmRepThu.isChecked()? ApplicationLayer.REPETITION_THU : 0x00)
                            | (mcbWristSetAlarmRepFri.isChecked()? ApplicationLayer.REPETITION_FRI : 0x00)
                            | (mcbWristSetAlarmRepSat.isChecked()? ApplicationLayer.REPETITION_SAT : 0x00)
                            | (mcbWristSetAlarmRepSun.isChecked()? ApplicationLayer.REPETITION_SUN : 0x00));
                    // cancel
                    dialog.cancel();
                    if(isChanged
                            || (hour != mDefHour || minute != mDefMinute)) {
                        // tell the saved
                        mListener.onAlarmInfoSaved(mPosition, hour, minute, dayFlag);
                    }
                }
            }
        });

        mrlCancel = (RelativeLayout) dialogView.findViewById(R.id.rlWristAlarmCancel);
        mrlCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        mcbWristSetAlarmRepMon = (CheckBox)dialogView.findViewById(R.id.cbWristSetAlarmRepMon);
        mcbWristSetAlarmRepTues = (CheckBox)dialogView.findViewById(R.id.cbWristSetAlarmRepTues);
        mcbWristSetAlarmRepWed = (CheckBox)dialogView.findViewById(R.id.cbWristSetAlarmRepWed);
        mcbWristSetAlarmRepThu = (CheckBox)dialogView.findViewById(R.id.cbWristSetAlarmRepThu);
        mcbWristSetAlarmRepFri = (CheckBox)dialogView.findViewById(R.id.cbWristSetAlarmRepFri);
        mcbWristSetAlarmRepSat = (CheckBox)dialogView.findViewById(R.id.cbWristSetAlarmRepSat);
        mcbWristSetAlarmRepSun = (CheckBox)dialogView.findViewById(R.id.cbWristSetAlarmRepSun);
        mcbWristSetAlarmRepMon.setOnCheckedChangeListener(checkChangeListener);
        mcbWristSetAlarmRepTues.setOnCheckedChangeListener(checkChangeListener);
        mcbWristSetAlarmRepWed.setOnCheckedChangeListener(checkChangeListener);
        mcbWristSetAlarmRepThu.setOnCheckedChangeListener(checkChangeListener);
        mcbWristSetAlarmRepFri.setOnCheckedChangeListener(checkChangeListener);
        mcbWristSetAlarmRepSat.setOnCheckedChangeListener(checkChangeListener);
        mcbWristSetAlarmRepSun.setOnCheckedChangeListener(checkChangeListener);

        initialUI();
        return dialog;
    }


    private void updateChangedSetting() {
        if(mFirstInitialFlag) {
            return;
        }
        isChanged = true;
    }

    CompoundButton.OnCheckedChangeListener checkChangeListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            updateChangedSetting();
        }
    };

    private void initialUI() {
        mtpWristSetAlarmTime.setCurrentHour(mDefHour);
        mtpWristSetAlarmTime.setCurrentMinute(mDefMinute);

        mcbWristSetAlarmRepMon.setChecked((mDefDayFlag & ApplicationLayer.REPETITION_MON) != 0);
        mcbWristSetAlarmRepTues.setChecked((mDefDayFlag & ApplicationLayer.REPETITION_TUES) != 0);
        mcbWristSetAlarmRepWed.setChecked((mDefDayFlag & ApplicationLayer.REPETITION_WED) != 0);
        mcbWristSetAlarmRepThu.setChecked((mDefDayFlag & ApplicationLayer.REPETITION_THU) != 0);
        mcbWristSetAlarmRepFri.setChecked((mDefDayFlag & ApplicationLayer.REPETITION_FRI) != 0);
        mcbWristSetAlarmRepSat.setChecked((mDefDayFlag & ApplicationLayer.REPETITION_SAT) != 0);
        mcbWristSetAlarmRepSun.setChecked((mDefDayFlag & ApplicationLayer.REPETITION_SUN) != 0);

        mFirstInitialFlag = false;
    }

    /**
     * Interface required to be implemented by activity
     */
    public static interface OnSaveListener {
        /**
         * Fired when user click the save button
         *
         * @param position  position, use for modify.
         * @param hour      the hour
         * @param minute       the minute
         * @param dayFlag the dayFlag
         */
        public void onAlarmInfoSaved(int position, int hour, int minute, byte dayFlag);
    }

    /**
     * This will make sure that {@link OnSaveListener} interface is implemented by activity
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnSaveListener) activity;
        } catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnSaveListener");
        }
    }
}
