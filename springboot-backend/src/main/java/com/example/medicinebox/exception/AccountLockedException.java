package com.example.medicinebox.exception;

/**
 * 账号被锁定异常
 *
 * <p>当用户账号因多次登录失败、安全策略等原因被锁定时抛出此异常。
 * 由GlobalExceptionHandler统一处理，返回423 Locked状态码。
 *
 * <p>典型使用场景：
 * <ul>
 *   <li>用户连续多次输入错误密码，账号被临时锁定</li>
 *   <li>管理员手动锁定用户账号</li>
 *   <li>账号存在安全风险被系统自动锁定</li>
 * </ul>
 *
 * @author medicine-box
 * @since 1.0.0
 */
public class AccountLockedException extends RuntimeException {

    /**
     * 构造账号锁定异常
     *
     * @param message 异常信息，描述锁定原因
     */
    public AccountLockedException(String message) {
        super(message);
    }
}
