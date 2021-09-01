package com.romanpulov.library.gdrive;

/**
 * Google Drive configuration class
 */
public class GDConfig {
    private int mConfigResId;
    private String mScope;

    public int getConfigResId() {
        return mConfigResId;
    }

    public String getScope() {
        return mScope;
    }

    private static GDConfig instance;

    private GDConfig(int mConfigResId, String mScope) {
        this.mConfigResId = mConfigResId;
        this.mScope = mScope;
    }

    public static GDConfig configure(int configResId, String scope) {
        if (instance == null) {
            instance = new GDConfig(configResId, scope);
        } else {
            instance.mConfigResId = configResId;
            instance.mScope = scope;
        }

        return instance;
    }

    public static GDConfig get() {
        if (instance == null) {
            throw new RuntimeException("GD configuration not complete. Please, configure first");
        }
        return instance;
    }
}
