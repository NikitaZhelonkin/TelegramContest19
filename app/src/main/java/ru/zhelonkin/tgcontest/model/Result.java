package ru.zhelonkin.tgcontest.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Result<T> {

    private final T mData;
    private final Throwable mError;

    public Result(@NonNull T data) {
        this(data, null);
    }

    public Result(@NonNull Throwable error) {
        this(null, error);
    }

    private Result(@Nullable T data, @Nullable Throwable error) {
        mData = data;
        mError = error;
    }

    public boolean isSuccess() {
        return mData != null;
    }

    @Nullable
    public Throwable getError() {
        return mError;
    }

    @Nullable
    public T getData() {
        return mData;
    }
}
