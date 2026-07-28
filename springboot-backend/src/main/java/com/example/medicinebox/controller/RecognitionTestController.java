package com.example.medicinebox.controller;

import com.example.medicinebox.dto.response.BaseResponse;
import com.example.medicinebox.entity.RecognitionTestRecord;
import com.example.medicinebox.service.RecognitionTestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Recognition test data controller
 * For challenge cup experiment data collection and analysis
 */
@RestController
@RequestMapping("/api/v1/tests/recognition")
public class RecognitionTestController {

    private static final Logger log = LoggerFactory.getLogger(RecognitionTestController.class);

    private final RecognitionTestService testService;

    public RecognitionTestController(RecognitionTestService testService) {
        this.testService = testService;
    }

    /**
     * Get statistics for a batch
     */
    @GetMapping("/statistics")
    public BaseResponse<Map<String, Object>> getStatistics(@RequestParam("batchId") String batchId) {
        Map<String, Object> stats = testService.getBatchStatistics(batchId);
        return BaseResponse.success("查询成功", stats);
    }

    /**
     * Get all batch IDs
     */
    @GetMapping("/batches")
    public BaseResponse<List<String>> getBatchIds() {
        List<String> batchIds = testService.getAllBatchIds();
        return BaseResponse.success("查询成功", batchIds);
    }

    /**
     * Get records for a batch
     */
    @GetMapping("/records")
    public BaseResponse<List<RecognitionTestRecord>> getRecords(@RequestParam("batchId") String batchId) {
        List<RecognitionTestRecord> records = testService.getRecordsByBatch(batchId);
        return BaseResponse.success("查询成功", records);
    }

    /**
     * Clear records for a batch
     */
    @DeleteMapping("/records")
    public BaseResponse<Void> clearRecords(@RequestParam("batchId") String batchId) {
        testService.clearBatch(batchId);
        return BaseResponse.success("清除成功");
    }
}
