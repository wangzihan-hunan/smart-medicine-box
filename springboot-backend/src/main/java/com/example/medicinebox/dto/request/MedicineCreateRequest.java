package com.example.medicinebox.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 药品创建请求DTO
 * 用于接收创建药品时提交的药品信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineCreateRequest {

    // 药品名称
    @NotBlank(message = "药品名称不能为空")
    private String name;

    // 药品分类
    private String category;

    // 药品规格
    private String spec;

    // 生产厂家
    private String manufacturer;

    // 库存数量
    @NotNull(message = "库存数量不能为空")
    private Integer stock;

    // 过期日期
    private LocalDateTime expireDate;

    // 低库存预警阈值（默认值：5）
    private Integer lowStockThreshold = 5;
}
