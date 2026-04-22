package com.cqie.shortlink_common.common.convention.errorcode;

public enum BaseErrorCode implements IErrorCode {

    CLIENT_ERROR("A000001", "客户端错误"),

    USER_REGISTER_ERROR("A000100", "用户注册错误"),
    USER_NAME_VERIFY_ERROR("A000110", "用户名校验失败"),
    USER_NAME_EXIST_ERROR("A000111", "用户名已存在"),
    USER_NAME_SENSITIVE_ERROR("A000112", "用户名包含敏感词"),
    USER_NAME_SPECIAL_CHARACTER_ERROR("A000113", "用户名包含特殊字符"),
    PASSWORD_VERIFY_ERROR("A000120", "密码校验失败"),
    PASSWORD_SHORT_ERROR("A000121", "密码长度不够"),
    USER_AUTHENTICATION_ERROR("A000150", "用户认证失败"),
    PHONE_VERIFY_ERROR("A000151", "手机格式校验失败"),
    USER_NOT_EXIST_ERROR("A000152", "用户不存在"),
    USER_EXIST_ERROR("A000153", "用户已存在"),
    USER_SAVE_ERROR("A000154", "用户保存失败"),
    USERNAME_NULL_ERROR("A000155", "用户名为空"),
    USER_LOGIN_ERROR("A000156", "用户名或密码错误"),

    GROUP_SAVE_ERROR("A000157", "用户组保存失败"),
    GROUP_SAVE_COUNT_ERROR("A000158", "用户组数量超出限制"),
    GROUP_NOT_EXIST_ERROR("A000159", "用户组不存在"),
    GROUP_UPDATE_ERROR("A000160", "用户组更新失败"),
    GROUP_DELETE_ERROR("A000161", "用户组删除失败"),
    GROUP_SORT_ERROR("A000162", "用户组排序失败"),

    SHORT_LINK_NOT_EXIST_ERROR("A000300", "短链接不存在"),
    SHORT_LINK_UPDATE_ERROR("A000301", "短链接更新失败"),
    SHORT_LINK_DELETE_ERROR("A000302", "短链接删除失败"),
    SHORT_LINK_QUERY_ERROR("A000303", "短链接查询失败"),
    SUMMARY_GENERATION_ERROR("A000304", "短链接摘要生成失败"),

    IDEMPOTENT_TOKEN_NULL_ERROR("A000200", "幂等 Token 为空"),
    IDEMPOTENT_TOKEN_DELETE_ERROR("A000201", "幂等 Token 已被使用或失效"),

    SERVICE_ERROR("B000001", "系统执行出错"),
    SERVICE_TIMEOUT_ERROR("B000100", "系统执行超时"),

    REMOTE_ERROR("C000001", "调用第三方服务出错");

    private final String code;
    private final String message;

    BaseErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
