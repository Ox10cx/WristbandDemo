package com.realsil.android.wristbanddemo.utility;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;

import com.realsil.android.wristbanddemo.R;
import com.realsil.android.wristbanddemo.view.CycleWheelView;

import java.util.ArrayList;
import java.util.List;

public class ValuePickerFragment extends DialogFragment {
    private static Context mContext;
    private OnSaveListener mListener;

    // Type
    public final static int TYPE_AGE = 0;
    public final static int TYPE_HEIGHT = 1;
    public final static int TYPE_WEIGHT = 2;
    public final static int TYPE_TOTAL_STEP = 3;
    private int mType = TYPE_AGE;
    public static final String EXTRAS_VALUE_TYPE = "VALUE_TYPE";
    public static final String EXTRAS_VALUE_DEFAULT = "VALUE_DEFAULT";

    private int mDefaultValue = 0;

    private RelativeLayout mrlSave;
    private RelativeLayout mrlCancel;

    private CycleWheelView mcwvWristSetValue;
    /**
     * Static implementation of fragment so that it keeps data when phone orientation is changed.
     * For standard BLE Service UUID, we can filter devices using normal android provided command
     * startScanLe() with required BLE Service UUID
     * For custom BLE Service UUID, we will use class ScannerServiceParser to filter out required
     * device.
     */
    public static ValuePickerFragment getInstance(Context context) {
        final ValuePickerFragment fragment = new ValuePickerFragment();
        mContext = context;
        return fragment;
    }
    /**
     * When dialog is created then set AlertDialog with list and button views
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_value_picker, null);
        final AlertDialog dialog = builder.setView(dialogView).create();

        mType = getArguments().getInt(EXTRAS_VALUE_TYPE);
        mDefaultValue = getArguments().getInt(EXTRAS_VALUE_DEFAULT);
        Log.i("123", "mType: " + mType + ", mDefaultValue: " + mDefaultValue);

        // initial UI
        mcwvWristSetValue = (CycleWheelView)dialogView.findViewById(R.id.cwvWristSetValue);
        ArrayList<String> labels = new ArrayList<>();
        int min = 0;
        int max = 0;
        int times = 1;
        switch (mType) {
            case TYPE_AGE:
                min = 5;
                max = 100;
                break;
            case TYPE_HEIGHT:
                min = 40;
                max = 230;
                break;
            case TYPE_WEIGHT:
                min = 3;
                max = 150;
                break;
            case TYPE_TOTAL_STEP:
                min = 20;
                max = 300;
                times = 100;
                break;
        }
        for(int i = min; i <= max; i++) {
            labels.add(String.valueOf(i * times));
        }
        mcwvWristSetValue.setLabels(labels);
        mcwvWristSetValue.setSelection((mDefaultValue / times) - min);

        mrlSave = (RelativeLayout) dialogView.findViewById(R.id.rlWristValueSure);
        mrlSave.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int returnValue = 0;
                switch (mType) {
                    case TYPE_AGE:
                    case TYPE_HEIGHT:
                    case TYPE_WEIGHT:
                    case TYPE_TOTAL_STEP:
                        returnValue = Integer.valueOf(mcwvWristSetValue.getSelectLabel());
                        break;
                }

                // cancel
                dialog.cancel();
                // tell the saved
                mListener.onValueInfoSaved(mType, returnValue);
            }
        });

        mrlCancel = (RelativeLayout) dialogView.findViewById(R.id.rlWristValueCancel);
        mrlCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // cancel
                dialog.cancel();
            }
        });

        return dialog;
    }

    /**
     * Interface required to be implemented by activity
     */
    public static interface OnSaveListener {
        /**
         * Fired when user click the save button
         *
         * @param type      the value type
         * @param value     the select value
         */
        public void onValueInfoSaved(int type, int value);
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
