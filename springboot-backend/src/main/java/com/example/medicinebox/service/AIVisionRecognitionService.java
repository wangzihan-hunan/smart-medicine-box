package com.example.medicinebox.service;

import com.example.medicinebox.dto.response.MedicineRecognitionResponse;
import com.example.medicinebox.util.ImageUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI视觉药方识别服务类
 * 核心功能：基于 Kimi（月之暗面）Vision API 实现药方图片智能识别，
 * 使用 OpenAI 兼容的 Chat Completions 格式，支持提取药品名称、规格、用法用量等信息，
 * 集成套餐用量检查，支持图片压缩、API调用、响应解析、JSON提取等完整流程
 */
@Service
public class AIVisionRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(AIVisionRecognitionService.class);

    // Kimi API Chat Completions 端点
    private static final String API_URL = "https://api.moonshot.cn/v1/chat/completions";

    // API密钥，优先使用 kimi 配置，回退到旧的 qwen 配置
    @Value("${ai.kimi.api-key:${ai.qwen.api-key:${AI_QWEN_API_KEY:}}}")
    private String apiKey;

    // 使用的模型名称，默认 kimi-k2.6（Kimi 多模态视觉模型）
    @Value("${ai.kimi.model:${ai.qwen.model:kimi-k2.6}}")
    private String model;

    // 是否启用AI识别服务
    @Value("${ai.kimi.enabled:${ai.qwen.enabled:false}}")
    private boolean enabled;

    // API调用超时时间（毫秒）
    @Value("${ai.kimi.timeout:${ai.qwen.timeout:60000}}")
    private int timeout;

    // 图片最大压缩后大小（字节）
    @Value("${ai.kimi.max-image-size:${ai.qwen.max-image-size:512000}}")
    private int maxImageSize;

    // JSON序列化工具
    private final ObjectMapper objectMapper;
    // 套餐服务，用于用量检查和扣减
    private final PlanService planService;
    // HTTP客户端
    private OkHttpClient httpClient;

    /**
     * 构造函数，注入JSON工具和套餐服务
     *
     * @param objectMapper JSON序列化工具
     * @param planService 套餐服务
     */
    public AIVisionRecognitionService(ObjectMapper objectMapper, PlanService planService) {
        this.objectMapper = objectMapper;
        this.planService = planService;
    }

    /**
     * 初始化方法，创建HTTP客户端并输出服务启用状态
     */
    @PostConstruct
    public void init() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .build();

        if (!enabled || apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("AI Vision 识别服务未启用。请配置 ai.kimi.api-key 和 ai.kimi.enabled=true");
        } else {
            log.info("AI Vision 识别服务已初始化 (Kimi), 模型: {}", model);
        }
    }

    /**
     * 判断是否可用
     */
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * AI 识别药方图片（不带用户ID，不做套餐检查）
     *
     * @param imageFile 上传的图片文件
     * @return 识别结果
     */
    public MedicineRecognitionResponse recognize(MultipartFile imageFile) {
        return recognize(imageFile, null);
    }

    /**
     * AI 识别药方图片（带用户ID，进行套餐检查）
     *
     * @param imageFile 上传的图片文件
     * @param userId 用户ID，用于套餐权限检查
     * @return 识别结果
     */
    public MedicineRecognitionResponse recognize(MultipartFile imageFile, Integer userId) {
        if (!isAvailable()) {
            return MedicineRecognitionResponse.builder()
                    .success(false)
                    .message("AI 识别服务未配置，请联系管理员")
                    .build();
        }

        if (imageFile == null || imageFile.isEmpty()) {
            return MedicineRecognitionResponse.builder()
                    .success(false)
                    .message("请选择图片")
                    .build();
        }

        // 套餐权限检查
        if (userId != null) {
            if (!planService.canUseAiRecognition(userId)) {
                int remaining = planService.getRemainingAiCount(userId);
                log.warn("User {} AI recognition quota exceeded, remaining: {}", userId, remaining);
                return MedicineRecognitionResponse.builder()
                        .success(false)
                        .message("本月 AI 识别次数已用完（剩余 " + remaining + " 次），请升级套餐")
                        .build();
            }
        }

        long startTime = System.currentTimeMillis();
        try {
            // 1. 图片压缩并转 Base64
            byte[] imageBytes = imageFile.getBytes();
            String originalFilename = imageFile.getOriginalFilename();
            String mimeType = ImageUtils.getMimeType(originalFilename);

            // WebP 格式警告：Java ImageIO 不支持 WebP，无法压缩，直接发送原始字节
            if (mimeType != null && mimeType.contains("webp")) {
                log.warn("WebP 格式图片，Java ImageIO 不支持压缩，将直接发送原始字节到 AI API");
            }

            String base64Image = ImageUtils.compressAndEncodeToBase64(imageBytes, maxImageSize);

            log.info("AI 识别请求, 用户: {}, 文件名: {}, 原始大小: {} bytes, MIME: {}, 压缩后 Base64 长度: {}",
                    userId, originalFilename, imageBytes.length, mimeType, base64Image.length());

            // 2. 构造 OpenAI 兼容格式请求体
            String requestBody = buildRequestBody(base64Image, mimeType);

            // 3. 调用 API + 扣减用量
            String responseBody;
            try {
                responseBody = callApi(requestBody);
            } finally {
                if (userId != null) {
                    planService.consumeAiRecognition(userId);
                }
            }

            // 4. 解析 OpenAI 兼容格式响应
            MedicineRecognitionResponse result = parseResponse(responseBody);

            long cost = System.currentTimeMillis() - startTime;
            log.info("AI 识别完成, 用户: {}, 耗时: {}ms, 识别到 {} 种药品, 成功: {}", userId, cost,
                    result.getMedicines() != null ? result.getMedicines().size() : 0, result.isSuccess());

            return result;

        } catch (IOException e) {
            log.error("读取图片失败: {}", e.getMessage());
            return MedicineRecognitionResponse.builder()
                    .success(false)
                    .message("读取图片失败: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("AI 识别过程发生错误: {}", e.getMessage(), e);
            return MedicineRecognitionResponse.builder()
                    .success(false)
                    .message("AI 识别失败，请稍后重试或手动输入")
                    .build();
        }
    }

    /**
     * 构建 OpenAI 兼容格式的 API 请求体
     * Kimi API 使用标准 Chat Completions 格式，图片通过 image_url 类型传入
     */
    private String buildRequestBody(String base64Image, String mimeType) throws JsonProcessingException {
        String prompt = "你是一个专业的药方识别专家。请仔细分析这张药方图片，提取以下信息并以严格的JSON格式返回：\n" +
                "{\n" +
                "  \"medicines\": [\n" +
                "    {\n" +
                "      \"name\": \"药品名称\",\n" +
                "      \"spec\": \"规格\",\n" +
                "      \"dosage\": \"用法用量\",\n" +
                "      \"frequency\": \"用药频次\",\n" +
                "      \"quantity\": \"数量\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"diagnosis\": \"诊断结果\",\n" +
                "  \"doctor\": \"医生姓名\",\n" +
                "  \"hospital\": \"医院名称\",\n" +
                "  \"date\": \"开具日期\"\n" +
                "}\n" +
                "要求：\n" +
                "1. 只返回JSON，不要包含任何其他文字或解释\n" +
                "2. 如果无法识别某字段，返回空字符串\n" +
                "3. 药品名称要准确，规格要包含剂量单位\n" +
                "4. 用法用量要具体，如'每次1片，每日3次'\n" +
                "5. 数量要包含单位，如'7片'或'1盒'";

        String dataUrl = "data:" + mimeType + ";base64," + base64Image;

        // 构建 user message（OpenAI 兼容格式：image_url + text）
        java.util.Map<String, Object> imageUrlObj = new java.util.HashMap<>();
        imageUrlObj.put("url", dataUrl);

        java.util.Map<String, Object> imagePart = new java.util.HashMap<>();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrlObj);

        java.util.Map<String, Object> textPart = new java.util.HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", prompt);

        java.util.List<java.util.Map<String, Object>> userContent = new java.util.ArrayList<>();
        userContent.add(imagePart);
        userContent.add(textPart);

        java.util.Map<String, Object> userMessage = new java.util.HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userContent);

        // 构建 messages 数组（Kimi 多模态推荐只使用 user 消息）
        java.util.List<java.util.Map<String, Object>> messages = new java.util.ArrayList<>();
        messages.add(userMessage);

        // 构建完整请求体（OpenAI 兼容格式）
        java.util.Map<String, Object> request = new java.util.HashMap<>();
        request.put("model", model);
        request.put("messages", messages);
        request.put("max_tokens", 2048);
        // kimi-k2.6 模型要求 temperature 必须为 1，不设置 temperature 使用默认值

        return objectMapper.writeValueAsString(request);
    }

    /**
     * 调用 Kimi API
     */
    private String callApi(String requestBody) throws IOException {
        RequestBody body = RequestBody.create(requestBody, MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        log.debug("AI API 请求体长度: {} chars, 模型: {}", requestBody.length(), model);

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                log.error("AI API 调用失败, HTTP状态码: {}, 响应: {}", response.code(),
                        responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody);
                throw new IOException("AI API 调用失败: HTTP " + response.code() + " - " +
                        (responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody));
            }

            // 记录原始响应用于调试
            log.debug("AI API 原始响应 (前500字符): {}",
                    responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody);

            return responseBody;
        }
    }

    /**
     * 解析 OpenAI 兼容格式的 API 响应
     * Kimi 返回格式：{ "choices": [{ "message": { "content": "..." } }] }
     */
    private MedicineRecognitionResponse parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // 检查 API 错误（OpenAI 兼容格式：{ "error": { "message": "..." } }）
            if (root.has("error")) {
                JsonNode errorNode = root.get("error");
                String errorMessage = errorNode.has("message") ? errorNode.get("message").asText() : "未知错误";
                log.error("AI API 返回错误: {}", errorMessage);
                return MedicineRecognitionResponse.builder()
                        .success(false)
                        .message("AI 服务错误: " + errorMessage)
                        .build();
            }

            // 提取内容（OpenAI 兼容格式：choices[0].message.content）
            String content = "";
            if (root.has("choices") && root.get("choices").isArray()) {
                JsonNode choices = root.get("choices");
                if (choices.size() > 0) {
                    JsonNode firstChoice = choices.get(0);
                    if (firstChoice.has("message")) {
                        JsonNode message = firstChoice.get("message");
                        if (message.has("content")) {
                            content = message.get("content").asText();
                        }
                    }
                }
            }

            if (content == null || content.trim().isEmpty()) {
                log.warn("AI 响应内容为空, 原始响应(前300字符): {}",
                        responseBody.length() > 300 ? responseBody.substring(0, 300) : responseBody);
                return MedicineRecognitionResponse.builder()
                        .success(false)
                        .message("AI 未能识别出内容，请尝试：1) 确保图片清晰可见 2) 使用 JPG/PNG 格式 3) 重新拍照上传")
                        .build();
            }

            log.debug("AI 原始响应内容: {}", content);

            // 提取 JSON
            String jsonStr = extractJson(content);
            if (jsonStr == null) {
                log.warn("无法从 AI 响应中提取 JSON: {}", content);
                return MedicineRecognitionResponse.builder()
                        .success(false)
                        .message("AI 响应格式异常，请稍后重试")
                        .recognizedText(content)
                        .build();
            }

            // 解析 JSON
            JsonNode resultNode = objectMapper.readTree(jsonStr);
            MedicineRecognitionResponse response = MedicineRecognitionResponse.builder()
                    .success(true)
                    .message("识别成功")
                    .recognizedText(content)
                    .build();

            // 解析 medicines
            if (resultNode.has("medicines") && resultNode.get("medicines").isArray()) {
                List<MedicineRecognitionResponse.MedicineInfo> medicines = new ArrayList<>();
                for (JsonNode medNode : resultNode.get("medicines")) {
                    MedicineRecognitionResponse.MedicineInfo info = MedicineRecognitionResponse.MedicineInfo.builder()
                            .name(getTextOrEmpty(medNode, "name"))
                            .spec(getTextOrEmpty(medNode, "spec"))
                            .dosage(getTextOrEmpty(medNode, "dosage"))
                            .frequency(getTextOrEmpty(medNode, "frequency"))
                            .quantity(getTextOrEmpty(medNode, "quantity"))
                            .build();
                    medicines.add(info);
                }
                response.setMedicines(medicines);

                // 兼容旧字段：取第一个药品作为 medicineInfo
                if (!medicines.isEmpty()) {
                    response.setMedicineInfo(medicines.get(0));
                }
            }

            // 解析其他字段
            response.setDiagnosis(getTextOrEmpty(resultNode, "diagnosis"));
            response.setDoctor(getTextOrEmpty(resultNode, "doctor"));
            response.setHospital(getTextOrEmpty(resultNode, "hospital"));
            response.setDate(getTextOrEmpty(resultNode, "date"));

            return response;

        } catch (JsonProcessingException e) {
            log.error("解析 AI 响应 JSON 失败: {}", e.getMessage());
            return MedicineRecognitionResponse.builder()
                    .success(false)
                    .message("解析识别结果失败")
                    .recognizedText(responseBody)
                    .build();
        }
    }

    /**
     * 从文本中提取 JSON 字符串
     */
    private String extractJson(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        text = text.trim();

        // 策略 1：提取 ```json ... ``` 代码块
        Pattern codeBlockPattern = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```");
        Matcher codeBlockMatcher = codeBlockPattern.matcher(text);
        if (codeBlockMatcher.find()) {
            return codeBlockMatcher.group(1).trim();
        }

        // 策略 2：提取 ``` ... ``` 代码块（无语言标记）
        Pattern genericBlockPattern = Pattern.compile("```\\s*([\\s\\S]*?)\\s*```");
        Matcher genericBlockMatcher = genericBlockPattern.matcher(text);
        if (genericBlockMatcher.find()) {
            String candidate = genericBlockMatcher.group(1).trim();
            if (candidate.startsWith("{") || candidate.startsWith("[")) {
                return candidate;
            }
        }

        // 策略 3：提取第一个 { ... } 或 [ ... ]
        if (text.startsWith("{") || text.startsWith("[")) {
            return text;
        }

        // 策略 4：在文本中搜索 JSON 对象
        Pattern jsonPattern = Pattern.compile("(\\{[\\s\\S]*\\})");
        Matcher jsonMatcher = jsonPattern.matcher(text);
        if (jsonMatcher.find()) {
            return jsonMatcher.group(1).trim();
        }

        return null;
    }

    /**
     * 安全获取文本字段
     */
    private String getTextOrEmpty(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText("");
        }
        return "";
    }
}