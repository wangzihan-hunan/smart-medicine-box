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
import com.example.medicinebox.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 药方管理服务类
 * 核心功能：药方上传（含文件类型校验）、药方历史查询、药方删除、药方信息更新等
 */
@Service
public class PrescriptionService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionService.class);

    private final PrescriptionRepository prescriptionRepository;
    private final FileUploadProperties fileUploadProperties;

    /**
     * 构造函数，注入依赖
     *
     * @param prescriptionRepository 药方数据访问层
     * @param fileUploadProperties 文件上传配置
     */
    public PrescriptionService(UserRepository userRepository, PrescriptionRepository prescriptionRepository,
                               FileUploadProperties fileUploadProperties) {
        super(userRepository);
        this.prescriptionRepository = prescriptionRepository;
        this.fileUploadProperties = fileUploadProperties;
    }

    /**
     * 上传药方文件
     * 校验文件类型和MIME类型，使用UUID生成唯一文件名，保存文件并记录药方信息
     *
     * @param file 上传的药方文件
     * @param diagnosis 诊断结果
     * @param dosageInstructions 用药说明
     * @return 药方响应对象
     * @throws IOException 文件读写失败时抛出
     * @throws BadRequestException 文件校验失败时抛出
     */
    @Transactional
    public PrescriptionResponse uploadPrescription(MultipartFile file, String diagnosis, String dosageInstructions) throws IOException {
        User user = getCurrentUser();
        log.debug("上传药方, 用户ID: {}, 文件名: {}, 大小: {} bytes", user.getId(), file.getOriginalFilename(), file.getSize());

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            log.warn("文件名不能为空");
            throw new BadRequestException("文件名不能为空");
        }

        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex == -1) {
            log.warn("文件必须包含扩展名");
            throw new BadRequestException("文件必须包含扩展名");
        }

        String extension = originalFilename.substring(dotIndex + 1).toLowerCase();
        if (!fileUploadProperties.isExtensionAllowed(extension)) {
            log.warn("不允许上传的文件类型: {}, 支持的类型: {}", extension, fileUploadProperties.getAllowedExtensions());
            throw new BadRequestException("不允许上传的文件类型，支持的类型: pdf, jpg, jpeg, png, gif, webp");
        }

        if (!fileUploadProperties.isMimeTypeValid(extension, file.getContentType())) {
            log.warn("文件类型与MIME类型不匹配, 扩展名: {}, MIME: {}", extension, file.getContentType());
            throw new BadRequestException("文件类型与MIME类型不匹配");
        }

        String storedFileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        Path uploadPath = Paths.get(fileUploadProperties.getUploadDir());
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(storedFileName);
        Files.copy(file.getInputStream(), filePath);

        Prescription prescription = new Prescription();
        prescription.setFileUrl(filePath.toString());
        prescription.setOriginalFilename(originalFilename);
        prescription.setUserId(user.getId());
        prescription.updateInfo(diagnosis, dosageInstructions);

        prescription = prescriptionRepository.save(prescription);

        log.debug("上传药方成功, 药方ID: {}", prescription.getId());
        return PrescriptionResponse.fromEntity(prescription);
    }

    /**
     * 分页查询药方历史记录
     * 按上传时间倒序排列，pageSize 上限 100
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页药方列表响应
     */
    @Transactional(readOnly = true)
    public PrescriptionListResponse getPrescriptionHistory(int page, int size) {
        User user = getCurrentUser();
        int validatedPage = PaginationUtil.validatePage(page);
        int validatedSize = PaginationUtil.validateSize(size);
        log.debug("查询药方列表, 用户ID: {}, 页码: {}, 大小: {}", user.getId(), validatedPage, validatedSize);

        Pageable pageable = PageRequest.of(validatedPage, validatedSize, Sort.by(Sort.Direction.DESC, "uploadDate"));
        Page<Prescription> prescriptionPage = prescriptionRepository.findByUserId(user.getId(), pageable);
        
        List<PrescriptionResponse> responseList = prescriptionPage.getContent().stream()
                .map(PrescriptionResponse::fromEntity)
                .collect(Collectors.toList());

        log.debug("查询药方列表成功, 用户ID: {}, 总数: {}", user.getId(), prescriptionPage.getTotalElements());
        return new PrescriptionListResponse(
                (int) prescriptionPage.getTotalElements(),
                prescriptionPage.getTotalPages(),
                prescriptionPage.getNumber(),
                responseList
        );
    }

    /**
     * 删除药方
     * 同时删除本地存储的文件，校验药方归属权
     *
     * @param prescriptionId 药方ID
     * @throws ResourceNotFoundException 药方不存在或无权限时抛出
     */
    @Transactional
    public void deletePrescription(Integer prescriptionId) {
        User user = getCurrentUser();
        log.debug("删除药方, 用户ID: {}, 药方ID: {}", user.getId(), prescriptionId);

        Prescription prescription = prescriptionRepository.findByIdAndUserId(prescriptionId, user.getId())
                .orElseThrow(() -> {
                    log.warn("药方不存在或无权限删除, 用户ID: {}, 药方ID: {}", user.getId(), prescriptionId);
                    return new ResourceNotFoundException("药方不存在或无权限删除");
                });

        // 删除本地文件
        if (prescription.getFileUrl() != null) {
            try {
                Path filePath = Paths.get(prescription.getFileUrl());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
            } catch (IOException e) {
                log.warn("删除药方文件失败, 药方ID: {}, 文件路径: {}", prescriptionId, prescription.getFileUrl(), e);
            }
        }

        prescriptionRepository.delete(prescription);
        log.debug("删除药方成功, 药方ID: {}", prescriptionId);
    }

    /**
     * 更新药方信息
     * 更新诊断结果和用药说明，校验药方归属权
     *
     * @param prescriptionId 药方ID
     * @param diagnosis 诊断结果
     * @param dosageInstructions 用药说明
     * @return 更新后的药方响应对象
     * @throws ResourceNotFoundException 药方不存在或无权限时抛出
     */
    @Transactional
    public PrescriptionResponse updatePrescription(Integer prescriptionId, String diagnosis, String dosageInstructions) {
        User user = getCurrentUser();
        log.debug("更新药方, 用户ID: {}, 药方ID: {}", user.getId(), prescriptionId);

        Prescription prescription = prescriptionRepository.findByIdAndUserId(prescriptionId, user.getId())
                .orElseThrow(() -> {
                    log.warn("药方不存在或无权限修改, 用户ID: {}, 药方ID: {}", user.getId(), prescriptionId);
                    return new ResourceNotFoundException("药方不存在或无权限修改");
                });

        prescription.updateInfo(diagnosis, dosageInstructions);

        prescription = prescriptionRepository.save(prescription);

        log.debug("更新药方成功, 药方ID: {}", prescription.getId());
        return PrescriptionResponse.fromEntity(prescription);
    }
}