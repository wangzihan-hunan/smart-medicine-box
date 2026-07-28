package com.example.medicinebox.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 健康检查配置类
 *
 * <p>实现Spring Boot Actuator的HealthIndicator接口，
 * 提供自定义的健康检查逻辑，用于监控系统关键组件的运行状态。
 *
 * <p>检查项目：
 * <ul>
 *   <li>数据库连接状态（PostgreSQL）</li>
 *   <li>文件上传目录状态（是否存在、是否可写）</li>
 * </ul>
 *
 * <p>通过 /actuator/health 端点可以查看健康检查结果。
 *
 * @author medicine-box
 * @since 1.0.0
 */
@Component
public class HealthCheckConfig implements HealthIndicator {

    /** 数据源，用于检查数据库连接 */
    private final DataSource dataSource;

    /**
     * 文件上传目录路径
     * 从配置文件file.upload-dir读取，默认为./static/uploads/
     */
    @Value("${file.upload-dir:./static/uploads/}")
    private String uploadDir;

    /**
     * 构造函数，注入数据源
     *
     * @param dataSource 数据源
     */
    public HealthCheckConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 执行健康检查
     *
     * <p>依次检查各个关键组件的健康状态，返回整体健康状态。
     * 如果所有组件都正常，返回UP状态；任何一个组件异常，返回DOWN状态。
     *
     * <p>检查项：
     * <ol>
     *   <li>数据库连接检查</li>
     *   <li>上传目录检查</li>
     * </ol>
     *
     * @return Health健康状态对象，包含各组件的详细信息
     */
    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        // 检查数据库连接状态
        checkDatabaseConnection(builder);
        // 检查文件上传目录状态
        checkUploadDirectory(builder);

        return builder.build();
    }

    /**
     * 检查数据库连接状态
     *
     * <p>从数据源获取一个连接，验证连接是否有效。
     * 如果连接有效，添加数据库相关信息到健康检查详情中；
     * 如果连接失败，将整体健康状态设置为DOWN。
     *
     * <p>检查内容：
     * <ul>
     *   <li>连接是否有效（1秒超时）</li>
     *   <li>数据库产品名称</li>
     *   <li>数据库版本号</li>
     * </ul>
     *
     * @param builder Health构建器，用于构建健康检查结果
     */
    private void checkDatabaseConnection(Health.Builder builder) {
        try (Connection connection = dataSource.getConnection()) {
            // 验证连接是否有效，超时时间1秒
            if (connection.isValid(1)) {
                builder.withDetail("database", "PostgreSQL connection OK");
                builder.withDetail("database-product", connection.getMetaData().getDatabaseProductName());
                builder.withDetail("database-version", connection.getMetaData().getDatabaseProductVersion());
            } else {
                builder.withDetail("database", "PostgreSQL connection invalid");
            }
        } catch (SQLException e) {
            // 数据库连接失败，设置健康状态为DOWN
            builder.down();
            builder.withDetail("database", "PostgreSQL connection failed");
            builder.withDetail("error", e.getMessage());
        }
    }

    /**
     * 检查文件上传目录状态
     *
     * <p>检查上传目录是否存在、是否为目录、是否可写。
     * 如果目录不存在，尝试自动创建；创建失败则设置为DOWN状态。
     *
     * <p>检查内容：
     * <ul>
     *   <li>路径是否存在</li>
     *   <li>是否为目录（而非文件）</li>
     *   <li>是否具有写入权限</li>
     *   <li>目录绝对路径</li>
     * </ul>
     *
     * @param builder Health构建器，用于构建健康检查结果
     */
    private void checkUploadDirectory(Health.Builder builder) {
        File uploadDirectory = new File(uploadDir);

        if (uploadDirectory.exists()) {
            // 目录已存在
            if (uploadDirectory.isDirectory()) {
                // 是目录，检查是否可写
                if (uploadDirectory.canWrite()) {
                    builder.withDetail("upload-directory", "Directory exists and writable");
                    builder.withDetail("upload-path", uploadDirectory.getAbsolutePath());
                } else {
                    // 目录不可写，设置为DOWN
                    builder.down();
                    builder.withDetail("upload-directory", "Directory exists but not writable");
                }
            } else {
                // 路径存在但不是目录，设置为DOWN
                builder.down();
                builder.withDetail("upload-directory", "Path exists but is not a directory");
            }
        } else {
            // 目录不存在，尝试创建
            boolean created = uploadDirectory.mkdirs();
            if (created) {
                builder.withDetail("upload-directory", "Directory created successfully");
                builder.withDetail("upload-path", uploadDirectory.getAbsolutePath());
            } else {
                // 创建失败，设置为DOWN
                builder.down();
                builder.withDetail("upload-directory", "Failed to create directory");
            }
        }
    }
}
