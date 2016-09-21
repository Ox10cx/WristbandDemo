package com.realsil.android.wristbanddemo.utility;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

import com.realsil.android.wristbanddemo.R;


public class AppHelpFragment extends DialogFragment {
    private static final String ARG_TEXT = "ARG_TEXT";
    private static final String ARG_VERSION = "ARG_VERSION";

    public static AppHelpFragment getInstance(final int aboutResId, final boolean appendVersion) {
        final AppHelpFragment fragment = new AppHelpFragment();

        final Bundle args = new Bundle();
        args.putInt(ARG_TEXT, aboutResId);
        args.putBoolean(ARG_VERSION, appendVersion);
        fragment.setArguments(args);

        return fragment;
    }

    public static AppHelpFragment getInstance(final int aboutResId) {
        final AppHelpFragment fragment = new AppHelpFragment();

        final Bundle args = new Bundle();
        args.putInt(ARG_TEXT, aboutResId);
        args.putBoolean(ARG_VERSION, false);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Bundle args = getArguments();
        final StringBuilder text = new StringBuilder(getString(args.getInt(ARG_TEXT)));

        final boolean appendVersion = args.getBoolean(ARG_VERSION);
        if (appendVersion) {
            try {
                final String version = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
                text.append(getString(R.string.about_version, version));
            } catch (final NameNotFoundException e) {
                // do nothing
            }
        }
        text.append(getString(R.string.about_copyright_text));
        return new AlertDialog.Builder(getActivity()).setTitle(R.string.about_title).setMessage(text).setPositiveButton(android.R.string.ok, null).create();
    }
}

