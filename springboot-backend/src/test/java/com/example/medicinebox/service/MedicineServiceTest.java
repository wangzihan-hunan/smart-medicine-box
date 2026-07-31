package com.example.medicinebox.service;

import com.example.medicinebox.dto.request.MedicineCreateRequest;
import com.example.medicinebox.dto.response.MedicineListResponse;
import com.example.medicinebox.dto.response.MedicineResponse;
import com.example.medicinebox.entity.Medicine;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.MedicineRepository;
import com.example.medicinebox.repository.UserRepository;
import com.example.medicinebox.util.TestSecurityContextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * MedicineService 单元测试
 * 覆盖药品增删改查、低库存查询、分类查询、多条件组合查询等核心功能
 * Mock 数量：2（MedicineRepository, UserRepository）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MedicineService 单元测试")
class MedicineServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private UserRepository userRepository;

    private MedicineService medicineService;

    @Captor
    private ArgumentCaptor<Medicine> medicineCaptor;

    private User testUser;
    private Medicine testMedicine;
    private MedicineCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        medicineService = new MedicineService(userRepository, medicineRepository);

        testUser = new User();
        testUser.setId(1);
        testUser.setPhone("13800138000");
        testUser.setPassword("encodedPassword");
        testUser.setName("张三");

        testMedicine = new Medicine();
        testMedicine.setId(1);
        testMedicine.setName("感冒药");
        testMedicine.setCategory("感冒药");
        testMedicine.setSpec("10片/盒");
        testMedicine.setManufacturer("XX制药");
        testMedicine.setStock(100);
        testMedicine.setUserId(1);

        createRequest = new MedicineCreateRequest("感冒药", "感冒药", "10片/盒", "XX制药", 100, null, 5);

        // 使用 TestSecurityContextHelper 统一设置 SecurityContext
        TestSecurityContextHelper.setCurrentUser("13800138000", "USER");
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void tearDown() {
        TestSecurityContextHelper.clearContext();
    }

    // ==================== 添加药品 ====================

    @Test
    @DisplayName("添加药品 - 成功添加并返回药品信息")
    void addMedicine_ValidRequest_ReturnsMedicineWithCorrectFields() {
        when(medicineRepository.save(any(Medicine.class))).thenReturn(testMedicine);

        MedicineResponse response = medicineService.addMedicine(createRequest);

        assertNotNull(response, "响应不应为空");
        assertEquals("感冒药", response.getName(), "药品名称应匹配");
        assertEquals(1, response.getUserId(), "用户ID应匹配");
        verify(medicineRepository).save(medicineCaptor.capture());

        Medicine savedMedicine = medicineCaptor.getValue();
        assertEquals("感冒药", savedMedicine.getName(), "保存的药品名称应匹配");
        assertEquals(1, savedMedicine.getUserId(), "保存的用户ID应匹配");
        assertEquals(100, savedMedicine.getStock(), "保存的库存应匹配");
    }

    // ==================== 查询药品列表（使用 Specification） ====================

    @Test
    @DisplayName("获取药品列表 - 无条件查询返回全部")
    void getMedicines_NoFilter_ReturnsAll() {
        Medicine medicine2 = new Medicine();
        medicine2.setId(2);
        medicine2.setName("退烧药");
        medicine2.setUserId(1);

        Page<Medicine> medicinePage = new PageImpl<>(Arrays.asList(testMedicine, medicine2));
        when(medicineRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(medicinePage);

        MedicineListResponse response = medicineService.getMedicines(0, 10, null, null, null, null);

        assertNotNull(response, "响应不应为空");
        assertEquals(2, response.getTotalElements(), "总数应为2");
        assertEquals(2, response.getList().size(), "列表大小应为2");
        assertEquals("感冒药", response.getList().get(0).getName(), "第一个药品名称应匹配");
        assertEquals("退烧药", response.getList().get(1).getName(), "第二个药品名称应匹配");
    }

    @Test
    @DisplayName("获取药品列表 - 按关键词筛选")
    void getMedicines_WithKeyword_FiltersByName() {
        Page<Medicine> medicinePage = new PageImpl<>(Collections.singletonList(testMedicine));
        when(medicineRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(medicinePage);

        MedicineListResponse response = medicineService.getMedicines(0, 10, "感冒", null, null, null);

        assertNotNull(response, "响应不应为空");
        assertEquals(1, response.getTotalElements(), "过滤后应只有1条");
        assertEquals("感冒药", response.getList().get(0).getName(), "药品名称应匹配");
    }

    @Test
    @DisplayName("获取药品列表 - 按分类筛选")
    void getMedicines_WithCategory_FiltersByCategory() {
        Page<Medicine> medicinePage = new PageImpl<>(Collections.singletonList(testMedicine));
        when(medicineRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(medicinePage);

        MedicineListResponse response = medicineService.getMedicines(0, 10, null, "感冒药", null, null);

        assertNotNull(response, "响应不应为空");
        assertEquals(1, response.getTotalElements());
    }

    @Test
    @DisplayName("获取药品列表 - 按低库存筛选")
    void getMedicines_WithLowStock_FiltersByStock() {
        testMedicine.setStock(3);
        Page<Medicine> medicinePage = new PageImpl<>(Collections.singletonList(testMedicine));
        when(medicineRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(medicinePage);

        MedicineListResponse response = medicineService.getMedicines(0, 10, null, null, 5, null);

        assertNotNull(response, "响应不应为空");
        assertEquals(1, response.getTotalElements());
    }

    @Test
    @DisplayName("获取药品列表 - 按过期筛选")
    void getMedicines_WithExpired_FiltersByExpired() {
        testMedicine.setExpireDate(LocalDateTime.now().minusDays(1));
        Page<Medicine> medicinePage = new PageImpl<>(Collections.singletonList(testMedicine));
        when(medicineRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(medicinePage);

        MedicineListResponse response = medicineService.getMedicines(0, 10, null, null, null, true);

        assertNotNull(response, "响应不应为空");
        assertEquals(1, response.getTotalElements());
    }

    @Test
    @DisplayName("获取药品列表 - 多条件组合查询（关键词+分类+低库存+过期）")
    void getMedicines_MultipleFilters_CombinesAllConditions() {
        testMedicine.setStock(2);
        testMedicine.setExpireDate(LocalDateTime.now().minusDays(1));
        Page<Medicine> medicinePage = new PageImpl<>(Collections.singletonList(testMedicine));
        when(medicineRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(medicinePage);

        MedicineListResponse response = medicineService.getMedicines(0, 10, "感冒", "感冒药", 5, true);

        assertNotNull(response, "响应不应为空");
        assertEquals(1, response.getTotalElements(), "多条件组合仍应返回匹配结果");
    }

    @Test
    @DisplayName("获取药品列表 - 空结果")
    void getMedicines_NoMatch_ReturnsEmptyList() {
        Page<Medicine> emptyPage = new PageImpl<>(Collections.emptyList());
        when(medicineRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        MedicineListResponse response = medicineService.getMedicines(0, 10, "不存在的药", null, null, null);

        assertNotNull(response, "响应不应为空");
        assertEquals(0, response.getTotalElements(), "总数应为0");
        assertTrue(response.getList().isEmpty(), "列表应为空");
    }

    // ==================== 查询药品详情 ====================

    @Test
    @DisplayName("获取药品详情 - 药品存在时返回药品信息")
    void getMedicineById_Exists_ReturnsMedicine() {
        when(medicineRepository.findByIdAndUserId(anyInt(), anyInt())).thenReturn(testMedicine);

        MedicineResponse response = medicineService.getMedicineById(1);

        assertNotNull(response, "响应不应为空");
        assertEquals("感冒药", response.getName(), "药品名称应匹配");
        assertEquals(100, response.getStock(), "库存应匹配");
    }

    @Test
    @DisplayName("获取药品详情 - 药品不存在时抛出 ResourceNotFoundException")
    void getMedicineById_NotExists_ThrowsResourceNotFoundException() {
        when(medicineRepository.findByIdAndUserId(anyInt(), anyInt())).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> medicineService.getMedicineById(999),
                "不存在的药品应抛出异常");

        assertEquals("药品不存在", exception.getMessage(), "异常消息应匹配");
    }

    // ==================== 低库存查询 ====================

    @Test
    @DisplayName("低库存查询 - 有低库存药品时返回列表")
    void getLowStockMedicines_HasLowStock_ReturnsList() {
        Medicine lowStockMedicine = new Medicine();
        lowStockMedicine.setId(2);
        lowStockMedicine.setName("低库存药");
        lowStockMedicine.setStock(2);
        lowStockMedicine.setLowStockThreshold(5);
        lowStockMedicine.setUserId(1);

        when(medicineRepository.findLowStockByUserId(anyInt())).thenReturn(Collections.singletonList(lowStockMedicine));

        List<MedicineResponse> response = medicineService.getLowStockMedicines();

        assertNotNull(response, "响应不应为空");
        assertEquals(1, response.size(), "应有1个低库存药品");
        assertEquals("低库存药", response.get(0).getName(), "药品名称应匹配");
    }

    @Test
    @DisplayName("低库存查询 - 无低库存药品时返回空列表")
    void getLowStockMedicines_NoLowStock_ReturnsEmptyList() {
        when(medicineRepository.findLowStockByUserId(anyInt())).thenReturn(Collections.emptyList());

        List<MedicineResponse> response = medicineService.getLowStockMedicines();

        assertNotNull(response, "响应不应为空");
        assertTrue(response.isEmpty(), "列表应为空");
    }

    // ==================== 分类查询 ====================

    @Test
    @DisplayName("分类查询 - 有分类时返回去重列表")
    void getCategories_HasCategories_ReturnsDistinctList() {
        when(medicineRepository.findDistinctCategoriesByUserId(anyInt()))
                .thenReturn(Arrays.asList("感冒药", "退烧药", "消炎药"));

        List<String> categories = medicineService.getCategories();

        assertNotNull(categories, "响应不应为空");
        assertEquals(3, categories.size(), "应有3个分类");
        assertTrue(categories.contains("感冒药"), "应包含感冒药");
        assertTrue(categories.contains("退烧药"), "应包含退烧药");
    }

    @Test
    @DisplayName("分类查询 - 无分类时返回空列表")
    void getCategories_NoCategories_ReturnsEmptyList() {
        when(medicineRepository.findDistinctCategoriesByUserId(anyInt()))
                .thenReturn(Collections.emptyList());

        List<String> categories = medicineService.getCategories();

        assertNotNull(categories, "响应不应为空");
        assertTrue(categories.isEmpty(), "列表应为空");
    }

    // ==================== 更新药品 ====================

    @Test
    @DisplayName("更新药品 - 成功更新所有字段")
    void updateMedicine_ValidRequest_UpdatesAllFields() {
        when(medicineRepository.findByIdAndUserId(anyInt(), anyInt())).thenReturn(testMedicine);
        when(medicineRepository.save(any(Medicine.class))).thenReturn(testMedicine);

        MedicineCreateRequest updateRequest = new MedicineCreateRequest("新感冒药", "感冒用药",
                "20片/盒", "YY制药", 200, null, 5);
        MedicineResponse response = medicineService.updateMedicine(1, updateRequest);

        assertNotNull(response, "响应不应为空");
        verify(medicineRepository).save(medicineCaptor.capture());

        Medicine updatedMedicine = medicineCaptor.getValue();
        assertEquals("新感冒药", updatedMedicine.getName(), "药品名称应更新");
        assertEquals("感冒用药", updatedMedicine.getCategory(), "分类应更新");
        assertEquals("20片/盒", updatedMedicine.getSpec(), "规格应更新");
        assertEquals("YY制药", updatedMedicine.getManufacturer(), "生产商应更新");
        assertEquals(200, updatedMedicine.getStock(), "库存应更新");
    }

    @Test
    @DisplayName("更新药品 - 药品不存在时抛出 ResourceNotFoundException")
    void updateMedicine_NotExists_ThrowsResourceNotFoundException() {
        when(medicineRepository.findByIdAndUserId(anyInt(), anyInt())).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> medicineService.updateMedicine(999, createRequest),
                "不存在的药品应抛出异常");

        assertEquals("药品不存在", exception.getMessage(), "异常消息应匹配");
        verify(medicineRepository, never()).save(any(Medicine.class));
    }

    // ==================== 删除药品 ====================

    @Test
    @DisplayName("删除药品 - 药品存在时成功删除")
    void deleteMedicine_Exists_DeletesSuccessfully() {
        when(medicineRepository.findByIdAndUserId(anyInt(), anyInt())).thenReturn(testMedicine);
        doNothing().when(medicineRepository).delete(any(Medicine.class));

        medicineService.deleteMedicine(1);

        verify(medicineRepository).delete(testMedicine);
    }

    @Test
    @DisplayName("删除药品 - 药品不存在时抛出 ResourceNotFoundException")
    void deleteMedicine_NotExists_ThrowsResourceNotFoundException() {
        when(medicineRepository.findByIdAndUserId(anyInt(), anyInt())).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> medicineService.deleteMedicine(999),
                "不存在的药品应抛出异常");

        assertEquals("药品不存在", exception.getMessage(), "异常消息应匹配");
        verify(medicineRepository, never()).delete(any(Medicine.class));
    }
}