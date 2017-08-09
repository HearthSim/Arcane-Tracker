package net.mbonnin.arcanetracker;

public class Lce<T> {
    private final Throwable mError;
    T mData;

    private Lce(T data, Throwable error) {
        this.mData = data;
        this.mError = error;
    }
    public static <T> Lce<T> data(T data) {
        return new Lce<T>(data, null);
    }
    public static <T> Lce<T> error(Throwable error) {
        return new Lce<T>(null, error);
    }
    public static <T> Lce<T> loading() {
        return new Lce<T>(null, null);
    }
    public boolean isLoading() {
        return mData == null && mError == null;
    }
    public Throwable getError() {
        return mError;
    }
    public T getData() {
        return mData;
    }
}