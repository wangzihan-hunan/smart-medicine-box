package com.example.medicinebox.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 文件上传配置属性类
 * 将文件上传校验规则从硬编码常量抽取为可配置属性，
 * 便于后续扩展支持的文件类型和统一管理上传安全策略
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "file")
public class FileUploadProperties {

    /** 文件上传目录 */
    private String uploadDir = "./uploads";

    /** 允许上传的文件扩展名集合 */
    private Set<String> allowedExtensions = new HashSet<>();

    /** 各扩展名对应的允许 MIME 类型映射 */
    private Map<String, Set<String>> allowedMimeTypes = new HashMap<>();

    /**
     * 初始化默认文件类型配置
     * 在 Spring 属性注入后执行，确保默认值与配置文件中的值合并
     */
    public FileUploadProperties() {
        allowedExtensions.add("pdf");
        allowedExtensions.add("jpg");
        allowedExtensions.add("jpeg");
        allowedExtensions.add("png");
        allowedExtensions.add("gif");
        allowedExtensions.add("webp");

        allowedMimeTypes.put("pdf", new HashSet<String>() {{ add("application/pdf"); }});
        allowedMimeTypes.put("jpg", new HashSet<String>() {{ add("image/jpeg"); add("image/pjpeg"); }});
        allowedMimeTypes.put("jpeg", new HashSet<String>() {{ add("image/jpeg"); add("image/pjpeg"); }});
        allowedMimeTypes.put("png", new HashSet<String>() {{ add("image/png"); }});
        allowedMimeTypes.put("gif", new HashSet<String>() {{ add("image/gif"); }});
        allowedMimeTypes.put("webp", new HashSet<String>() {{ add("image/webp"); }});
    }

    /**
     * 校验文件扩展名是否在白名单内
     *
     * @param extension 文件扩展名（小写）
     * @return true-允许，false-不允许
     */
    public boolean isExtensionAllowed(String extension) {
        return allowedExtensions.contains(extension);
    }

    /**
     * 校验 MIME 类型是否与扩展名匹配
     *
     * @param extension 文件扩展名（小写）
     * @param contentType MIME 类型
     * @return true-匹配或无需校验，false-不匹配
     */
    public boolean isMimeTypeValid(String extension, String contentType) {
        Set<String> allowedMimes = allowedMimeTypes.get(extension);
        if (allowedMimes == null || contentType == null) {
            return true; // 无法校验时放行，由扩展名校验兜底
        }
        return allowedMimes.contains(contentType.toLowerCase());
    }
}