package com.mobiliteam.networkrepoexample;

import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.mobiliteam.networkrepo.IConnectionListener;
import com.mobiliteam.networkrepo.NetworkRepository;
import com.mobiliteam.networkrepo.NetworkResponse;
import com.mobiliteam.networkrepo.ODataBatchRequest;
import com.mobiliteam.networkrepoexample.databinding.ActivityMainBinding;
import com.mobiliteam.networkrepoexample.model.SampleFuelType;

import java.util.LinkedHashMap;
import java.util.List;

import okhttp3.Call;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final String ODATE_ENDPOINT = "http://fg.mobiliteam.in/odata/FuelType";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        attachListeners();
    }

    private void batchReq() {

        final ODataBatchRequest batchRequest = new ODataBatchRequest.Builder()
                .addRequest(NetworkRepository.NetworkHttpMethod.PATCH, "Contact(353)", "{\"Name\":\"Swapnil N\"}")
                .addRequest(NetworkRepository.NetworkHttpMethod.GET, "Contact(353)")
                .addRequest(NetworkRepository.NetworkHttpMethod.DELETE, "ContactProduct(2)")
                .addRequest(NetworkRepository.NetworkHttpMethod.DELETE, "ContactProduct(4)").build();

        NetworkRepository.getInstance(getApplicationContext()).post("http://63moonsfa.mobiliteam.in/odata/$batch", batchRequest, new IConnectionListener() {
            @Override
            public void onResponse(Call call, NetworkResponse networkResponse) {
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
                List<NetworkResponse> networkResponses = batchRequest.parseBatchResponse((String) networkResponse.getResponse());
                for (NetworkResponse batchResponse : networkResponses) {
                    Log.i(this.getClass().getSimpleName(), "" + batchResponse.isSuccess());
                }
            }

            @Override
            public void onFailure(Call call, NetworkResponse networkResponse) {
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
            }
        });


    }

    private void attachListeners() {
        binding.buttonGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRequest();
            }
        });
        binding.buttonEnqueue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enqueueMethod();
            }
        });
        binding.buttonPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                postRequest();
            }
        });
        binding.buttonPut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                putRequest();
            }
        });
        binding.buttonPatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                patchRequest();
            }
        });
        binding.buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteRequest();
            }
        });
        binding.buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginRequest();
            }
        });
        binding.buttonCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                countRequest();
            }
        });
        binding.buttonBatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                batchReq();
                ;
            }
        });
    }

    private void countRequest() {
//        AsyncTask.execute(new Runnable() {
//            @Override
//            public void run() {
//                NetworkResponse networkResponse = NetworkRepository.getInstance(getApplicationContext()).count(ODATE_ENDPOINT + "/$count");
//                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
//            }
//        });

        NetworkRepository.getInstance(getApplicationContext()).count(ODATE_ENDPOINT + "/$count", new IConnectionListener() {
            @Override
            public void onResponse(Call call, NetworkResponse networkResponse) {
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
            }

            @Override
            public void onFailure(Call call, NetworkResponse networkResponse) {
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
            }
        });
    }

    private void loginRequest() {
        final String url = "https://www.birlashaktiman.in:7007/IndogulfAPI/oauth/token";
        LinkedHashMap<String, String> loginParams = new LinkedHashMap<>();
        loginParams.put("username", "sarab");
        loginParams.put("password", "SARAB123");
        loginParams.put("grant_type", "password");
        loginParams.put("scope", "read write trust");
        loginParams.put("client_secret", "secret");
        loginParams.put("client_id", "my-trusted-client");

        LinkedHashMap<String, String> refreshParams = new LinkedHashMap<>();
        refreshParams.put("grant_type", "password");
        refreshParams.put("scope", "read write trust");
        refreshParams.put("client_secret", "secret");
        refreshParams.put("client_id", "my-trusted-client");

        NetworkRepository.getInstance(getApplicationContext()).login(url, loginParams, refreshParams, null, new IConnectionListener() {
            @Override
            public void onResponse(Call call, NetworkResponse networkResponse) {
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
            }

            @Override
            public void onFailure(Call call, NetworkResponse networkResponse) {
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
            }
        });
    }

    private void deleteRequest() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                NetworkResponse networkResponse = NetworkRepository.getInstance(getApplicationContext()).delete(ODATE_ENDPOINT + "(4)");
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
            }
        });

        NetworkRepository.getInstance(getApplicationContext()).delete(ODATE_ENDPOINT + "(3)", new IConnectionListener() {
            @Override
            public void onResponse(Call call, NetworkResponse networkResponse) {
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
            }

            @Override
            public void onFailure(Call call, NetworkResponse networkResponse) {
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
            }
        });

    }

    private void patchRequest() {
        final SampleFuelType sampleFuelType = new SampleFuelType("PATCH REQ");
        sampleFuelType.setId(3);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                NetworkResponse networkResponse = NetworkRepository.getInstance(getApplicationContext()).patch(SampleFuelType.class, ODATE_ENDPOINT + "(3)", sampleFuelType);
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
            }
        });

