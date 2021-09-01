package com.romanpulov.library.gdrive;

public abstract class GDAbstractAction<D> {
    protected OnGDActionListener<D> mGDActionListener;

    public OnGDActionListener<D> getGDActionListener() {
        return mGDActionListener;
    }

    public void setGDActionListener(OnGDActionListener<D> mGDActionListener) {
        this.mGDActionListener = mGDActionListener;
    }

    public GDAbstractAction(OnGDActionListener<D> mGDActionListener) {
        this.mGDActionListener = mGDActionListener;
    }

    public abstract void execute();
}
