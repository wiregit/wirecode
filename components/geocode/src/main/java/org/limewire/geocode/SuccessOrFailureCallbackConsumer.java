package org.limewire.geocode;

/**
 * Consumes a {@link SuccessOrFailureCallback} and calls either
 * {@link SuccessOrFailureCallback#setInvalid(Throwable)} or
 * {@link SuccessOrFailureCallback#process(T)} depending on whether the
 * request from the given callback was a success or not.
 */
public interface SuccessOrFailureCallbackConsumer<T> {

    /**
     * Consumes the given callback and calls either
     * {@link StringOrFailureCallback#setInvalid(Throwable)} or
     * {@link StringOrFailureCallback#process(InputStream)} depending on whether this
     * request was a success or not.
     * 
     * @param callback callback to to which we give the results.
     */
    void consume(SuccessOrFailureCallback<T> callback);
}
