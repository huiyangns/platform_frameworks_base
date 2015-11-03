/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.printspooler.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.printservice.PrintService;
import android.util.ArraySet;

import java.util.Set;

/**
 * Manage approved print services. These services are stored in the shared preferences.
 */
public class ApprovedPrintServices {
    /**
     * Used for locking accesses to the approved services.
     */
    static final public Object sLock = new Object();

    private static final String APPROVED_SERVICES_PREFERENCE = "PRINT_SPOOLER_APPROVED_SERVICES";
    private final SharedPreferences mPreferences;

    /**
     * Create a new {@link ApprovedPrintServices}
     *
     * @param owner The {@link Context} using this object.
     */
    public ApprovedPrintServices(Context owner) {
        mPreferences = owner.getSharedPreferences(APPROVED_SERVICES_PREFERENCE,
                Context.MODE_PRIVATE);
    }

    /**
     * Get {@link Set} of approved services.
     *
     * @return A {@link Set} containing all currently approved services.
     */
    public Set<String> getApprovedServices() {
        return mPreferences.getStringSet(APPROVED_SERVICES_PREFERENCE, null);
    }

    /**
     * Check if a {@link PrintService} is approved.
     *
     * This function does not acquire the {@link #sLock}.
     *
     * @param service The {@link ComponentName} of the {@link PrintService} that might be approved
     * @return true iff the service is currently approved
     */
    public boolean isApprovedService(ComponentName service) {
        final Set<String> approvedServices = getApprovedServices();

        if (approvedServices != null) {
            final String flattenedString = service.flattenToShortString();

            for (String approvedService : approvedServices) {
                if (approvedService.equals(flattenedString)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Add a {@link PrintService} to the list of approved print services.
     *
     * @param serviceToAdd The {@link ComponentName} of the {@link PrintService} to be approved.
     */
    public void addApprovedService(ComponentName serviceToAdd) {
        synchronized (sLock) {
            Set<String> oldApprovedServices =
                    mPreferences.getStringSet(APPROVED_SERVICES_PREFERENCE, null);

            Set<String> newApprovedServices;
            if (oldApprovedServices == null) {
                newApprovedServices = new ArraySet<String>(1);
            } else {
                // Copy approved services.
                newApprovedServices = new ArraySet<String>(oldApprovedServices);
            }
            newApprovedServices.add(serviceToAdd.flattenToShortString());

            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putStringSet(APPROVED_SERVICES_PREFERENCE, newApprovedServices);
            editor.apply();
        }
    }

    /**
     * Add a {@link OnSharedPreferenceChangeListener} that listens for changes to the approved
     * services. Should only be called while holding {@link #sLock} to synchronize against
     * {@link #addApprovedService}.
     *
     * @param listener {@link OnSharedPreferenceChangeListener} to register
     */
    public void registerChangeListenerLocked(OnSharedPreferenceChangeListener listener) {
        mPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    /**
     * Unregister a listener registered in {@link #registerChangeListenerLocked}.
     *
     * @param listener {@link OnSharedPreferenceChangeListener} to unregister
     */
    public void unregisterChangeListener(OnSharedPreferenceChangeListener listener) {
        mPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    /**
     * If a {@link PrintService} is approved, remove it from the list of approved services.
     *
     * @param serviceToRemove The {@link ComponentName} of the {@link PrintService} to be removed
     */
    public void removeApprovedService(ComponentName serviceToRemove) {
        synchronized (sLock) {
            if (isApprovedService(serviceToRemove)) {
                // Copy approved services.
                ArraySet<String> approvedServices = new ArraySet<String>(
                        mPreferences.getStringSet(APPROVED_SERVICES_PREFERENCE, null));

                SharedPreferences.Editor editor = mPreferences.edit();

                final int numApprovedServices = approvedServices.size();
                for (int i = 0; i < numApprovedServices; i++) {
                    if (approvedServices.valueAt(i)
                            .equals(serviceToRemove.flattenToShortString())) {
                        approvedServices.removeAt(i);
                        break;
                    }
                }

                editor.putStringSet(APPROVED_SERVICES_PREFERENCE, approvedServices);
                editor.apply();
            }
        }
    }
}
