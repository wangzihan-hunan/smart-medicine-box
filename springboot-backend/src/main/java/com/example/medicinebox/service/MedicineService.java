package com.example.medicinebox.service;

import com.example.medicinebox.constant.BusinessConstants;
import com.example.medicinebox.dto.request.MedicineCreateRequest;
import com.example.medicinebox.dto.response.MedicineListResponse;
import com.example.medicinebox.dto.response.MedicineResponse;
import com.example.medicinebox.entity.Medicine;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.MedicineRepository;
import com.example.medicinebox.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.opencsv.CSVWriter;

/**
 * 药品管理服务类
 * 核心功能：药品的增删改查、低库存/过期药品查询、药品分类管理、CSV导出等
 * 查询使用 JPA Specification 动态构建，支持任意条件组合
 */
@Service
public class MedicineService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(MedicineService.class);

    private final MedicineRepository medicineRepository;

    /**
     * 构造函数，注入药品数据访问层
     *
     * @param medicineRepository 药品数据访问层
     */
    public MedicineService(UserRepository userRepository, MedicineRepository medicineRepository) {
        super(userRepository);
        this.medicineRepository = medicineRepository;
    }

    /**
     * 添加药品
     * 创建新的药品记录并关联到当前用户
     *
     * @param request 药品创建请求（名称、分类、规格、厂家、库存、过期日期等）
     * @return 药品响应对象
     */
    @Transactional
    public MedicineResponse addMedicine(MedicineCreateRequest request) {
        User user = getCurrentUser();
        log.debug("添加药品, 用户ID: {}, 药品名称: {}", user.getId(), request.getName());

        Medicine medicine = new Medicine();
        medicine.setName(request.getName());
        medicine.setCategory(request.getCategory());
        medicine.setSpec(request.getSpec());
        medicine.setManufacturer(request.getManufacturer());
        medicine.setStock(request.getStock());
        medicine.setExpireDate(request.getExpireDate());
        medicine.setLowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : BusinessConstants.DEFAULT_LOW_STOCK_THRESHOLD);
        medicine.setUserId(user.getId());

        medicine = medicineRepository.save(medicine);

        log.debug("添加药品成功, 药品ID: {}", medicine.getId());
        return MedicineResponse.fromEntity(medicine);
    }

    /**
     * 分页查询药品列表
     * 使用 JPA Specification 动态构建查询条件，支持任意条件组合（关键词、分类、低库存、过期可同时使用）
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param keyword 关键词（药品名称模糊搜索）
     * @param category 药品分类
     * @param lowStock 低库存阈值（库存小于等于该值）
     * @param expired 是否只查询过期药品
     * @return 分页药品列表响应
     */
    public MedicineListResponse getMedicines(int page, int size, String keyword, String category, 
                                              Integer lowStock, Boolean expired) {
        User user = getCurrentUser();
        log.debug("查询药品列表, 用户ID: {}, 关键词: {}, 分类: {}, 低库存: {}, 过期: {}", 
                user.getId(), keyword, category, lowStock, expired);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Medicine> spec = buildQuerySpecification(user.getId(), keyword, category, lowStock, expired);
        Page<Medicine> medicinePage = medicineRepository.findAll(spec, pageable);
        
        List<MedicineResponse> responseList = medicinePage.getContent().stream()
                .map(MedicineResponse::fromEntity)
                .collect(Collectors.toList());

        log.debug("查询药品列表成功, 用户ID: {}, 总数: {}", user.getId(), medicinePage.getTotalElements());
        return new MedicineListResponse(
                (int) medicinePage.getTotalElements(),
                medicinePage.getTotalPages(),
                medicinePage.getNumber(),
                responseList
        );
    }

    /**
     * 动态构建查询条件
     * 所有非空条件以 AND 方式组合，支持同时筛选过期+低库存+关键词+分类
     */
    private Specification<Medicine> buildQuerySpecification(Integer userId, String keyword, 
                                                             String category, Integer lowStock, Boolean expired) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 基础条件：限定当前用户
            predicates.add(cb.equal(root.get("userId"), userId));

            // 关键词模糊搜索
            if (keyword != null && !keyword.isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + keyword + "%"));
            }

            // 分类筛选
            if (category != null && !category.isEmpty()) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            // 低库存筛选
            if (lowStock != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("stock"), lowStock));
            }

            // 过期药品筛选
            if (expired != null && expired) {
                predicates.add(cb.lessThan(root.get("expireDate"), LocalDateTime.now()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 根据ID获取药品详情
     * 校验药品归属权，只能查看自己的药品
     *
     * @param id 药品ID
     * @return 药品响应对象
     * @throws ResourceNotFoundException 药品不存在或无权限时抛出
     */
    public MedicineResponse getMedicineById(Integer id) {
        User user = getCurrentUser();
        log.debug("查询药品详情, 用户ID: {}, 药品ID: {}", user.getId(), id);
        
        Medicine medicine = medicineRepository.findByIdAndUserId(id, user.getId());
        if (medicine == null) {
            log.warn("药品不存在, 用户ID: {}, 药品ID: {}", user.getId(), id);
            throw new ResourceNotFoundException("药品不存在");
        }
        return MedicineResponse.fromEntity(medicine);
    }

    /**
     * 更新药品信息
     * 仅更新非空字段，校验药品归属权
     *
     * @param id 药品ID
     * @param request 药品更新请求
     * @return 更新后的药品响应对象
     * @throws ResourceNotFoundException 药品不存在或无权限时抛出
     */
    @Transactional
    public MedicineResponse updateMedicine(Integer id, MedicineCreateRequest request) {
        User user = getCurrentUser();
        log.debug("更新药品, 用户ID: {}, 药品ID: {}", user.getId(), id);
        
        Medicine medicine = medicineRepository.findByIdAndUserId(id, user.getId());
        if (medicine == null) {
            log.warn("药品不存在, 用户ID: {}, 药品ID: {}", user.getId(), id);
            throw new ResourceNotFoundException("药品不存在");
        }
        
        // 逐个字段非空判断，仅更新有值的字段
        if (request.getName() != null) {
            medicine.setName(request.getName());
        }
        if (request.getCategory() != null) {
            medicine.setCategory(request.getCategory());
        }
        if (request.getSpec() != null) {
            medicine.setSpec(request.getSpec());
        }
        if (request.getManufacturer() != null) {
            medicine.setManufacturer(request.getManufacturer());
        }
        if (request.getStock() != null) {
            medicine.setStock(request.getStock());
        }
        if (request.getExpireDate() != null) {
            medicine.setExpireDate(request.getExpireDate());
        }
        if (request.getLowStockThreshold() != null) {
            medicine.setLowStockThreshold(request.getLowStockThreshold());
        }
        
        medicine = medicineRepository.save(medicine);
        log.debug("更新药品成功, 药品ID: {}", medicine.getId());
        return MedicineResponse.fromEntity(medicine);
    }

    /**
     * 删除药品
     * 校验药品归属权，只能删除自己的药品
     *
     * @param id 药品ID
     * @throws ResourceNotFoundException 药品不存在或无权限时抛出
     */
    @Transactional
    public void deleteMedicine(Integer id) {
        User user = getCurrentUser();
        log.debug("删除药品, 用户ID: {}, 药品ID: {}", user.getId(), id);
        
        Medicine medicine = medicineRepository.findByIdAndUserId(id, user.getId());
        if (medicine == null) {
            log.warn("药品不存在, 用户ID: {}, 药品ID: {}", user.getId(), id);
            throw new ResourceNotFoundException("药品不存在");
        }
        medicineRepository.delete(medicine);
        log.debug("删除药品成功, 药品ID: {}", id);
    }

    /**
     * 获取低库存药品列表
     * 数据库层面比较 stock <= low_stock_threshold，避免全量加载后内存过滤
     *
     * @return 低库存药品响应列表
     */
    public List<MedicineResponse> getLowStockMedicines() {
        User user = getCurrentUser();
        log.debug("查询低库存药品, 用户ID: {}", user.getId());
        
        List<Medicine> lowStockMedicines = medicineRepository.findLowStockByUserId(user.getId());
        
        log.debug("查询低库存药品成功, 用户ID: {}, 数量: {}", user.getId(), lowStockMedicines.size());
        return lowStockMedicines.stream()
                .map(MedicineResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 获取过期药品列表
     * 过期日期早于当前时间的药品
     *
     * @return 过期药品响应列表
     */
    public List<MedicineResponse> getExpiredMedicines() {
        User user = getCurrentUser();
        log.debug("查询过期药品, 用户ID: {}", user.getId());
        
        List<Medicine> expiredMedicines = medicineRepository.findByUserIdAndExpireDateBefore(user.getId(), LocalDateTime.now());
        
        log.debug("查询过期药品成功, 用户ID: {}, 数量: {}", user.getId(), expiredMedicines.size());
        return expiredMedicines.stream()
                .map(MedicineResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 获取当前登录用户ID
     * 供 Controller 层获取当前用户标识使用
     *
     * @return 当前用户ID
     */
    public Integer getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * 获取用户所有药品分类（数据库层面去重）
     *
     * @return 分类名称列表
     */
    public List<String> getCategories() {
        User user = getCurrentUser();
        log.debug("查询药品分类, 用户ID: {}", user.getId());
        
        return medicineRepository.findDistinctCategoriesByUserId(user.getId());
    }

    /**
     * 获取所有药品用于导出
     * 返回原始实体列表
     *
     * @return 药品实体列表
     */
    public List<Medicine> getAllMedicinesForExport() {
        User user = getCurrentUser();
        log.debug("导出药品数据, 用户ID: {}", user.getId());
        
        return medicineRepository.findByUserId(user.getId());
    }

    /**
     * 导出药品数据为CSV格式
     * 包含药品基本信息、库存、过期日期等字段
     *
     * @return CSV格式字符串
     * @throws RuntimeException 导出失败时抛出
     */
    public String exportToCsv() {
        User user = getCurrentUser();
        log.debug("导出药品CSV, 用户ID: {}", user.getId());
        
        List<Medicine> medicines = medicineRepository.findByUserId(user.getId());
        
        try (StringWriter writer = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(writer)) {
            
            String[] header = {"ID", "名称", "分类", "规格", "生产厂家", "库存", 
                              "低库存阈值", "过期日期", "创建时间", "更新时间"};
            csvWriter.writeNext(header);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            for (Medicine medicine : medicines) {
                String[] row = {
                        String.valueOf(medicine.getId()),
                        medicine.getName(),
                        medicine.getCategory() != null ? medicine.getCategory() : "",
                        medicine.getSpec() != null ? medicine.getSpec() : "",
                        medicine.getManufacturer() != null ? medicine.getManufacturer() : "",
                        String.valueOf(medicine.getStock()),
                        medicine.getLowStockThreshold() != null ? String.valueOf(medicine.getLowStockThreshold()) : "",
                        medicine.getExpireDate() != null ? medicine.getExpireDate().format(formatter) : "",
                        medicine.getCreatedAt() != null ? medicine.getCreatedAt().format(formatter) : "",
                        medicine.getUpdatedAt() != null ? medicine.getUpdatedAt().format(formatter) : ""
                };
                csvWriter.writeNext(row);
            }
            
            log.info("药品CSV导出成功, 用户ID: {}, 数量: {}", user.getId(), medicines.size());
            return writer.toString();
            
        } catch (IOException e) {
            log.error("导出药品CSV失败: {}", e.getMessage());
            throw new RuntimeException("导出失败", e);
        }
    }
}
