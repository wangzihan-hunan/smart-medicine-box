package com.example.medicinebox.controller;

import com.example.medicinebox.dto.request.MedicineCreateRequest;
import com.example.medicinebox.dto.response.BaseResponse;
import com.example.medicinebox.dto.response.MedicineListResponse;
import com.example.medicinebox.dto.response.MedicineRecognitionResponse;
import com.example.medicinebox.dto.response.MedicineResponse;
import com.example.medicinebox.service.AIVisionRecognitionService;
import com.example.medicinebox.service.MedicineMatchingService;
import com.example.medicinebox.service.MedicineRecognitionService;
import com.example.medicinebox.service.MedicineService;
import com.example.medicinebox.util.PaginationUtil;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 药品管理控制器
 * 提供药品的增删改查、分类管理、库存预警、过期提醒、AI识别等功能
 * 路由前缀：/api/v1/medicines
 */
@RestController
@RequestMapping("/api/v1/medicines")
public class MedicineController {

    private static final Logger log = LoggerFactory.getLogger(MedicineController.class);

    private final MedicineService medicineService;
    private final MedicineRecognitionService recognitionService;
    private final AIVisionRecognitionService aiVisionService;
    private final MedicineMatchingService matchingService;

    public MedicineController(MedicineService medicineService,
                              MedicineRecognitionService recognitionService,
                              AIVisionRecognitionService aiVisionService,
                              MedicineMatchingService matchingService) {
        this.medicineService = medicineService;
        this.recognitionService = recognitionService;
        this.aiVisionService = aiVisionService;
        this.matchingService = matchingService;
    }

    /**
     * 添加药品接口
     * HTTP方法：POST
     * 权限要求：已登录用户
     * 
     * @param request 药品创建请求体，包含药品名称、分类、规格、库存等信息
     * @return 添加成功的药品详细信息
     */
    @PostMapping
    public BaseResponse<MedicineResponse> addMedicine(@Valid @RequestBody MedicineCreateRequest request) {
        log.info("添加药品请求, 名称: {}, 分类: {}", request.getName(), request.getCategory());
        MedicineResponse response = medicineService.addMedicine(request);
        log.info("添加药品成功, 药品ID: {}", response.getId());
        return BaseResponse.success("成功添加药品：" + request.getName(), response);
    }

    /**
     * 分页查询药品列表接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 支持按关键词、分类、低库存、过期等条件筛选
     * 
     * @param keyword 搜索关键词（可选）
     * @param category 药品分类（可选）
     * @param lowStock 低库存阈值（可选）
     * @param expired 是否只显示过期药品（可选）
     * @param page 页码，默认0
     * @param size 每页数量，默认20
     * @return 药品列表响应，包含总数和药品信息列表
     */
    @GetMapping
    public BaseResponse<MedicineListResponse> getMedicines(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer lowStock,
            @RequestParam(required = false) Boolean expired,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int validatedPage = PaginationUtil.validatePage(page);
        int validatedSize = PaginationUtil.validateSize(size);
        log.info("查询药品列表, 关键词: {}, 分类: {}, 低库存: {}, 过期: {}, 页码: {}, 大小: {}",
                keyword, category, lowStock, expired, validatedPage, validatedSize);
        MedicineListResponse response = medicineService.getMedicines(validatedPage, validatedSize, keyword, category, lowStock, expired);
        log.info("查询药品列表成功, 总数: {}", response.getTotalElements());
        return BaseResponse.success("查询所有药品成功", response);
    }

    /**
     * 获取药品分类列表接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @return 所有药品分类名称列表
     */
    @GetMapping("/categories")
    public BaseResponse<List<String>> getCategories() {
        log.info("查询药品分类");
        List<String> categories = medicineService.getCategories();
        return BaseResponse.success("查询分类成功", categories);
    }

    /**
     * 获取低库存药品列表接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @return 库存不足的药品列表
     */
    @GetMapping("/low-stock")
    public BaseResponse<List<MedicineResponse>> getLowStockMedicines() {
        log.info("查询低库存药品");
        List<MedicineResponse> medicines = medicineService.getLowStockMedicines();
        return BaseResponse.success("查询低库存药品成功", medicines);
    }

    /**
     * 获取过期药品列表接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @return 已过期的药品列表
     */
    @GetMapping("/expired")
    public BaseResponse<List<MedicineResponse>> getExpiredMedicines() {
        log.info("查询过期药品");
        List<MedicineResponse> medicines = medicineService.getExpiredMedicines();
        return BaseResponse.success("查询过期药品成功", medicines);
    }

    /**
     * 根据ID查询药品详情接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @param id 药品ID
     * @return 药品详细信息
     */
    @GetMapping("/{id}")
    public BaseResponse<MedicineResponse> getMedicineById(@PathVariable Integer id) {
        log.info("查询药品详情, ID: {}", id);
        MedicineResponse response = medicineService.getMedicineById(id);
        return BaseResponse.success("查询药品成功", response);
    }

