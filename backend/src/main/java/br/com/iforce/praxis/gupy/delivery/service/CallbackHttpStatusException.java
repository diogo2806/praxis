package br.com.iforce.praxis.gupy.delivery.service;

/**
 * Indica que o callback respondeu, mas não confirmou o handoff com um status HTTP 2xx.
 */
public final class CallbackHttpStatusException extends RuntimeException {

    private final int statusCode;

    public CallbackHttpStatusException(int statusCode) {
        super("Callback Gupy respondeu HTTP " + statusCode + "; era esperado um status 2xx.");
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
