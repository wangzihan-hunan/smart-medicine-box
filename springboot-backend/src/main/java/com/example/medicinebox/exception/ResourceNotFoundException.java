package com.example.medicinebox.exception;

/**
 * 资源不存在异常
 *
 * <p>当请求的资源（如用户、药品、提醒等）在数据库中不存在时抛出此异常。
 * 由GlobalExceptionHandler统一处理，返回404 Not Found状态码。
 *
 * <p>典型使用场景：
 * <ul>
 *   <li>根据ID查询用户，用户不存在</li>
 *   <li>根据ID查询药品，药品不存在</li>
 *   <li>更新或删除不存在的资源</li>
 * </ul>
 *
 * @author medicine-box
 * @since 1.0.0
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * 构造资源不存在异常
     *
     * @param message 异常信息，描述哪个资源不存在
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