//        NetworkRepository.getInstance(getApplicationContext()).patch(SampleFuelType.class, ODATE_ENDPOINT + "(3)", sampleFuelType, new IConnectionListener() {
//            @Override
//            public void onResponse(Call call, NetworkResponse networkResponse) {
//                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
//            }
//
//            @Override
//            public void onFailure(Call call, NetworkResponse networkResponse) {
//                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
//            }
//        });

    }

    private void putRequest() {
        final SampleFuelType sampleFuelType = new SampleFuelType("PUT REQ ASYNC");
        sampleFuelType.setId(2);
//        AsyncTask.execute(new Runnable() {
//            @Override
//            public void run() {
//                NetworkResponse networkResponse = NetworkRepository.getInstance(getApplicationContext()).put(SampleFuelType.class, ODATE_ENDPOINT + "(2)", sampleFuelType);
//                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
//            }
//        });

        NetworkRepository.getInstance(getApplicationContext()).put(SampleFuelType.class, ODATE_ENDPOINT + "(2)", sampleFuelType, new IConnectionListener() {
            @Override
            public void onResponse(Call call, NetworkResponse networkResponse) {
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
            }

            @Override
            public void onFailure(Call call, NetworkResponse networkResponse) {
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
            }
        });

    }

    private void postRequest() {
        final SampleFuelType sampleFuelType = new SampleFuelType("Swapnil N");

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                NetworkResponse networkResponse = NetworkRepository.getInstance(getApplicationContext()).post(SampleFuelType.class, ODATE_ENDPOINT, sampleFuelType);
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
            }
        });

//        NetworkRepository.getInstance(getApplicationContext()).post(SampleFuelType.class, ODATE_ENDPOINT, sampleFuelType, new IConnectionListener() {
//            @Override
//            public void onResponse(Call call, NetworkResponse networkResponse) {
//                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
//            }
//
//            @Override
//            public void onFailure(Call call, NetworkResponse networkResponse) {
//                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
//            }
//        });

    }

    private void getRequest() {

//        AsyncTask.execute(new Runnable() {
//            @Override
//            public void run() {
//                final NetworkResponse networkResponse = NetworkRepository.getInstance(getApplicationContext()).get(SampleFuelType.class, ODATE_ENDPOINT + "(1)");
//                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
//            }
//        });

        NetworkRepository.getInstance(getApplicationContext()).get("https://www.birlashaktiman.in:7007/IndogulfAPI/category_mstr/updates/revid/0", new IConnectionListener() {
            @Override
            public void onResponse(Call call, NetworkResponse networkResponse) {
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
            }

            @Override
            public void onFailure(Call call, NetworkResponse networkResponse) {
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
            }
        });

    }

    private void getAllRequest() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final NetworkResponse networkResponse = NetworkRepository.getInstance(getApplicationContext()).getAll(SampleFuelType.class, ODATE_ENDPOINT);
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
            }
        });

        NetworkRepository.getInstance(getApplicationContext()).getAll(SampleFuelType.class, ODATE_ENDPOINT, new IConnectionListener() {
            @Override
            public void onResponse(Call call, NetworkResponse networkResponse) {
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
            }

            @Override
            public void onFailure(Call call, NetworkResponse networkResponse) {
                Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
            }
        });

    }

    private void enqueueMethod() {
        NetworkRepository.getInstance(getApplicationContext()).getAll(SampleFuelType.class, ODATE_ENDPOINT, 1, enqueueListener);
        NetworkRepository.getInstance(getApplicationContext()).get(SampleFuelType.class, ODATE_ENDPOINT + "(1)", 2, enqueueListener);
    }

    private IConnectionListener enqueueListener = new IConnectionListener() {
        @Override
        public void onResponse(Call call, NetworkResponse networkResponse) {
            Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
        }

        @Override
        public void onFailure(Call call, NetworkResponse networkResponse) {
            Log.i(this.getClass().getSimpleName(), "" + networkResponse.isSuccess());
        }
    };
}
