package com.romanpulov.library.gdrive;

import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class GDActionExecutor {
    private static final String TAG = GDActionExecutor.class.getSimpleName();

    public static <D> void execute(GDAbstractAction<D> action) {
        action.execute();
    }

    public static <D> D executeSync(GDAbstractAction<D> action) throws GDActionException {
        final CountDownLatch mLocker = new CountDownLatch(1);
        final AtomicReference<D> mData = new AtomicReference<>();
        final AtomicReference<GDActionException> mException = new AtomicReference<>();

        action.setGDActionListener(new OnGDActionListener<D>() {
            @Override
            public void onActionSuccess(D data) {
                Log.d(TAG, "Action success");
                mData.set(data);
                mLocker.countDown();
            }

            @Override
            public void onActionFailure(Exception e) {
                Log.d(TAG, "Action failure");
                mException.set(new GDActionException(e.getMessage()));
                mLocker.countDown();
            }
        });

        action.execute();

        try {
            mLocker.await();

            if (mException.get() != null) {
                throw mException.get();
            }

            return mData.get();

        } catch (InterruptedException e) {
            throw new GDActionException("Process interrupted");
        }
    }
}
