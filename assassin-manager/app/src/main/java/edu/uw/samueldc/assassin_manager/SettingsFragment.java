package edu.uw.samueldc.assassin_manager;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

/**
 * Created by Emile on 5/28/16.
 */
public class SettingsFragment extends PreferenceFragment {
    public SettingsFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);
    }
}
