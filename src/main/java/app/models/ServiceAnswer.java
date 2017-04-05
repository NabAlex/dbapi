package app.models;


public class ServiceAnswer<T> {
    private int errorCode;
    private T data;
    public ServiceAnswer(T data, int errorCode) {
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
