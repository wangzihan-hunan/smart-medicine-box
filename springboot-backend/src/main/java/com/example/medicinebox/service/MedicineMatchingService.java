package com.example.medicinebox.service;

import com.example.medicinebox.dto.response.MedicineRecognitionResponse;
import com.example.medicinebox.entity.Medicine;
import com.example.medicinebox.repository.MedicineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 药品智能匹配服务类
 * 核心功能：将OCR/AI识别的药品信息与数据库中已有药品进行模糊匹配，
 * 使用 Jaro-Winkler 距离算法计算字符串相似度，返回匹配结果供用户确认入库
 */
@Service
public class MedicineMatchingService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(MedicineMatchingService.class);

    private final MedicineRepository medicineRepository;

    // Jaro-Winkler 匹配阈值：相似度 >= 此值视为匹配成功
    private static final double MATCH_THRESHOLD = 0.75;
    // 高置信度阈值：相似度 >= 此值视为高置信度匹配
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.90;

    public MedicineMatchingService(MedicineRepository medicineRepository,
                                   com.example.medicinebox.repository.UserRepository userRepository) {
        super(userRepository);
        this.medicineRepository = medicineRepository;
    }

    /**
     * 将识别结果中的药品列表与数据库进行匹配
     *
     * @param recognitionResult 识别结果
     * @return 匹配结果列表，每项包含识别信息和匹配到的数据库药品
     */
    public List<MedicineMatchResult> matchRecognitionResults(MedicineRecognitionResponse recognitionResult) {
        List<MedicineMatchResult> results = new ArrayList<>();

        if (recognitionResult.getMedicines() == null || recognitionResult.getMedicines().isEmpty()) {
            return results;
        }

        Integer userId = getCurrentUser().getId();
        List<Medicine> userMedicines = medicineRepository.findByUserId(userId);

        for (MedicineRecognitionResponse.MedicineInfo info : recognitionResult.getMedicines()) {
            MedicineMatchResult matchResult = matchSingle(info, userMedicines);
            results.add(matchResult);
        }

        return results;
    }

    /**
     * 匹配单个药品
     */
    private MedicineMatchResult matchSingle(MedicineRecognitionResponse.MedicineInfo info,
                                            List<Medicine> userMedicines) {
        if (info.getName() == null || info.getName().trim().isEmpty()) {
            return MedicineMatchResult.unmatched(info);
        }

        String recognizedName = info.getName().trim();
        Medicine bestMatch = null;
        double bestScore = 0;

        for (Medicine medicine : userMedicines) {
            double score = calculateSimilarity(recognizedName, medicine.getName());
            if (score > bestScore) {
                bestScore = score;
                bestMatch = medicine;
            }
        }

        if (bestScore >= MATCH_THRESHOLD && bestMatch != null) {
            return MedicineMatchResult.matched(info, bestMatch, bestScore);
        }

        return MedicineMatchResult.unmatched(info);
    }

    /**
     * 计算两个字符串的 Jaro-Winkler 相似度
     * 返回值范围 [0, 1]，1表示完全相同
     */
    public double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0;
        if (s1.equals(s2)) return 1.0;

        String a = s1.toLowerCase();
        String b = s2.toLowerCase();

        double jaroDistance = jaroDistance(a, b);
        // Winkler 修正：前缀匹配加分
        int prefixLength = commonPrefixLength(a, b, 4);
        return jaroDistance + (prefixLength * 0.1 * (1 - jaroDistance));
    }

    /**
     * Jaro 距离算法
     */
    private double jaroDistance(String s1, String s2) {
        if (s1.isEmpty() && s2.isEmpty()) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        int matchDistance = Math.max(s1.length(), s2.length()) / 2 - 1;

        boolean[] s1Matches = new boolean[s1.length()];
        boolean[] s2Matches = new boolean[s2.length()];

        int matches = 0;
        for (int i = 0; i < s1.length(); i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, s2.length());
            for (int j = start; j < end; j++) {
                if (s2Matches[j]) continue;
                if (s1.charAt(i) != s2.charAt(j)) continue;
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) return 0.0;

        // 计算换位数量
        double transpositions = 0;
        int k = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (!s1Matches[i]) continue;
            while (!s2Matches[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) transpositions++;
            k++;
        }
        transpositions /= 2.0;

        return (matches / (double) s1.length() +
                matches / (double) s2.length() +
                (matches - transpositions) / matches) / 3.0;
    }

    /**
     * 计算公共前缀长度（最多 maxLength 个字符）
     */
    private int commonPrefixLength(String s1, String s2, int maxLength) {
        int minLength = Math.min(Math.min(s1.length(), s2.length()), maxLength);
        for (int i = 0; i < minLength; i++) {
            if (s1.charAt(i) != s2.charAt(i)) return i;
        }
        return minLength;
    }

    /**
     * 批量添加入库
     * 将匹配确认后的药品批量保存到数据库
     *
     * @param items 待入库的药品列表（每项包含药品信息和是否匹配到现有药品）
     * @return 入库结果
     */
    public BatchAddResult batchAddMedicines(List<BatchAddItem> items) {
        Integer userId = getCurrentUser().getId();
        int newCount = 0;
        int updateCount = 0;
        int skipCount = 0;
        List<Medicine> addedMedicines = new ArrayList<>();

        for (BatchAddItem item : items) {
            if (item.isSkip()) {
                skipCount++;
                continue;
            }

            if (item.getMatchedMedicineId() != null && item.getMatchedMedicineId() > 0) {
                // 更新已有药品库存
                Medicine existing = medicineRepository.findByIdAndUserId(item.getMatchedMedicineId(), userId);
                if (existing != null) {
                    int addStock = item.getQuantity() != null ? item.getQuantity() : 1;
                    existing.setStock(existing.getStock() + addStock);
                    if (item.getSpec() != null && !item.getSpec().isEmpty()) {
                        existing.setSpec(item.getSpec());
                    }
                    medicineRepository.save(existing);
                    updateCount++;
                    addedMedicines.add(existing);
                }
            } else {
                // 新增药品
                Medicine medicine = new Medicine();
                medicine.setName(item.getName());
                medicine.setSpec(item.getSpec());
                medicine.setCategory(item.getCategory());
                medicine.setManufacturer(item.getManufacturer());
                medicine.setStock(item.getQuantity() != null ? item.getQuantity() : 1);
                medicine.setUserId(userId);
                medicine = medicineRepository.save(medicine);
                newCount++;
                addedMedicines.add(medicine);
            }
        }

        log.info("批量入库完成, 用户ID: {}, 新增: {}, 更新: {}, 跳过: {}", userId, newCount, updateCount, skipCount);

        return new BatchAddResult(newCount, updateCount, skipCount, addedMedicines);
    }

    // ==================== 内部类 ====================

    /**
     * 单个药品匹配结果
     */
    public static class MedicineMatchResult {
        private MedicineRecognitionResponse.MedicineInfo recognizedInfo;
        private Medicine matchedMedicine;
        private double similarity;
        private boolean matched;
        private String confidence; // HIGH / MEDIUM / NONE

        private MedicineMatchResult() {}

        static MedicineMatchResult matched(MedicineRecognitionResponse.MedicineInfo info,
                                           Medicine medicine, double similarity) {
            MedicineMatchResult result = new MedicineMatchResult();
            result.recognizedInfo = info;
            result.matchedMedicine = medicine;
            result.similarity = similarity;
            result.matched = true;
            result.confidence = similarity >= HIGH_CONFIDENCE_THRESHOLD ? "HIGH" : "MEDIUM";
            return result;
        }

        static MedicineMatchResult unmatched(MedicineRecognitionResponse.MedicineInfo info) {
            MedicineMatchResult result = new MedicineMatchResult();
            result.recognizedInfo = info;
            result.matched = false;
            result.similarity = 0;
            result.confidence = "NONE";
            return result;
        }

        public MedicineRecognitionResponse.MedicineInfo getRecognizedInfo() { return recognizedInfo; }
        public Medicine getMatchedMedicine() { return matchedMedicine; }
        public double getSimilarity() { return similarity; }
        public boolean isMatched() { return matched; }
        public String getConfidence() { return confidence; }
    }

    /**
     * 批量入库请求项
     */
    public static class BatchAddItem {
        private String name;
        private String spec;
        private String category;
        private String manufacturer;
        private Integer quantity;
        private Integer matchedMedicineId;
        private boolean skip;

        public BatchAddItem() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSpec() { return spec; }
        public void setSpec(String spec) { this.spec = spec; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getManufacturer() { return manufacturer; }
        public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Integer getMatchedMedicineId() { return matchedMedicineId; }
        public void setMatchedMedicineId(Integer matchedMedicineId) { this.matchedMedicineId = matchedMedicineId; }
        public boolean isSkip() { return skip; }
        public void setSkip(boolean skip) { this.skip = skip; }
    }

    /**
     * 批量入库结果
     */
    public static class BatchAddResult {
        private int newCount;
        private int updateCount;
        private int skipCount;
        private List<Medicine> addedMedicines;

        public BatchAddResult(int newCount, int updateCount, int skipCount, List<Medicine> addedMedicines) {
            this.newCount = newCount;
            this.updateCount = updateCount;
            this.skipCount = skipCount;
            this.addedMedicines = addedMedicines;
        }

        public int getNewCount() { return newCount; }
        public int getUpdateCount() { return updateCount; }
        public int getSkipCount() { return skipCount; }
        public List<Medicine> getAddedMedicines() { return addedMedicines; }
        public int getTotalCount() { return newCount + updateCount + skipCount; }
    }
}