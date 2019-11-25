package com.mobiliteam.networkrepo;

import java.lang.reflect.Type;

import okhttp3.RequestBody;

/**
 * Created by admin on 13/04/18.
 */

public interface INetworkRepository {

    <T> NetworkResponse get(Class<T> modelClass, String url);

    NetworkResponse get(String url);

    <T> void get(final Class<T> modelClass, final String url, final IConnectionListener listener);

    <T> void get(final String url, final IConnectionListener listener);

    <T> void get(final Class<T> modelClass, final String url, final int requestID, final IConnectionListener listener);

    <T> void get(final String url, final int requestID, final IConnectionListener listener);

    <T> NetworkResponse getAll(Class<T> modelClass, String url);

    <T> NetworkResponse getAll(Type modelType, String url);

    <T> void getAll(final String url, final IConnectionListener listener);

    <T> void getAll(final String url, final int requestID, IConnectionListener listener);

    <T> void getAll(final Class<T> modelClass, final String url, final IConnectionListener listener);

    <T> void getAll(final Class<T> modelClass, final String url, final int requestID, final IConnectionListener listener);

    // With Extended Type

    <T> void getAll(final String url, final IConnectionListenerEx listener);

    <T> void getAll(final String url, final int requestID, IConnectionListenerEx listener);

    <T> void getRaw(final String url, final int requestID, IConnectionListenerEx listener);

    // Start POST Methods

    <T> NetworkResponse post(Class<T> modelClass, String url, T item);

    NetworkResponse post(String url, String jsonBody);

    NetworkResponse post(String url, RequestBody requestBody);

    NetworkResponse post(String url, ODataBatchRequest batchRequest);

    <T> void post(final Class<T> modelClass, final String url, final T item, final IConnectionListener listener);

    <T> void post(final Class<T> modelClass, final String url, final T item, final int requestID, final IConnectionListener listener);

    void post(String url, String jsonBody, IConnectionListener listener);

    void post(String url, String jsonBody, int requestID, IConnectionListener listener);

    void post(String url, RequestBody requestBody, IConnectionListener listener);

    void post(String url, RequestBody requestBody, int requestID, IConnectionListener listener);

    void post(String url, ODataBatchRequest batchRequest, IConnectionListener listener);

    void post(String url, ODataBatchRequest batchRequest, int requestID, IConnectionListener listener);

    <T> NetworkResponse put(Class<T> modelClass, String url, T item);

    <T> void put(final Class<T> modelClass, final String url, final T item, final IConnectionListener listener);

    <T> void put(final Class<T> modelClass, final String url, final T item, final int requestID, final IConnectionListener listener);

    <T> NetworkResponse patch(Class<T> modelClass, String url, T item);

    <T> void patch(final Class<T> modelClass, final String url, final T item, final IConnectionListener listener);

    <T> void patch(final Class<T> modelClass, final String url, final T item, final int requestID, final IConnectionListener listener);

    <T> NetworkResponse delete(Class<T> modelClass, String url, T item);

    <T> void delete(final Class<T> modelClass, final String url, final T item, final IConnectionListener listener);

    <T> void delete(final Class<T> modelClass, final String url, final T item, final int requestID, final IConnectionListener listener);

    NetworkResponse delete(String url);

    void delete(final String url, final IConnectionListener listener);

    void delete(final String url, final int requestID, final IConnectionListener listener);

    NetworkResponse count(final String url);

    void count(final String url, final IConnectionListener listener);

    void count(final String url, final int requestID, final IConnectionListener listener);

    <T> NetworkResponse update(final NetworkRepository.NetworkHttpMethod method, String url, String content);

    <T> void update(final NetworkRepository.NetworkHttpMethod method, final String url, final String content, final int requestID, final IConnectionListener listener);

}