    /**
     * 更新药品信息接口
     * HTTP方法：PUT
     * 权限要求：已登录用户
     * 
     * @param id 药品ID
     * @param request 药品更新请求体，包含新的药品信息
     * @return 更新后的药品详细信息
     */
    @PutMapping("/{id}")
    public BaseResponse<MedicineResponse> updateMedicine(@PathVariable Integer id, @Valid @RequestBody MedicineCreateRequest request) {
        log.info("更新药品请求, ID: {}, 名称: {}", id, request.getName());
        MedicineResponse response = medicineService.updateMedicine(id, request);
        log.info("更新药品成功, 药品ID: {}", response.getId());
        return BaseResponse.success("更新药品成功", response);
    }

    /**
     * 删除药品接口
     * HTTP方法：DELETE
     * 权限要求：已登录用户
     * 
     * @param id 药品ID
     * @return 操作结果，无返回数据
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Void> deleteMedicine(@PathVariable Integer id) {
        log.info("删除药品请求, ID: {}", id);
        medicineService.deleteMedicine(id);
        log.info("删除药品成功, ID: {}", id);
        return BaseResponse.success("删除药品成功");
    }

    /**
     * 药品图片识别接口
     * HTTP方法：POST
     * 权限要求：已登录用户
     * 支持AI识别和OCR识别两种方式，auto模式下优先使用AI识别
     * 
     * @param image 上传的药品图片文件
     * @param method 识别方式：ai（AI识别）、ocr（OCR识别）、auto（自动选择，默认）
     * @return 药品识别结果，包含识别到的药品列表
     */
    @PostMapping("/recognize")
    public BaseResponse<MedicineRecognitionResponse> recognizeMedicine(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "method", defaultValue = "auto") String method) {

        if (image == null || image.isEmpty()) {
            log.warn("药品识别请求, 图片为空");
            return BaseResponse.error(400, "请选择图片");
        }

        log.info("药品识别请求, 文件名: {}, 大小: {} bytes, 识别方式: {}",
                image.getOriginalFilename(), image.getSize(), method);

        // 通过 Service 层获取当前用户ID
        Integer userId = medicineService.getCurrentUserId();

        MedicineRecognitionResponse response;

        // 识别策略：auto 模式下优先 AI，AI 不可用时降级到 Tesseract
        if ("ai".equalsIgnoreCase(method)) {
            response = aiVisionService.recognize(image, userId);
        } else if ("ocr".equalsIgnoreCase(method)) {
            response = recognitionService.recognizeMedicine(image);
        } else {
            // auto 模式：优先 AI
            if (aiVisionService.isAvailable()) {
                response = aiVisionService.recognize(image, userId);
            } else {
                log.info("AI 服务不可用，降级到 OCR 识别");
                response = recognitionService.recognizeMedicine(image);
            }
        }

        if (response.isSuccess()) {
            int medicineCount = response.getMedicines() != null ? response.getMedicines().size() : 0;
            log.info("药品识别成功, 识别到 {} 种药品", medicineCount);
            return BaseResponse.success("识别成功", response);
        } else {
            log.warn("药品识别失败: {}", response.getMessage());
            return BaseResponse.error(400, response.getMessage());
        }
    }

    /**
     * 药品识别结果匹配接口
     * 将识别到的药品列表与数据库中已有药品进行模糊匹配
     * HTTP方法：POST
     * 权限要求：已登录用户
     *
     * @param recognitionResult 识别结果（来自 OCR 或 AI 识别）
     * @return 匹配结果列表，包含识别信息和匹配到的数据库药品
     */
    @PostMapping("/match")
    public BaseResponse<List<MedicineMatchingService.MedicineMatchResult>> matchMedicines(
            @RequestBody MedicineRecognitionResponse recognitionResult) {
        log.info("药品匹配请求, 识别药品数: {}",
                recognitionResult.getMedicines() != null ? recognitionResult.getMedicines().size() : 0);

        List<MedicineMatchingService.MedicineMatchResult> matchResults =
                matchingService.matchRecognitionResults(recognitionResult);

        long matchedCount = matchResults.stream().filter(MedicineMatchingService.MedicineMatchResult::isMatched).count();
        log.info("药品匹配完成, 总药品: {}, 匹配: {}", matchResults.size(), matchedCount);

        return BaseResponse.success("匹配完成", matchResults);
    }

    /**
     * 批量入库接口
     * 将匹配确认后的药品批量保存到数据库（新增或更新库存）
     * HTTP方法：POST
     * 权限要求：已登录用户
     *
     * @param items 待入库的药品列表
     * @return 入库结果（新增/更新/跳过数量）
     */
    @PostMapping("/batch-add")
    public BaseResponse<MedicineMatchingService.BatchAddResult> batchAddMedicines(
            @RequestBody List<MedicineMatchingService.BatchAddItem> items) {
        log.info("批量入库请求, 药品数: {}", items != null ? items.size() : 0);

        if (items == null || items.isEmpty()) {
            return BaseResponse.error(400, "请选择要入库的药品");
        }

        MedicineMatchingService.BatchAddResult result = matchingService.batchAddMedicines(items);
        log.info("批量入库完成, 新增: {}, 更新: {}, 跳过: {}",
                result.getNewCount(), result.getUpdateCount(), result.getSkipCount());

        return BaseResponse.success("入库完成", result);
    }

    /**
     * 导出药品数据接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @return CSV格式的药品数据字符串
     */
    @GetMapping("/export")
    public BaseResponse<String> exportMedicines() {
        log.info("导出药品数据");
        String csv = medicineService.exportToCsv();
        return BaseResponse.success("导出成功", csv);
    }
}
