package org.limewire.swarm.http.handler;

import java.io.IOException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.ProducingNHttpEntity;
import org.apache.http.nio.protocol.NHttpRequestExecutionHandler;
import org.apache.http.protocol.HttpContext;

/**
 * A version of {@link NHttpRequestExecutionHandler} without the
 * {@link NHttpRequestExecutionHandler#initalizeContext(HttpContext, Object)} call.
 */
public interface ExecutionHandler {

    /**
     * Triggered when the underlying connection is ready to send a new
     * HTTP request to the target host. This method may return
     * <code>null</code> if the client is not yet ready to send a
     * request. In this case the connection will remain open and
     * can be activated at a later point.
     * <p>
     * If the request has an entity, the entity <b>must</b> be an
     * instance of {@link ProducingNHttpEntity}.
     *
     * @param context the actual HTTP context
     * @return an HTTP request to be sent or <code>null</code> if no
     *   request needs to be sent
     */
    HttpRequest submitRequest(HttpContext context);

    /**
     * Triggered when a response is received with an entity. This method should
     * return a {@link ConsumingNHttpEntity} that will be used to consume the
     * entity. <code>null</code> is a valid response value, and will indicate
     * that the entity should be silently ignored.
     * <p>
     * After the entity is fully consumed,
     * {@link NHttpRequestExecutionHandler#handleResponse(HttpResponse, HttpContext)}
     * is called to notify a full response & entity are ready to be processed.
     *
     * @param response
     *            The response containing the existing entity.
     * @param context
     *            the actual HTTP context
     * @return An entity that will asynchronously consume the response's content
     *         body.
     */
    ConsumingNHttpEntity responseEntity(HttpResponse response, HttpContext context)
        throws IOException;

    /**
     * Triggered when an HTTP response is ready to be processed.
     *
     * @param response
     *            the HTTP response to be processed
     * @param context
     *            the actual HTTP context
     */
    void handleResponse(HttpResponse response, HttpContext context)
        throws IOException;

    /**
     * Triggered when the connection is terminated. This event can be used
     * to release objects stored in the context or perform some other kind
     * of cleanup.
     *
     * @param context the actual HTTP context
     */
    void finalizeContext(HttpContext context);

}
