package com.example.medicinebox.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 参考药品实体类
 * 对应数据库表：reference_medicines
 * 业务含义：全局药品标准库，用于 AI 识别时作为第二层匹配基准，
 * 补全用户本地库存中不存在的药品信息
 */
@Entity
@Table(name = "reference_medicines", indexes = {
        @Index(name = "idx_ref_medicines_name", columnList = "name"),
        @Index(name = "idx_ref_medicines_category", columnList = "category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceMedicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String category;

    @Column(length = 200)
    private String spec;

    @Column(length = 200)
    private String manufacturer;

    @Column(length = 200)
    private String dosage;

    @Column(name = "is_otc")
    private Boolean isOtc = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}