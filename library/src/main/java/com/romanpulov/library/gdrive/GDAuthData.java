package com.romanpulov.library.gdrive;

import java.util.concurrent.atomic.AtomicReference;

class GDAuthData {
    final static AtomicReference<String> mAuthCode = new AtomicReference<>();
    final static AtomicReference<String> mAccessToken = new AtomicReference<>();
    final static AtomicReference<Long> mAccessTokenExpireTime = new AtomicReference<>();

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
}
