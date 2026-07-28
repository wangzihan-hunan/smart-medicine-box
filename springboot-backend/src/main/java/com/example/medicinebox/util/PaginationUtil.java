package com.example.medicinebox.util;

/**
 * 分页参数校验工具类
 *
 * <p>提供分页参数（page、size）的统一校验和修正方法，
 * 防止前端传入非法参数导致数据库查询异常或内存溢出。
 */
public final class PaginationUtil {

    // 默认页码
    public static final int DEFAULT_PAGE = 0;
    // 默认每页大小
    public static final int DEFAULT_SIZE = 20;
    // 最大每页大小（防止恶意请求拉取全表数据）
    public static final int MAX_SIZE = 100;

    private PaginationUtil() {
        // 工具类禁止实例化
    }

    /**
     * 校验并修正页码
     *
     * @param page 原始页码
     * @return 修正后的页码（小于0时返回0）
     */
    public static int validatePage(int page) {
        return Math.max(page, DEFAULT_PAGE);
    }

    /**
     * 校验并修正每页大小
     *
     * @param size 原始每页大小
     * @return 修正后的每页大小（小于1时返回默认值，大于最大值时返回最大值）
     */
    public static int validateSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
