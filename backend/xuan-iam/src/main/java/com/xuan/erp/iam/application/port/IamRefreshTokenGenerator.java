package com.xuan.erp.iam.application.port;

/**
 * IAM refresh token 生成端口，负责生成不可预测的不透明令牌明文。
 */
public interface IamRefreshTokenGenerator {

    String generate();
}
