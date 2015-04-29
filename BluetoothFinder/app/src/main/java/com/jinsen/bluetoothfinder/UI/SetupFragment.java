package com.jinsen.bluetoothfinder.UI;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.RingtonePreference;
import android.util.Log;

import com.jinsen.bluetoothfinder.Events.AddressMessage;
import com.jinsen.bluetoothfinder.Events.AlarmMessage;
import com.jinsen.bluetoothfinder.Events.StartupMessage;
import com.jinsen.bluetoothfinder.Events.TimeMessage;
import com.jinsen.bluetoothfinder.R;

import de.greenrobot.event.EventBus;

public class SetupFragment extends PreferenceFragment {
    public static final String TAG = "SetupFragment";

    //Preference Keys
    public static final String KEY_ALARM = "pref_key_alarm";
    public static final String KEY_TIME = "pref_key_time";
    public static final String KEY_DEVICE = "pref_key_device";

    private RingtonePreference mRingtone;
    private ListPreference mTime;
    private Preference mDevice;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment SetupFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SetupFragment newInstance() {
        return new SetupFragment();
    }

    public SetupFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.perference);
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mRingtone = ((RingtonePreference) findPreference(KEY_ALARM));
        mDevice = findPreference(KEY_DEVICE);
        mTime = ((ListPreference) findPreference(KEY_TIME));

        mRingtone.setOnPreferenceChangeListener(new SetupChangeListener());
        mDevice.setOnPreferenceChangeListener(new SetupChangeListener());
        mTime.setOnPreferenceChangeListener(new SetupChangeListener());

        mDevice.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent();
                intent.setClass(getActivity(), DeviceListActivity.class);
                startActivityForResult(intent, MainActivity.REQUEST_CONNECT_DEVICE);
                return true;
            }
        });

        // Load cache
        SharedPreferences sp = getPreferenceManager().getSharedPreferences();
        String cacheAlarm = sp.getString(KEY_ALARM, "");
        String cacheDevice = sp.getString(KEY_DEVICE, "");
        int cacheTime = 5;
        mRingtone.setSummary(cacheAlarm);
        mDevice.setSummary(cacheDevice);
        String tempString = sp.getString(KEY_TIME, "false");
        if (!tempString.equals("false")) {
            int realTime = (Integer.valueOf(tempString) + 1) * 5;
            mTime.setSummary(realTime + "");
        }
        EventBus.getDefault().post(new StartupMessage(cacheAlarm, cacheTime, cacheDevice));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MainActivity.REQUEST_CONNECT_DEVICE){
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                        .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                mDevice.setSummary(address);

                EventBus.getDefault().post(new AddressMessage(address));

                SharedPreferences sp = mDevice.getPreferenceManager().getSharedPreferences();
                sp.edit().putString(KEY_DEVICE, address).apply();
            } else Log.e("DeviceList", resultCode + "");
        }
    }

    public class SetupChangeListener implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference instanceof RingtonePreference) {
                RingtonePreference temp = ((RingtonePreference) preference);
                Uri uri = Uri.parse(newValue.toString());
                temp.setSummary(getRingtoneName(uri));
                SharedPreferences sp = temp.getPreferenceManager().getSharedPreferences();
                sp.edit().putString(KEY_ALARM, newValue.toString()).apply();

                EventBus.getDefault().post(new AlarmMessage(newValue.toString()));

                Log.d("SetupFragment:uri=", uri.toString());
                return true;

            }else if (preference instanceof ListPreference) {
                ListPreference temp = ((ListPreference) preference);

                // preferences may be changed auto-ly after modified, these code dont work
                SharedPreferences sp = temp.getPreferenceManager().getSharedPreferences();
                int realtime = ((Integer.valueOf(newValue.toString())) + 1 ) * 5;
                sp.edit().putString(KEY_TIME, newValue.toString()).apply();
                temp.setSummary(realtime + "");

                Log.d("SetupFragment:time=", newValue.toString());

                EventBus.getDefault().post(new TimeMessage(realtime));

                return true;
            }else {
//                preference.setSummary(((BluetoothDevice) newValue).getAddress());
//                SharedPreferences sp = preference.getPreferenceManager().getSharedPreferences();
//                sp.edit().putString("pref_key_device", newValue.toString()).commit();
//                onItemChanged(Uri.parse(newValue.toString()));
                  return true;
            }
        }
    }

    private String getRingtoneName(Uri uri) {
        Ringtone r = RingtoneManager.getRingtone(this.getActivity(), uri);
        return r.getTitle(this.getActivity());
    }
}
