package com.example.medicinebox.config;

import com.example.medicinebox.dto.response.BaseResponse;
import com.example.medicinebox.exception.AccountLockedException;
import com.example.medicinebox.exception.BadRequestException;
import com.example.medicinebox.exception.ResourceNotFoundException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 *
 * <p>使用@RestControllerAdvice注解，统一处理Controller层抛出的各类异常，
 * 将异常转换为统一格式的JSON响应返回给前端。
 *
 * <p>主要功能：
 * <ul>
 *   <li>参数校验异常处理（MethodArgumentNotValidException）</li>
 *   <li>认证相关异常处理（BadCredentialsException、AuthenticationException等）</li>
 *   <li>JWT相关异常处理（MalformedJwtException、ExpiredJwtException、SignatureException）</li>
 *   <li>权限不足异常处理（AccessDeniedException）</li>
 *   <li>业务异常处理（ResourceNotFoundException、BadRequestException、AccountLockedException）</li>
 *   <li>乐观锁冲突异常处理（ObjectOptimisticLockingFailureException）</li>
 *   <li>文件上传大小超限异常处理（MaxUploadSizeExceededException）</li>
 *   <li>运行时异常和通用异常兜底处理</li>
 * </ul>
 *
 * @author medicine-box
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 服务器内部错误的统一提示信息 */
    private static final String INTERNAL_ERROR_MESSAGE = "服务器内部错误，请稍后重试";

    /**
     * 处理参数校验异常
     *
     * <p>当使用@Valid注解校验请求参数失败时触发，
     * 返回所有字段的校验错误信息。
     *
     * @param ex 参数校验异常
     * @return 400 Bad Request响应，包含字段错误详情
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("参数校验失败: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest()
                .body(new BaseResponse<>(400, "error", "参数校验失败", errors));
    }

    /**
     * 处理文件上传大小超限异常
     *
     * <p>当上传的文件大小超过配置的最大限制时触发。
     *
     * @param ex 文件大小超限异常
     * @return 413 Payload Too Large响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<BaseResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("文件大小超过限制: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new BaseResponse<>(413, "error", "文件大小超过限制", null));
    }

    /**
     * 处理凭证错误异常（用户名或密码错误）
     *
     * <p>当用户输入的手机号或密码不正确时触发。
     * 为安全起见，不明确提示是手机号还是密码错误。
     *
     * @param ex 凭证错误异常
     * @return 401 Unauthorized响应
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<BaseResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("认证失败: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new BaseResponse<>(401, "error", "手机号或密码错误！", null));
    }

    /**
     * 处理认证异常
     *
     * <p>通用的认证异常处理，作为认证相关异常的兜底。
     *
     * @param ex 认证异常
     * @return 401 Unauthorized响应
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<BaseResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("认证异常: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new BaseResponse<>(401, "error", "认证失败，请重新登录", null));
    }

    /**
     * 处理权限不足异常
     *
     * <p>当用户已认证但没有访问资源的权限时触发。
     *
     * @param ex 权限不足异常
     * @return 403 Forbidden响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("权限不足: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new BaseResponse<>(403, "error", "权限不足，无法访问", null));
    }

    /**
     * 处理JWT格式错误异常
     *
     * <p>当JWT token格式不正确或损坏时触发。
     *
     * @param ex JWT格式异常
     * @return 401 Unauthorized响应
     */
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<BaseResponse<Void>> handleMalformedJwtException(MalformedJwtException ex) {
        log.warn("无效的token: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new BaseResponse<>(401, "error", "无效的token，请重新登录", null));
    }

    /**
     * 处理JWT过期异常
     *
     * <p>当JWT token已超过有效期时触发。
     *
     * @param ex JWT过期异常
     * @return 401 Unauthorized响应
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<BaseResponse<Void>> handleExpiredJwtException(ExpiredJwtException ex) {
        log.warn("token已过期: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new BaseResponse<>(401, "error", "token已过期，请重新登录", null));
    }

    /**
     * 处理JWT签名错误异常
     *
     * <p>当JWT token签名验证失败（可能被篡改）时触发。
     *
     * @param ex JWT签名异常
     * @return 401 Unauthorized响应
     */
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<BaseResponse<Void>> handleSignatureException(SignatureException ex) {
        log.warn("token签名错误: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new BaseResponse<>(401, "error", "token签名错误，请重新登录", null));
    }

    /**
     * 处理非法参数异常
     *
     * <p>当业务代码中抛出IllegalArgumentException时触发。
     *
     * @param ex 非法参数异常
     * @return 400 Bad Request响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("参数错误: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new BaseResponse<>(400, "error", ex.getMessage(), null));
    }

    /**
     * 处理资源不存在异常
     *
     * <p>当请求的资源（如用户、药品等）不存在时触发。
     *
     * @param ex 资源不存在异常
     * @return 404 Not Found响应
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("资源不存在: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new BaseResponse<>(404, "error", ex.getMessage(), null));
    }

    /**
     * 处理错误请求异常
     *
     * <p>当请求参数不符合业务规则时触发。
     *
     * @param ex 错误请求异常
     * @return 400 Bad Request响应
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<BaseResponse<Void>> handleBadRequestException(BadRequestException ex) {
        log.warn("请求错误: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new BaseResponse<>(400, "error", ex.getMessage(), null));
    }

    /**
     * 处理账号被锁定异常
     *
     * <p>当用户账号因多次登录失败等原因被锁定时触发。
     *
     * @param ex 账号锁定异常
     * @return 423 Locked响应
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccountLockedException(AccountLockedException ex) {
        log.warn("账号被锁定: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(new BaseResponse<>(423, "error", ex.getMessage(), null));
    }

    /**
     * 处理乐观锁冲突异常
     *
     * <p>当并发修改同一条数据导致乐观锁冲突时触发。
     * 提示用户刷新页面后重试。
     *
     * @param ex 乐观锁失败异常
     * @return 409 Conflict响应
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<BaseResponse<Void>> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        log.warn("乐观锁冲突: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new BaseResponse<>(409, "error", "数据已被其他用户修改，请刷新页面后重试", null));
    }

    /**
     * 处理运行时异常
     *
     * <p>捕获所有未被上述处理器处理的RuntimeException，
     * 记录错误日志并返回通用错误信息。
     *
     * @param ex 运行时异常
     * @return 500 Internal Server Error响应
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<BaseResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("运行时异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BaseResponse<>(500, "error", INTERNAL_ERROR_MESSAGE, null));
    }

    /**
     * 处理IO异常
     *
     * <p>当文件读写等IO操作失败时触发。
     *
     * @param ex IO异常
     * @return 500 Internal Server Error响应
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<BaseResponse<Void>> handleIOException(IOException ex) {
        log.error("IO异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BaseResponse<>(500, "error", "文件操作失败，请稍后重试", null));
    }

    /**
     * 处理所有未捕获的异常（兜底处理）
     *
     * <p>作为最后的异常处理防线，捕获所有Exception类型的异常，
     * 确保不会将原始异常信息暴露给前端。
     *
     * @param ex 异常
     * @return 500 Internal Server Error响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleException(Exception ex) {
        log.error("未处理的异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BaseResponse<>(500, "error", INTERNAL_ERROR_MESSAGE, null));
    }
}
