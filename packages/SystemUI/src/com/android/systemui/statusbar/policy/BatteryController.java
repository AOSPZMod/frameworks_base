/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy;

import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter.BluetoothStateChangeCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.util.Slog;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.provider.Settings;
import android.view.View;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.CharacterStyle;
import com.android.systemui.R;

public class BatteryController extends BroadcastReceiver {
    private static final String TAG = "StatusBar.BatteryController";

    private Context mContext;
    private ArrayList<ImageView> mIconViews = new ArrayList<ImageView>();
    private ArrayList<TextView> mLabelViews = new ArrayList<TextView>();
    private TextView mBatteryText;
    private int mBatteryStyle;
    
    private ArrayList<BatteryStateChangeCallback> mChangeCallbacks =
            new ArrayList<BatteryStateChangeCallback>();

    public interface BatteryStateChangeCallback {
        public void onBatteryLevelChanged(int level, boolean pluggedIn);
    }

    public BatteryController(Context context) {
        mContext = context;
		mBatteryText = new TextView(mContext);
		
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(this, filter);

        SettingsObserver settingsObserver = new SettingsObserver(new Handler());
        settingsObserver.observe();
    }

    public void addIconView(ImageView v) {
        mIconViews.add(v);
    }

    public void addLabelView(TextView v) {
        mLabelViews.add(v);
    }
    
    public void addBatteryTextView(TextView v) {
        mBatteryText = v;
    }
    
    public void addStateChangedCallback(BatteryStateChangeCallback cb) {
        mChangeCallbacks.add(cb);
    }

    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
           
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
               
            final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            final int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);

            boolean plugged = false;
            switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING: 
                case BatteryManager.BATTERY_STATUS_FULL:
                    plugged = true;
                    break;
            }

            setBatteryData(level,plugged);
		
            for (BatteryStateChangeCallback cb : mChangeCallbacks) {
                cb.onBatteryLevelChanged(level, plugged);
            }
        }
    }
    
    private void setIconsVisibility(int visible)
    {
       if(mBatteryText!=null)
			for (int i=0; i< mIconViews.size(); i++)
	    		mIconViews.get(i).setVisibility(visible);
    }
    
    private void setBatteryData(int level, boolean plugged)
    {
            final int icon = plugged ? R.drawable.stat_sys_battery_charge
                                     : R.drawable.stat_sys_battery;

            int N = mIconViews.size();
            for (int i=0; i<N; i++) {
                ImageView v = mIconViews.get(i);
                v.setImageResource(icon);
                v.setImageLevel(level);
                v.setContentDescription(mContext.getString(R.string.accessibility_battery_level,
                        level));
            }
            N = mLabelViews.size();
            for (int i=0; i<N; i++) {
                TextView v = mLabelViews.get(i);
                v.setText(mContext.getString(R.string.status_bar_settings_battery_meter_format,
                        level));
            }

            SpannableStringBuilder formatted = new SpannableStringBuilder(
                    Integer.toString(level) + "%");
            CharacterStyle style = new RelativeSizeSpan(0.7f); // beautiful
                                                               // formatting
            if (level < 10) { // level < 10, 2nd char is %
                formatted.setSpan(style, 1, 2,
                        Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            } else if (level < 100) { // level 10-99, 3rd char is %
                formatted.setSpan(style, 2, 3,
                        Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            } else { // level 100, 4th char is %
                formatted.setSpan(style, 3, 4,
                        Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            }

	    	mBatteryText.setText(formatted);
	    		
	     	updateSettings();
    }


    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUSBAR_BATTERY_ICON), false,
                    this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUSBAR_COLOR), false,
                    this);         
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }
    
    private void updateSettings() {
     
        ContentResolver resolver = mContext.getContentResolver();
        mBatteryStyle = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_BATTERY_ICON, 1);
        int iconColor = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_COLOR,-1);
                
        mBatteryText.setTextColor(iconColor);     
        switch (mBatteryStyle) {
            case 1:
		  		mBatteryText.setVisibility(View.VISIBLE);
				setIconsVisibility(View.GONE);
                break;
            default:
                mBatteryText.setVisibility(View.GONE);
                setIconsVisibility(View.VISIBLE);
                break;
        }
    }
    
    
}
