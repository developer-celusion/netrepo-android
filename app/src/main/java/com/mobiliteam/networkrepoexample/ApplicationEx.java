package com.mobiliteam.networkrepoexample;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.mobiliteam.networkrepo.ODataBatchRequest;
import com.mobiliteam.networkrepo.IRefreshTokenListener;
import com.mobiliteam.networkrepo.NetworkRepository;
import com.mobiliteam.networkrepo.NetworkResponse;

/**
 * Created by swapnilnandgave on 16/04/18.
 */

public class ApplicationEx extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        try {
            ProviderInstaller.installIfNeeded(context);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }

        NetworkRepository.getInstance(getApplicationContext()).setup(60, false,true);
        NetworkRepository.getInstance(getApplicationContext()).setRefreshTokenListener(new IRefreshTokenListener() {
            @Override
            public void refreshed(NetworkResponse networkResponse) {
                Log.i(this.getClass().getSimpleName(), "Old Refresh Token is refreshed - " + networkResponse.getResponse().toString());
            }

            @Override
            public void needAppLogout(NetworkResponse networkResponse) {
                Log.i(this.getClass().getSimpleName(), "This Refresh Token is not refreshed - " + networkResponse.getResponse().toString());
            }
        });

    }


    public static Context getContext() {
        return context;
    }

    @Override
    public void onTerminate() {
        context = null;
        super.onTerminate();
    }

}
