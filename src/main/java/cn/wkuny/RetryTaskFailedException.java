package cn.wkuny;

public class RetryTaskFailedException extends RuntimeException {
    private final Throwable cause;

    public RetryTaskFailedException(String stepName, int maxAttempts, Throwable cause) {
        super(String.format("任务%s尝试%d次后失败！: %s", stepName, maxAttempts, cause.getMessage()), cause);
        this.cause = cause;
    }
    public Throwable getCause() {
        return cause;
    }
}
