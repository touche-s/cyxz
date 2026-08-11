package com.cyxz.common.utils;

import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;

/**
 * ID 工具类
 * <p>统一处理前端传入的 String 型 ID（避免 JS 精度丢失）到 Long 的转换，
 * 替代各 DTO 内分散且行为不一致的 {@code Long.parseLong} 实现。
 * <p>转换约定：
 * <ul>
 *   <li>null / 空白字符串：可选字段返回 null，必填字段抛 {@link ErrorCode#PARAM_MISSING}</li>
 *   <li>格式非法（非数字）：统一抛 {@link ErrorCode#PARAM_ERROR}，不再静默吞异常返回 null</li>
 * </ul>
 */
public final class IdUtil {

    private IdUtil() {
    }

    /**
     * 将 String 解析为 Long（可选字段语义）
     * <p>null / 空白返回 null；格式非法抛 PARAM_ERROR。
     *
     * @param value 字符串型 ID，可为 null
     * @return Long 值，或 null（当入参为空白时）
     */
    public static Long asLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ID格式错误: " + value);
        }
    }

    /**
     * 将 String 解析为 Long（必填字段语义）
     * <p>null / 空白抛 PARAM_MISSING；格式非法抛 PARAM_ERROR。
     *
     * @param value     字符串型 ID
     * @param fieldName 字段名，用于拼装错误提示
     * @return Long 值
     */
    public static Long asLongRequired(String value, String fieldName) {
        Long id = asLong(value);
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, fieldName + "不能为空");
        }
        return id;
    }
}
