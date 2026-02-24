package com.ismartapps.reachalert;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.unity3d.ads.IUnityAdsInitializationListener;
//import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsShowOptions;

public class AdsUnity extends AppCompatActivity implements IUnityAdsInitializationListener {

    private static final String TAG = "Unity Ads Script";
    private final String placementID = "Rewarded_Android";
    private final Boolean testMode = false;
    private Context context;
    private final boolean showed = false;

    final String unityGameID = "4257683";

    private final String adUnitId = "rewardedVideo";

    private final IUnityAdsLoadListener loadListener = new IUnityAdsLoadListener() {
        @Override
        public void onUnityAdsAdLoaded(String placementId) {
            UnityAds.show((Activity) getApplicationContext(), adUnitId, new UnityAdsShowOptions(), showListener);
        }

        @Override
        public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
            Log.e("UnityAdsExample", "Unity Ads failed to load ad for " + placementId + " with error: [" + error + "] " + message);
        }
    };


    private final IUnityAdsShowListener showListener = new IUnityAdsShowListener() {
        @Override
        public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
            Log.e("UnityAdsExample", "Unity Ads failed to show ad for " + placementId + " with error: [" + error + "] " + message);
            goBack(true);
        }

        @Override
        public void onUnityAdsShowStart(String placementId) {
            Log.v("UnityAdsExample", "onUnityAdsShowStart: " + placementId);
        }

        @Override
        public void onUnityAdsShowClick(String placementId) {
            Log.v("UnityAdsExample", "onUnityAdsShowClick: " + placementId);
        }

        @Override
        public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState state) {
            Log.v("UnityAdsExample", "onUnityAdsShowComplete: " + placementId);
            goBack(state.equals(UnityAds.UnityAdsShowCompletionState.COMPLETED));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loading_screen);
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        boolean dark = settings.getBoolean("dark", false);

        if (dark) {
            TextView text = findViewById(R.id.textView);
            text.setTextColor(Color.WHITE);
            CardView card = findViewById(R.id.card);
            card.setCardBackgroundColor(Color.BLACK);
        }
        context = this;
        goBack(true);
//        init();
    }

    private void init() {
        if (testMode) {
            goBack(false);
            return;
        }
        Log.d(TAG, "init: " + UnityAds.isInitialized());
        UnityAds.initialize(getApplicationContext(), unityGameID, testMode, this);
    }

    @Override
    public void onInitializationComplete() {
        DisplayRewardedAd();
    }

    @Override
    public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
        Log.e("UnityAdsExample", "Unity Ads initialization failed with error: [" + error + "] " + message);
    }

    public void DisplayRewardedAd () {
        UnityAds.load(adUnitId, loadListener);
    }

    private void goBack(boolean status) {
        Intent backIntent = new Intent();
        backIntent.putExtra("ad_status", status);
        setResult(132, backIntent);
        finish();
    }

}
