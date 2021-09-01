package com.romanpulov.library.gdrive;

public class GDActionExecutor {
    private static final String TAG = GDActionExecutor.class.getSimpleName();

    public static <D> void execute(GDAbstractAction<D> action) {
        action.execute();
    }
}
