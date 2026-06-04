package com.cyxz.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功返回（无数据）
     *
     * @param <T> 数据类型
     * @return 结果
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * 成功返回（带数据）
     *
     * @param data 返回数据
     * @param <T>  数据类型
     * @return 结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 成功返回（自定义消息，无数据）
     *
     * @param message 提示信息
     * @param <T>     数据类型
     * @return 结果
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null);
    }

    /**
     * 成功返回（自定义消息 + 数据）
     *
     * @param message 提示信息
     * @param data    返回数据
     * @param <T>     数据类型
     * @return 结果
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败返回（默认500）
     *
     * @param <T> 数据类型
     * @return 结果
     */
    public static <T> Result<T> fail() {
        return new Result<>(500, "操作失败", null);
    }

    /**
     * 失败返回（自定义消息）
     *
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 结果
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 失败返回（指定code + 消息）
     *
     * @param code    错误码
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 结果
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败返回（指定code + 消息 + 数据）
     *
     * @param code    错误码
     * @param message 错误信息
     * @param data    返回数据
     * @param <T>     数据类型
     * @return 结果
     */
    public static <T> Result<T> fail(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }

}
