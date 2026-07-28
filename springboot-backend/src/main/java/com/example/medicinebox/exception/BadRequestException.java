package com.example.medicinebox.exception;

/**
 * 错误请求异常
 *
 * <p>当客户端请求参数不合法或不符合业务规则时抛出此异常。
 * 由GlobalExceptionHandler统一处理，返回400 Bad Request状态码。
 *
 * <p>与MethodArgumentNotValidException的区别：
 * <ul>
 *   <li>MethodArgumentNotValidException：用于@Valid注解的参数校验失败</li>
 *   <li>BadRequestException：用于业务逻辑层面的参数错误或规则校验失败</li>
 * </ul>
 *
 * <p>典型使用场景：
 * <ul>
 *   <li>用户已存在，不能重复注册</li>
 *   <li>参数值不符合业务规则（如日期范围错误）</li>
 *   <li>操作条件不满足（如取消已完成的订单）</li>
 * </ul>
 *
 * @author medicine-box
 * @since 1.0.0
 */
public class BadRequestException extends RuntimeException {

    /**
     * 构造错误请求异常
     *
     * @param message 异常信息，描述请求错误的具体原因
     */
    public BadRequestException(String message) {
        super(message);
    }
}
