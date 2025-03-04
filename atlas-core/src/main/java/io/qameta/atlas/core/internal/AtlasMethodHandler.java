package io.qameta.atlas.core.internal;

import io.qameta.atlas.core.api.MethodInvoker;
import io.qameta.atlas.core.api.Retry;
import io.qameta.atlas.core.api.Timeout;
import io.qameta.atlas.core.context.RetryerContext;
import io.qameta.atlas.core.util.MethodInfo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * Atlas method handler.
 */
public class AtlasMethodHandler implements InvocationHandler {

    private final ListenerNotifier notifier;

    private final Configuration configuration;

    private final Map<Method, MethodInvoker> handlers;

    public AtlasMethodHandler(final Configuration configuration,
                              final ListenerNotifier listenerNotifier,
                              final Map<Method, MethodInvoker> handlers) {
        this.configuration = configuration;
        this.notifier = listenerNotifier;
        this.handlers = handlers;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final MethodInfo methodInfo = new MethodInfo(method, args);

        notifier.beforeMethodCall(methodInfo, configuration);
        try {
            final MethodInvoker handler = handlers.get(method);
            final Object result = invokeWithRetry(handler, proxy, methodInfo);
            notifier.onMethodReturn(methodInfo, configuration, result);
            return result;
        } catch (Throwable e) {
            notifier.onMethodFailure(methodInfo, configuration, e);
            throw e;
        } finally {
            notifier.afterMethodCall(methodInfo, configuration);
        }
    }

    private Object invokeWithRetry(final MethodInvoker invoker,
                                   final Object proxy,
                                   final MethodInfo methodInfo) throws Throwable {
        final Retryer retryer = Optional.ofNullable(methodInfo.getMethod().getAnnotation(Retry.class))
                .map(DefaultRetryer::new)
                .map(Retryer.class::cast)
                .orElseGet(() -> configuration.getContext(RetryerContext.class)
                        .orElseGet(() -> new RetryerContext(new EmptyRetryer())).getValue());

        final Optional<Integer> customTimeout = methodInfo.getParameter(Integer.class, Timeout.class);
        final boolean isCustomTimeout = customTimeout.isPresent();

        Throwable lastException;
        final long start = System.currentTimeMillis();
        do {
            try {
                return invoker.invoke(proxy, methodInfo, configuration);
            } catch (Throwable e) {
                lastException = e;
            }
        } while (isCustomTimeout
                ? retryer.shouldRetry(start, customTimeout.get(), lastException)
                : retryer.shouldRetry(start, lastException));
        throw lastException;
    }
}
