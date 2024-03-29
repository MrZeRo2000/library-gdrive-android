package com.romanpulov.library.gdrive.testapp;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.romanpulov.library.gdrive.GDBaseHelper;
import com.romanpulov.library.gdrive.GDConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GDHelper extends GDBaseHelper {
    private static final int REQUEST_CODE_SIGN_IN = 1543;

    @Override
    protected void configure() {
        GDConfig.configure(R.raw.gd_config, "https://www.googleapis.com/auth/drive", REQUEST_CODE_SIGN_IN);
    }

    private static GDHelper instance;

    private GDHelper() {
        super();
    }

    public static GDHelper getInstance() {
        if (instance == null) {
            instance = new GDHelper();
        }
        return instance;
    }

    public static void handleActivityResult(Context context, int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            GoogleSignIn.getSignedInAccountFromIntent(data)
                    .addOnSuccessListener(account -> {
                        Toast.makeText(context, "Signed in", Toast.LENGTH_SHORT).show();
                        GDHelper.getInstance().setServerAuthCode(account.getServerAuthCode());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error signing in:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    public static File[] generateFiles(Context context) {
        String[] fileStrings = new String[] {"1-dfwkerkwerw", "2-6,vdfgdfg", "3-gkkkrk444", "4-,fkfkfkdrtrrewwerwer"};
        File[] files = new File[fileStrings.length];

        for (int i = 0; i < fileStrings.length; i++) {
            try {
                File file = new File(context.getFilesDir().getAbsolutePath() + File.separator + "testfilename_" + i);
                try (FileOutputStream outputStream = new FileOutputStream(file.getAbsolutePath())) {
                    outputStream.write(fileStrings[i].getBytes(StandardCharsets.UTF_8));
                }
                files[i] = file;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return files;
    }
}
