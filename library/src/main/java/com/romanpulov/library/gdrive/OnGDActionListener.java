package com.romanpulov.library.gdrive;

public interface OnGDActionListener<D> {
    void onActionSuccess(int action, D data);
    void onActionFailure(int action, Exception exception);
}
