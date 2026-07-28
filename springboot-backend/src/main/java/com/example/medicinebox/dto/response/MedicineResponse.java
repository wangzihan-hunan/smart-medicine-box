package com.example.medicinebox.dto.response;

import com.example.medicinebox.entity.Medicine;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 药品详情响应DTO
 * 用于返回单个药品的详细信息，包含库存预警和过期状态
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineResponse {
    // 药品ID
    private Integer id;
    // 所属用户ID
    private Integer userId;
    // 药品名称
    private String name;
    // 药品分类
    private String category;
    // 药品规格
    private String spec;
    // 生产厂家
    private String manufacturer;
    // 库存数量
    private Integer stock;
    // 低库存预警阈值
    private Integer lowStockThreshold;
    // 过期日期
    private LocalDateTime expireDate;
    // 是否低库存
    private Boolean isLowStock;
    // 是否已过期
    private Boolean isExpired;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间
    private LocalDateTime updatedAt;

    /**
     * 从实体对象转换为响应DTO
     * 自动计算低库存和过期状态
     * @param medicine 药品实体
     * @return 药品响应DTO
     */
    public static MedicineResponse fromEntity(Medicine medicine) {
        MedicineResponse response = new MedicineResponse();
        response.setId(medicine.getId());
        response.setUserId(medicine.getUserId());
        response.setName(medicine.getName());
        response.setCategory(medicine.getCategory());
        response.setSpec(medicine.getSpec());
        response.setManufacturer(medicine.getManufacturer());
        response.setStock(medicine.getStock());
        response.setLowStockThreshold(medicine.getLowStockThreshold());
        response.setExpireDate(medicine.getExpireDate());
        
        int threshold = medicine.getLowStockThreshold() != null ? medicine.getLowStockThreshold() : 5;
        response.setIsLowStock(medicine.getStock() != null && medicine.getStock() <= threshold);
        response.setIsExpired(medicine.getExpireDate() != null && medicine.getExpireDate().isBefore(LocalDateTime.now()));
        
        response.setCreatedAt(medicine.getCreatedAt());
        response.setUpdatedAt(medicine.getUpdatedAt());
        return response;
    }
}
