package com.romanpulov.library.gdrive;

import android.os.SystemClock;

import java.util.concurrent.atomic.AtomicReference;

class GDAuthData {
    final static AtomicReference<String> mAuthCode = new AtomicReference<>();
    final static AtomicReference<String> mAccessToken = new AtomicReference<>();
    final static AtomicReference<Long> mAccessTokenExpireTime = new AtomicReference<>();

    static void setAccessToken(String accessToken) {
        mAuthCode.set("stub");
        clearAccessToken();
        mAccessToken.set(accessToken);
    }

    static void setAuthCode(String authCode) {
        if (authCode != null) {
            clearAccessToken();
        }
        mAuthCode.set(authCode);
    }

    static void clearAccessToken() {
        mAccessToken.set(null);
        mAccessTokenExpireTime.set(null);
    }

    static boolean accessTokenExists() {
        return mAccessToken.get() != null;
    }

    static boolean accessTokenExpired() {
        return (mAccessTokenExpireTime.get() != null) &&
                (SystemClock.elapsedRealtime() > GDAuthData.mAccessTokenExpireTime.get());
    }
}
