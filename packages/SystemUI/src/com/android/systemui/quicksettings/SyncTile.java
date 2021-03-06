/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.systemui.quicksettings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsContainerView;
import com.android.systemui.statusbar.phone.QuickSettingsController;

public class SyncTile extends QuickSettingsTile {

    private Object mSyncObserverHandle = null;
    private Handler mHandler;
    public static QuickSettingsTile mInstance;

    public static QuickSettingsTile getInstance(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, final QuickSettingsController qsc, Handler handler) {
        if (mInstance == null) mInstance = new SyncTile(context, inflater, container, qsc);
        return mInstance;
    }

    public SyncTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container,
            QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        updateTileState();

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleState();
                applySyncChanges();
            }
        };

        mOnLongClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent("android.settings.SYNC_SETTINGS");
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startSettingsActivity(intent);
                return true;
            }
        };
        mHandler = new Handler();
    }

    @Override
    public void setupQuickSettingsTile() {
        super.setupQuickSettingsTile();

        if(mSyncObserverHandle != null) {
            //Unregistering sync state listener
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        } else {
            // Registering sync state listener
            mSyncObserverHandle = ContentResolver.addStatusChangeListener(
                    ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS, mSyncObserver);
        }
    }

    private void applySyncChanges() {
        updateTileState();
        updateQuickSettings();
    }

    protected void toggleState() {
        // If ON turn OFF else turn ON
        if (getSyncState()) {
            ContentResolver.setMasterSyncAutomatically(false);
        } else {
            ContentResolver.setMasterSyncAutomatically(true);
        }
    }

    private void updateTileState() {
        // Get the initial label
        mLabel = mContext.getString(R.string.quick_settings_sync);

        if (getSyncState()) {
            mDrawable = R.drawable.ic_qs_sync_on;
        } else {
            mDrawable = R.drawable.ic_qs_sync_off;
            mLabel += " " + mContext.getString(R.string.quick_settings_label_disabled);
        }
    }

    private boolean getSyncState() {
        return ContentResolver.getMasterSyncAutomatically();
    }

    private SyncStatusObserver mSyncObserver = new SyncStatusObserver() {
        public void onStatusChanged(int which) {
            // update state/view if something happened
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    applySyncChanges();
                }
            });
        }
    };
}
