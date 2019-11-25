package com.mobiliteam.networkrepo;

import okhttp3.Call;

/**
 * Created by admin on 13/04/18.
 */

public interface IConnectionListener {

    void onResponse(Call call, NetworkResponse networkResponse);

    void onFailure(Call call, NetworkResponse networkResponse);

}
