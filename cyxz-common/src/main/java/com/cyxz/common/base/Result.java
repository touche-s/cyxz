package com.cyxz.common.base;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果
 * <p>所有接口的标准响应体，包含状态码、消息、数据和时间戳。
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 响应时间戳 */
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 全参构造
     *
     * @param code    状态码
     * @param message 提示信息
     * @param data    业务数据
     */
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
    }

    /**
     * 成功（带数据）
     *
     * @param data 业务数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), data);
    }

    /**
     * 成功（自定义消息，无数据）
     *
     * @param message 提示信息
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), message, null);
    }

    /**
     * 成功（自定义消息 + 数据）
     *
     * @param message 提示信息
     * @param data    业务数据
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败（默认 500）
     */
    public static <T> Result<T> fail() {
        return new Result<>(ErrorCode.FAIL.getCode(), ErrorCode.FAIL.getMsg(), null);
    }

    /**
     * 失败（自定义消息，code=500）
     *
     * @param message 错误信息
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(ErrorCode.FAIL.getCode(), message, null);
    }

    /**
     * 失败（自定义 code + 消息）
     *
     * @param code    错误码
     * @param message 错误信息
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败（自定义 code + 消息 + 数据）
     *
     * @param code    错误码
     * @param message 错误信息
     * @param data    附带数据
     */
    public static <T> Result<T> fail(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.code != null && this.code == ErrorCode.SUCCESS.getCode();
    }

}
