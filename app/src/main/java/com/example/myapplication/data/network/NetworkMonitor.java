package com.example.myapplication.data.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

public class NetworkMonitor extends LiveData<Boolean> {
    private final ConnectivityManager connectivityManager;
    private final ConnectivityManager.NetworkCallback networkCallback;// NetworkCallback — реагирует на изменения сети в реальном времени

    public NetworkMonitor(Context context) {
        connectivityManager = (ConnectivityManager)
                context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback() {

            // сеть появилась
            @Override
            public void onAvailable(@NonNull Network network) {
                postValue(true);
            }

            //  пропала
            @Override
            public void onLost(@NonNull Network network) {
                postValue(false);
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network,
                                              @NonNull NetworkCapabilities capabilities) {
                // NET_CAPABILITY_INTERNET — сеть имеет доступ к интернету
                // NET_CAPABILITY_VALIDATED — интернет р
                boolean hasRealInternet =
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                postValue(hasRealInternet);
            }
        };
    }

    @Override
    protected void onActive() {
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, networkCallback);

        checkCurrentState();
    }

    @Override
    protected void onInactive() {
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }

    private void checkCurrentState() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork != null) {
            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(activeNetwork);
            postValue(capabilities != null
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
        } else {
            postValue(false);
        }
    }
}