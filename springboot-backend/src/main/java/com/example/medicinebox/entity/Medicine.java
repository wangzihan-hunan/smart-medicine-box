package com.example.medicinebox.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 药品实体类
 * 对应数据库表：medicines
 * 业务含义：存储用户的药品库存信息，包括药品基本信息、库存数量、有效期等
 */
@Entity
@Table(name = "medicines", indexes = {
        @Index(name = "idx_medicines_name", columnList = "name"),
        @Index(name = "idx_medicines_user_id", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Medicine {

    // 药品唯一标识，主键自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 药品名称
    @Column(length = 100)
    private String name;

    // 所属用户ID
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    // 药品分类：如感冒药、退烧药、消炎药等
    @Column(length = 50)
    private String category;

    // 药品规格：如 0.5g*24片、10ml*10支等
    @Column(length = 100)
    private String spec;

    // 生产厂家
    @Column(length = 100)
    private String manufacturer;

    // 库存数量
    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    // 药品有效期/过期时间
    @Column(name = "expire_date")
    private LocalDateTime expireDate;

    // 低库存预警阈值，当库存低于此值时触发预警
    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 5;

    // 记录创建时间
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 记录最后更新时间
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 乐观锁版本号，用于并发控制
    @Version
    private Long version;

    // 实体持久化前自动设置创建时间、更新时间及默认值
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (stock == null) stock = 0;
    }

    // 实体更新前自动设置更新时间
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
