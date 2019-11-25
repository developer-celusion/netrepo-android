package com.mobiliteam.networkrepo;

/**
 * Created by admin on 14/04/18.
 */

public interface IRefreshTokenListener {

    void refreshed(NetworkResponse networkResponse);

    void needAppLogout(NetworkResponse networkResponse);

}
