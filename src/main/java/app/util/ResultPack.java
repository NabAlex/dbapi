package app.util;


public class ResultPack<T> {
    private int errorCode;
    private T data;
    public ResultPack(T data, int errorCode) {
        this.data = data;
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public T getData() {
        return data;
    }
}
