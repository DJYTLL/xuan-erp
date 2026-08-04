package org.springframework.cloud.client;

import java.net.URI;

/**
 * 测试桩：仅提供公共安全自动配置反射所需的最小方法集合。
 */
public interface ServiceInstance {

    URI getUri();
}
