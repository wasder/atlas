package io.qameta.atlas.core.internal;

import io.qameta.atlas.core.api.MethodExtension;
import io.qameta.atlas.core.util.MethodInfo;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * Default method extension.
 */
public class DefaultMethodExtension implements MethodExtension {

    @Override
    public boolean test(final Method method) {
        return method.isDefault();
    }

    @Override
    public Object invoke(final Object proxy, final MethodInfo methodInfo, final Configuration config) throws Throwable {
        return MethodHandles.lookup()
                .unreflectSpecial(methodInfo.getMethod(), methodInfo.getMethod().getDeclaringClass())
                .bindTo(proxy)
                .invokeWithArguments(methodInfo.getArgs());
    }
}
