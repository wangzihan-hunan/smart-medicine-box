package com.example.medicinebox.service;

import com.example.medicinebox.dto.response.MedicineRecognitionResponse;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 药品OCR识别服务类
 * 核心功能：基于Tess4J（Tesseract OCR引擎）实现药品图片文字识别，
 * 支持从药品说明书/包装图片中提取药品名称、类别、规格、生产厂家等信息，
 * 通过正则表达式多策略匹配提取结构化药品信息
 */
@Service
public class MedicineRecognitionService {

    private static final Logger logger = LoggerFactory.getLogger(MedicineRecognitionService.class);

    // 药品名称提取正则
    private static final Pattern NAME_PATTERN_1 = Pattern.compile("药品名称[：:]\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_PATTERN_2 = Pattern.compile("药名[：:]\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_PATTERN_3 = Pattern.compile("名称[：:]\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_PATTERN_4 = Pattern.compile("商品名[：:]\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_PATTERN_5 = Pattern.compile("【([^】]+)】", Pattern.CASE_INSENSITIVE);

    // 药品类别提取正则
    private static final Pattern CATEGORY_PATTERN_1 = Pattern.compile("类别[：:]\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CATEGORY_PATTERN_2 = Pattern.compile("分类[：:]\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CATEGORY_PATTERN_3 = Pattern.compile("剂型[：:]\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CATEGORY_PATTERN_4 = Pattern.compile("类型[：:]\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);

    // 药品规格提取正则
    private static final Pattern SPEC_PATTERN_1 = Pattern.compile("规格[：:]\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPEC_PATTERN_2 = Pattern.compile("规格型号[：:]\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPEC_PATTERN_3 = Pattern.compile("每片[^\\n]*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPEC_PATTERN_4 = Pattern.compile("每粒[^\\n]*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPEC_PATTERN_5 = Pattern.compile("每袋[^\\n]*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPEC_PATTERN_6 = Pattern.compile("\\d+[mg|g|ml|毫升|克]+.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPEC_PATTERN_7 = Pattern.compile("[0-9.]+\\s*[m]?[gl]\\s*[×x*]\\s*[0-9]+", Pattern.CASE_INSENSITIVE);

    // 生产厂家提取正则
    private static final Pattern MANUFACTURER_PATTERN_1 = Pattern.compile("生产厂家[：:]\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MANUFACTURER_PATTERN_2 = Pattern.compile("厂家[：:]\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MANUFACTURER_PATTERN_3 = Pattern.compile("生产商[：:]\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MANUFACTURER_PATTERN_4 = Pattern.compile("生产企业[：:]\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MANUFACTURER_PATTERN_5 = Pattern.compile("企业名称[：:]\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);

    // Tesseract OCR引擎实例
    private final ITesseract tesseract;
    // Tesseract是否可用（初始化失败时设为false）
    private boolean tesseractAvailable = true;

    /**
     * 构造函数，创建Tesseract实例并进行配置
     */
    public MedicineRecognitionService() {
        this.tesseract = new Tesseract();
        configureTesseract();
    }

    /**
     * 配置Tesseract OCR引擎
     * 设置训练数据路径、语言、分页模式，并进行可用性测试
     */
    private void configureTesseract() {
        try {
            String tessDataPath = System.getenv("TESSDATA_PREFIX");
            if (tessDataPath != null && !tessDataPath.isEmpty()) {
                tesseract.setDatapath(tessDataPath);
            } else {
                tesseract.setDatapath("tessdata");
            }
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(3);
            
            try {
                tesseract.doOCR(new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB));
            } catch (TesseractException e) {
                logger.warn("Tesseract初始化测试失败，可能缺少tessdata: {}", e.getMessage());
                tesseractAvailable = false;
            }
        } catch (Exception e) {
            logger.warn("无法初始化Tesseract OCR: {}", e.getMessage());
            tesseractAvailable = false;
        }
    }

    /**
     * 识别药品图片
     * 使用Tesseract OCR引擎识别图片中的文字，然后通过正则表达式提取结构化药品信息
     *
     * @param imageFile 上传的药品图片文件
     * @return 识别结果（包含原始识别文本和提取的药品信息）
     */
    public MedicineRecognitionResponse recognizeMedicine(MultipartFile imageFile) {
        // Tesseract不可用时直接返回错误
        if (!tesseractAvailable) {
            return MedicineRecognitionResponse.builder()
                    .success(false)
                    .message("OCR识别服务暂不可用，请手动输入药品信息")
                    .build();
        }

        // 校验图片文件是否为空
        if (imageFile == null || imageFile.isEmpty()) {
            return MedicineRecognitionResponse.builder()
                    .success(false)
                    .message("请选择图片")
                    .build();
        }

        try {
            // 读取图片为BufferedImage
            BufferedImage image = ImageIO.read(imageFile.getInputStream());
            if (image == null) {
                return MedicineRecognitionResponse.builder()
                        .success(false)
                        .message("无法读取图片，请检查文件格式是否正确（支持格式：JPG、JPEG、PNG、GIF、BMP）")
                        .build();
            }
            // 调用Tesseract进行OCR识别
            String text = tesseract.doOCR(image);
            
            logger.info("OCR识别文本: {}", text);

            // 从识别文本中提取结构化药品信息
            MedicineRecognitionResponse.MedicineInfo info = extractMedicineInfo(text);
            
            return MedicineRecognitionResponse.builder()
                    .success(true)
                    .message("识别成功")
                    .recognizedText(text)
                    .medicineInfo(info)
                    .build();

        } catch (IOException e) {
            logger.error("读取图片失败: {}", e.getMessage());
            return MedicineRecognitionResponse.builder()
                    .success(false)
                    .message("读取图片失败: " + e.getMessage())
                    .build();
        } catch (TesseractException e) {
            logger.error("OCR识别失败: {}", e.getMessage());
            return MedicineRecognitionResponse.builder()
                    .success(false)
                    .message("识别失败，请尝试手动输入")
                    .build();
        } catch (Exception e) {
            logger.error("识别过程发生错误: {}", e.getMessage());
            return MedicineRecognitionResponse.builder()
                    .success(false)
                    .message("识别过程发生错误")
                    .build();
        }
    }

    /**
     * 从OCR识别文本中提取药品信息
     * 分别提取药品名称、类别、规格、生产厂家
     *
     * @param text OCR识别的原始文本
     * @return 结构化的药品信息对象
     */
    private MedicineRecognitionResponse.MedicineInfo extractMedicineInfo(String text) {
        String name = extractName(text);
        String category = extractCategory(text);
        String spec = extractSpec(text);
        String manufacturer = extractManufacturer(text);

        return MedicineRecognitionResponse.MedicineInfo.builder()
                .name(name)
                .category(category)
                .spec(spec)
                .manufacturer(manufacturer)
                .build();
    }

    /**
     * 从文本中提取药品名称
     * 采用多策略匹配：先匹配明确的标签（药品名称、药名、名称、商品名、【药名】），
     * 再通过剂型关键词（片、胶囊、颗粒、口服液、注射液、软膏）模糊匹配
     *
     * @param text OCR识别文本
     * @return 提取的药品名称，提取失败返回空字符串
     */
    private String extractName(String text) {
        // 策略1：匹配明确的标签格式
        Pattern[] patterns = {
            NAME_PATTERN_1, NAME_PATTERN_2, NAME_PATTERN_3, NAME_PATTERN_4, NAME_PATTERN_5
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return cleanText(matcher.group(1));
            }
        }

        // 策略2：通过剂型关键词逐行模糊匹配
        String[] lines = text.split("\\n");
        for (String line : lines) {
            line = line.trim();
            // 过滤掉纯数字行和过长过短的行
            if (line.length() > 2 && line.length() < 50 && !isNumeric(line)) {
                // 包含常见剂型关键词则认为是药品名
                if (line.contains("片") || line.contains("胶囊") || line.contains("颗粒") || 
                    line.contains("口服液") || line.contains("注射液") || line.contains("软膏")) {
                    return cleanText(line);
                }
            }
        }

        return "";
    }

    /**
     * 从文本中提取药品类别/剂型
     *
     * @param text OCR识别文本
     * @return 提取的类别，提取失败返回空字符串
     */
    private String extractCategory(String text) {
        Pattern[] patterns = {
            CATEGORY_PATTERN_1, CATEGORY_PATTERN_2, CATEGORY_PATTERN_3, CATEGORY_PATTERN_4
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return cleanText(matcher.group(1));
            }
        }

        return "";
    }

    /**
     * 从文本中提取药品规格
     * 支持多种格式：明确标签格式、每片/每粒/每袋格式、剂量单位格式、包装规格格式
     *
     * @param text OCR识别文本
     * @return 提取的规格，提取失败返回空字符串
     */
    private String extractSpec(String text) {
        Pattern[] patterns = {
            SPEC_PATTERN_1, SPEC_PATTERN_2, SPEC_PATTERN_3, SPEC_PATTERN_4,
            SPEC_PATTERN_5, SPEC_PATTERN_6, SPEC_PATTERN_7
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String result = cleanText(matcher.group(0));
                // 过滤掉过长或过短的结果
                if (result.length() > 1 && result.length() < 60) {
                    return result;
                }
            }
        }

        return "";
    }

    /**
     * 从文本中提取生产厂家
     *
     * @param text OCR识别文本
     * @return 提取的生产厂家，提取失败返回空字符串
     */
    private String extractManufacturer(String text) {
        Pattern[] patterns = {
            MANUFACTURER_PATTERN_1, MANUFACTURER_PATTERN_2, MANUFACTURER_PATTERN_3,
            MANUFACTURER_PATTERN_4, MANUFACTURER_PATTERN_5
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return cleanText(matcher.group(1));
            }
        }

        return "";
    }

    /**
     * 清理文本
     * 去除首尾空白、合并多余空格、移除控制字符、去除方括号
     *
     * @param text 原始文本
     * @return 清理后的文本
     */
    private String cleanText(String text) {
        if (text == null) return "";
        return text.trim()
                .replaceAll("\\s+", " ")        // 合并多个空白字符为一个空格
                .replaceAll("[\\u0000-\\u001f]", "")  // 移除控制字符
                .replace("【", "")
                .replace("】", "");
    }

    /**
     * 判断字符串是否为纯数字（包含小数点、空格、百分号）
     *
     * @param text 待判断的文本
     * @return true-纯数字，false-包含非数字字符
     */
    private boolean isNumeric(String text) {
        return text.matches("^[0-9.\\s%]+$");
    }
}
