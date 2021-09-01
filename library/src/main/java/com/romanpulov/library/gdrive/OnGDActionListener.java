package com.romanpulov.library.gdrive;

public interface OnGDActionListener<D> {
    void onActionSuccess(D data);
    void onActionFailure(Exception exception);
}
