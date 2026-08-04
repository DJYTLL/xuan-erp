package com.xuan.erp.common.security.autoconfigure;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 通过反射复用 Spring Cloud DiscoveryClient 的 URI 解析支持。
 *
 * <p>公共安全模块不直接依赖 Spring Cloud API，但当业务服务自己已经引入注册发现能力时，
 * 这里可以自动发现 DiscoveryClient Bean，并优先把 IAM 服务名解析成真实实例地址。
 * 如果业务服务没有引入注册发现，再回退到固定 URI 配置。</p>
 */
final class ServiceDiscoveryUriResolverSupport {

    private static final String DISCOVERY_CLIENT_CLASS_NAME = "org.springframework.cloud.client.discovery.DiscoveryClient";

    private final ListableBeanFactory beanFactory;
    private final ClassLoader classLoader;

    ServiceDiscoveryUriResolverSupport(ListableBeanFactory beanFactory, ClassLoader classLoader) {
        this.beanFactory = Objects.requireNonNull(beanFactory, "beanFactory must not be null");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader must not be null");
    }

    Optional<Supplier<Mono<URI>>> buildUriSupplier(String serviceName, String path) {
        if (!hasText(serviceName) || !hasText(path)) {
            return Optional.empty();
        }
        Class<?> discoveryClientClass = resolveDiscoveryClientClass();
        if (discoveryClientClass == null) {
            return Optional.empty();
        }
        String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, discoveryClientClass);
        if (beanNames.length == 0) {
            return Optional.empty();
        }
        Object discoveryClient = beanFactory.getBean(beanNames[0]);
        Method getInstancesMethod = ReflectionUtils.findMethod(discoveryClientClass, "getInstances", String.class);
        if (getInstancesMethod == null) {
            throw new IllegalStateException("DiscoveryClient 缺少 getInstances(String) 方法，无法解析服务地址");
        }
        String normalizedPath = normalizePath(path);
        return Optional.of(() -> Mono.fromCallable(() -> resolveUri(discoveryClient, getInstancesMethod, serviceName, normalizedPath)));
    }

    private Class<?> resolveDiscoveryClientClass() {
        try {
            return ClassUtils.forName(DISCOVERY_CLIENT_CLASS_NAME, classLoader);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    private URI resolveUri(Object discoveryClient, Method getInstancesMethod, String serviceName, String path) {
        Object result = ReflectionUtils.invokeMethod(getInstancesMethod, discoveryClient, serviceName);
        if (!(result instanceof List<?> instances) || instances.isEmpty()) {
            throw new IllegalStateException("未找到可用的 IAM 服务实例: " + serviceName);
        }
        Object first = instances.get(0);
        Method getUriMethod = ReflectionUtils.findMethod(first.getClass(), "getUri");
        if (getUriMethod == null) {
            throw new IllegalStateException("ServiceInstance 缺少 getUri() 方法，无法解析服务地址");
        }
        Object uriValue = ReflectionUtils.invokeMethod(getUriMethod, first);
        if (!(uriValue instanceof URI baseUri)) {
            throw new IllegalStateException("ServiceInstance#getUri() 未返回有效 URI");
        }
        return UriComponentsBuilder.fromUri(baseUri)
                .path(path)
                .build(true)
                .toUri();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizePath(String path) {
        String normalized = path.trim();
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }
}
