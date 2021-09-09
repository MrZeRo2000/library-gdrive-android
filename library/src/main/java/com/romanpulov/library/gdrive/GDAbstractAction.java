package com.romanpulov.library.gdrive;

public abstract class GDAbstractAction<D> {
    protected OnGDActionListener<D> mGDActionListener;

    public void setGDActionListener(OnGDActionListener<D> mGDActionListener) {
        this.mGDActionListener = mGDActionListener;
    }

    public GDAbstractAction(OnGDActionListener<D> gdActionListener) {
        this.mGDActionListener = gdActionListener;
    }

    public abstract void execute();

    protected void notifyError(Exception e) {
        if (mGDActionListener != null) {
            mGDActionListener.onActionFailure(e);
        }
    }

    protected void notifySuccess(D data) {
        if (mGDActionListener != null) {
            mGDActionListener.onActionSuccess(data);
        }
    }
}
