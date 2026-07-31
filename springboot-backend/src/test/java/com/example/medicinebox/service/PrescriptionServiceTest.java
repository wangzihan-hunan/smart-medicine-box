package com.example.medicinebox.service;

import com.example.medicinebox.config.FileUploadProperties;
import com.example.medicinebox.dto.response.PrescriptionListResponse;
import com.example.medicinebox.dto.response.PrescriptionResponse;
import com.example.medicinebox.entity.Prescription;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.BadRequestException;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.PrescriptionRepository;
import com.example.medicinebox.repository.UserRepository;
import com.example.medicinebox.util.TestSecurityContextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * PrescriptionService 单元测试
 * 覆盖药方上传、历史查询、删除、更新等核心功能
 * Mock 数量：2（PrescriptionRepository, UserRepository）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PrescriptionService 单元测试")
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MultipartFile mockFile;

    private FileUploadProperties fileUploadProperties;
    private PrescriptionService prescriptionService;

    private User testUser;
    private Prescription testPrescription;

    @BeforeEach
    void setUp() {
        fileUploadProperties = new FileUploadProperties();
        prescriptionService = new PrescriptionService(userRepository, prescriptionRepository, fileUploadProperties);

        testUser = new User();
        testUser.setId(1);
        testUser.setPhone("13800138000");
        testUser.setName("测试用户");

        testPrescription = new Prescription();
        testPrescription.setId(1);
        testPrescription.setUserId(1);
        testPrescription.setFileUrl("./uploads/test123.jpg");
        testPrescription.setOriginalFilename("处方单.jpg");
        testPrescription.setDiagnosis("高血压");
        testPrescription.setDosageInstructions("每日一次，每次一片");
        testPrescription.setUploadDate(LocalDateTime.now());

        TestSecurityContextHelper.setCurrentUser(testUser.getPhone(), "USER");
        when(userRepository.findByPhone(testUser.getPhone())).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void tearDown() {
        TestSecurityContextHelper.clearContext();
    }

    // ========== 药方上传测试 ==========

    @Test
    @DisplayName("上传药方 - 成功上传并返回药方信息")
    void uploadPrescription_ValidFile_ReturnsPrescription() throws IOException {
        when(mockFile.getOriginalFilename()).thenReturn("处方单.jpg");
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        when(mockFile.getSize()).thenReturn(1024L);
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(testPrescription);

        PrescriptionResponse response = prescriptionService.uploadPrescription(mockFile, "高血压", "每日一次");

        assertNotNull(response, "响应不应为空");
        assertEquals("高血压", response.getDiagnosis(), "诊断结果应匹配");
        verify(prescriptionRepository).save(any(Prescription.class));
    }

    @Test
    @DisplayName("上传药方 - 文件名为空时抛出异常")
    void uploadPrescription_EmptyFilename_ThrowsBadRequest() {
        when(mockFile.getOriginalFilename()).thenReturn("");

        assertThrows(BadRequestException.class,
                () -> prescriptionService.uploadPrescription(mockFile, null, null),
                "空文件名应抛出异常");
    }

    @Test
    @DisplayName("上传药方 - 无扩展名文件抛出异常")
    void uploadPrescription_NoExtension_ThrowsBadRequest() {
        when(mockFile.getOriginalFilename()).thenReturn("noextension");

        assertThrows(BadRequestException.class,
                () -> prescriptionService.uploadPrescription(mockFile, null, null),
                "无扩展名文件应抛出异常");
    }

    @Test
    @DisplayName("上传药方 - 不允许的文件类型抛出异常")
    void uploadPrescription_InvalidExtension_ThrowsBadRequest() {
        when(mockFile.getOriginalFilename()).thenReturn("virus.exe");

        assertThrows(BadRequestException.class,
                () -> prescriptionService.uploadPrescription(mockFile, null, null),
                "不允许的文件类型应抛出异常");
    }

    @Test
    @DisplayName("上传药方 - MIME类型不匹配抛出异常")
    void uploadPrescription_MimeMismatch_ThrowsBadRequest() {
        when(mockFile.getOriginalFilename()).thenReturn("test.pdf");
        when(mockFile.getContentType()).thenReturn("image/png");

        assertThrows(BadRequestException.class,
                () -> prescriptionService.uploadPrescription(mockFile, null, null),
                "MIME类型不匹配应抛出异常");
    }

    // ========== 药方历史查询测试 ==========

    @Test
    @DisplayName("查询药方历史 - 有记录时返回分页列表")
    void getPrescriptionHistory_HasRecords_ReturnsPaginatedList() {
        Page<Prescription> page = new PageImpl<>(Arrays.asList(testPrescription));
        when(prescriptionRepository.findByUserId(anyInt(), any(Pageable.class))).thenReturn(page);

        PrescriptionListResponse response = prescriptionService.getPrescriptionHistory(0, 20);

        assertNotNull(response, "响应不应为空");
        assertEquals(1, response.getTotalElements(), "总数应为1");
        assertEquals(1, response.getList().size(), "列表应有1条记录");
    }

    @Test
    @DisplayName("查询药方历史 - 无记录时返回空列表")
    void getPrescriptionHistory_NoRecords_ReturnsEmptyList() {
        Page<Prescription> emptyPage = new PageImpl<>(Collections.emptyList());
        when(prescriptionRepository.findByUserId(anyInt(), any(Pageable.class))).thenReturn(emptyPage);

        PrescriptionListResponse response = prescriptionService.getPrescriptionHistory(0, 20);

        assertNotNull(response, "响应不应为空");
        assertEquals(0, response.getTotalElements(), "总数应为0");
    }

    @Test
    @DisplayName("查询药方历史 - pageSize 超过上限时自动限制为 100")
    void getPrescriptionHistory_LargeSize_ClampedToMax() {
        Page<Prescription> page = new PageImpl<>(Collections.emptyList());
        when(prescriptionRepository.findByUserId(anyInt(), any(Pageable.class))).thenReturn(page);

        // 传入 size=500，应被限制为 100
        prescriptionService.getPrescriptionHistory(0, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(prescriptionRepository).findByUserId(anyInt(), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber(), "页码应为0");
        assertEquals(100, captor.getValue().getPageSize(), "pageSize 应被限制为 100");
    }

    // ========== 药方删除测试 ==========

    @Test
    @DisplayName("删除药方 - 药方存在时成功删除")
    void deletePrescription_Exists_DeletesSuccessfully() {
        when(prescriptionRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(testPrescription));
        doNothing().when(prescriptionRepository).delete(any(Prescription.class));

        assertDoesNotThrow(() -> prescriptionService.deletePrescription(1),
                "删除存在的药方不应抛出异常");
        verify(prescriptionRepository).delete(any(Prescription.class));
    }

    @Test
    @DisplayName("删除药方 - 药方不存在时抛出异常")
    void deletePrescription_NotFound_ThrowsResourceNotFoundException() {
        when(prescriptionRepository.findByIdAndUserId(999, 1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> prescriptionService.deletePrescription(999),
                "不存在的药方应抛出异常");
    }

    // ========== 药方更新测试 ==========

    @Test
    @DisplayName("更新药方 - 成功更新诊断和用药说明")
    void updatePrescription_Exists_UpdatesSuccessfully() {
        when(prescriptionRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(testPrescription));
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(testPrescription);

        PrescriptionResponse response = prescriptionService.updatePrescription(1, "新诊断", "新用法");

        assertNotNull(response, "响应不应为空");
        assertEquals("新诊断", testPrescription.getDiagnosis(), "诊断应更新");
        assertEquals("新用法", testPrescription.getDosageInstructions(), "用药说明应更新");
        verify(prescriptionRepository).save(any(Prescription.class));
    }

    @Test
    @DisplayName("更新药方 - 药方不存在时抛出异常")
    void updatePrescription_NotFound_ThrowsResourceNotFoundException() {
        when(prescriptionRepository.findByIdAndUserId(999, 1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> prescriptionService.updatePrescription(999, "诊断", "用法"),
                "不存在的药方应抛出异常");
    }
}