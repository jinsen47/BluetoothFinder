package com.jinsen.bluetoothfinder.UI;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.RingtonePreference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jinsen.bluetoothfinder.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SetupFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SetupFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SetupFragment extends PreferenceFragment {
    public static final String TAG = "SetupFragment";

    //Preference Keys
    public static final String KEY_ALARM = "pref_key_alarm";
    public static final String KEY_TIME = "pref_key_time";
    public static final String KEY_DEVICE = "pref_key_device";

    private RingtonePreference mRingtone;
    private ListPreference mTime;
    private Preference mDevice;


    // TODO: Rename parameter arguments, choose names that match

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment SetupFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SetupFragment newInstance() {
        SetupFragment fragment = new SetupFragment();
        return fragment;
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
                //Set back a bundle to MainActivity
                Bundle bundle = new Bundle();
                bundle.putString(KEY_DEVICE, address);
                onItemChanged(bundle);

                SharedPreferences sp = mDevice.getPreferenceManager().getSharedPreferences();
                sp.edit().putString(KEY_DEVICE, address).commit();
            } else Log.e("DeviceList", resultCode + "");
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onItemChanged (Bundle bundle) {
        if (mListener != null) {
            mListener.onItemChanged(bundle);
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

    public class SetupChangeListener implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference instanceof RingtonePreference) {
                RingtonePreference temp = ((RingtonePreference) preference);
                Uri uri = Uri.parse(newValue.toString());
                temp.setSummary(getRingtoneName(uri));
                SharedPreferences sp = temp.getPreferenceManager().getSharedPreferences();
                sp.edit().putString(KEY_ALARM, newValue.toString()).commit();

                Bundle bundle = new Bundle();
                bundle.putString(KEY_ALARM, newValue.toString());
                onItemChanged(bundle);

                Log.d("SetupFragment:uri=", uri.toString());
                return true;

            }else if (preference instanceof ListPreference) {
                ListPreference temp = ((ListPreference) preference);
                temp.setSummary(newValue.toString());
                SharedPreferences sp = temp.getPreferenceManager().getSharedPreferences();
                int realtime = ((Integer.valueOf(newValue.toString()).intValue()) + 1 ) * 5;
                sp.edit().putString(KEY_TIME, realtime + "").commit();

                Log.d("SetupFragment:time=", newValue.toString());

                Bundle bundle = new Bundle();
                bundle.putString(KEY_TIME, realtime + "");
                onItemChanged(bundle);

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
        public void onItemChanged(Bundle bundle);
    }
//        public void startActivityForResult(Intent intent) {
//
//        }
}
