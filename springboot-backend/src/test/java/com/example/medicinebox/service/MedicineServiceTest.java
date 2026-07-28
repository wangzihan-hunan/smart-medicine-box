package com.example.medicinebox.service;

import com.example.medicinebox.dto.request.MedicineCreateRequest;
import com.example.medicinebox.dto.response.MedicineListResponse;
import com.example.medicinebox.dto.response.MedicineResponse;
import com.example.medicinebox.entity.Medicine;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.MedicineRepository;
import com.example.medicinebox.repository.UserRepository;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

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
        medicineService = new MedicineService(medicineRepository);
        ReflectionTestUtils.setField(medicineService, "userRepository", userRepository);

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

        org.springframework.security.core.userdetails.UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                "13800138000", "password", Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
        
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()));
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("添加药品 - 成功添加")
    void addMedicine_Success_AddsMedicine() {
        when(medicineRepository.save(any(Medicine.class))).thenReturn(testMedicine);

        MedicineResponse response = medicineService.addMedicine(createRequest);

        assertNotNull(response);
        assertEquals("感冒药", response.getName());
        assertEquals(1, response.getUserId());
        verify(medicineRepository).save(medicineCaptor.capture());
        
        Medicine savedMedicine = medicineCaptor.getValue();
        assertEquals("感冒药", savedMedicine.getName());
        assertEquals(1, savedMedicine.getUserId());
        assertEquals(100, savedMedicine.getStock());
    }

    @Test
    @DisplayName("获取药品列表 - 分页查询成功")
    void getMedicines_Success_ReturnsPaginatedList() {
        Medicine medicine2 = new Medicine();
        medicine2.setId(2);
        medicine2.setName("退烧药");
        medicine2.setUserId(1);

        Page<Medicine> medicinePage = new PageImpl<>(Arrays.asList(testMedicine, medicine2));
        when(medicineRepository.findByUserId(anyInt(), any(Pageable.class))).thenReturn(medicinePage);

        MedicineListResponse response = medicineService.getMedicines(0, 10, null, null, null, null);

        assertNotNull(response);
        assertEquals(2, response.getTotalElements());
        assertEquals(2, response.getList().size());
        assertEquals("感冒药", response.getList().get(0).getName());
        assertEquals("退烧药", response.getList().get(1).getName());
    }

    @Test
    @DisplayName("获取药品详情 - 存在返回药品")
    void getMedicineById_Exists_ReturnsMedicine() {
        when(medicineRepository.findByIdAndUserId(anyInt(), anyInt())).thenReturn(testMedicine);

        MedicineResponse response = medicineService.getMedicineById(1);

        assertNotNull(response);
        assertEquals("感冒药", response.getName());
        assertEquals(100, response.getStock());
    }

    @Test
    @DisplayName("获取药品详情 - 不存在抛出异常")
    void getMedicineById_NotExists_ThrowsResourceNotFoundException() {
        when(medicineRepository.findByIdAndUserId(anyInt(), anyInt())).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
                () -> medicineService.getMedicineById(999));

        assertEquals("药品不存在", exception.getMessage());
    }

    @Test
    @DisplayName("更新药品 - 成功更新")
    void updateMedicine_Success_UpdatesMedicine() {
        when(medicineRepository.findByIdAndUserId(anyInt(), anyInt())).thenReturn(testMedicine);
        when(medicineRepository.save(any(Medicine.class))).thenReturn(testMedicine);

        MedicineCreateRequest updateRequest = new MedicineCreateRequest("新感冒药", "感冒用药", 
                "20片/盒", "YY制药", 200, null, 5);
        MedicineResponse response = medicineService.updateMedicine(1, updateRequest);

        assertNotNull(response);
        verify(medicineRepository).save(medicineCaptor.capture());
        
        Medicine updatedMedicine = medicineCaptor.getValue();
        assertEquals("新感冒药", updatedMedicine.getName());
        assertEquals("感冒用药", updatedMedicine.getCategory());
        assertEquals("20片/盒", updatedMedicine.getSpec());
        assertEquals("YY制药", updatedMedicine.getManufacturer());
        assertEquals(200, updatedMedicine.getStock());
    }

    @Test
    @DisplayName("更新药品 - 不存在抛出异常")
    void updateMedicine_NotExists_ThrowsResourceNotFoundException() {
        when(medicineRepository.findByIdAndUserId(anyInt(), anyInt())).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
                () -> medicineService.updateMedicine(999, createRequest));

        assertEquals("药品不存在", exception.getMessage());
        verify(medicineRepository, never()).save(any(Medicine.class));
    }

    @Test
    @DisplayName("删除药品 - 成功删除")
    void deleteMedicine_Success_DeletesMedicine() {
        when(medicineRepository.findByIdAndUserId(anyInt(), anyInt())).thenReturn(testMedicine);
        doNothing().when(medicineRepository).delete(any(Medicine.class));

        medicineService.deleteMedicine(1);

        verify(medicineRepository).delete(testMedicine);
    }

    @Test
    @DisplayName("删除药品 - 不存在抛出异常")
    void deleteMedicine_NotExists_ThrowsResourceNotFoundException() {
        when(medicineRepository.findByIdAndUserId(anyInt(), anyInt())).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
                () -> medicineService.deleteMedicine(999));

        assertEquals("药品不存在", exception.getMessage());
        verify(medicineRepository, never()).delete(any(Medicine.class));
    }
}