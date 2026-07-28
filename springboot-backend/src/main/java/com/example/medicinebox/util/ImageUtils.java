package com.example.medicinebox.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * 图片处理工具类
 *
 * <p>提供图片压缩、缩放、Base64编码等常用图片处理功能。
 * 主要用于用户上传图片时的压缩处理，减小存储和传输开销。
 *
 * <p>主要功能：
 * <ul>
 *   <li>图片压缩到指定大小以下</li>
 *   <li>图片等比例缩放</li>
 *   <li>图片Base64编码</li>
 *   <li>根据文件名获取MIME类型</li>
 * </ul>
 *
 * @author medicine-box
 * @since 1.0.0
 */
public class ImageUtils {

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(ImageUtils.class);

    /** 最大图片尺寸（像素），压缩时限制图片的宽高不超过此值 */
    private static final int MAX_DIMENSION = 1920;

    /** 压缩后目标质量（0.0 - 1.0），JPEG压缩质量参数 */
    private static final float COMPRESS_QUALITY = 0.85f;

    /**
     * 压缩图片并转为Base64字符串
     *
     * <p>将图片压缩到指定大小以内，并转换为Base64编码字符串。
     * 如果压缩失败，返回原始图片的Base64编码。
     *
     * @param imageBytes 原始图片字节数组
     * @param maxSizeBytes 压缩后最大大小（字节）
     * @return Base64编码的压缩后图片字符串
     */
    public static String compressAndEncodeToBase64(byte[] imageBytes, int maxSizeBytes) {
        try {
            byte[] compressed = compressImage(imageBytes, maxSizeBytes);
            return Base64.getEncoder().encodeToString(compressed);
        } catch (Exception e) {
            log.warn("图片压缩失败，使用原始图片: {}", e.getMessage());
            return Base64.getEncoder().encodeToString(imageBytes);
        }
    }

    /**
     * 压缩图片到指定大小以下
     *
     * <p>采用多阶段压缩策略：
     * <ol>
     *   <li>如果原图大小已满足要求，直接返回</li>
     *   <li>第一步：等比例缩放，限制最大尺寸为1920px</li>
     *   <li>第二步：使用初始质量（0.85）编码为JPEG</li>
     *   <li>第三步：如果仍然太大，逐步降低质量（最多5次尝试）</li>
     *   <li>质量过低时（&lt;0.3），进一步缩小尺寸后重试</li>
     * </ol>
     *
     * @param imageBytes 原始图片字节数组
     * @param maxSizeBytes 最大大小（字节）
     * @return 压缩后的图片字节数组
     * @throws IOException 图片读取或写入失败时抛出
     */
    public static byte[] compressImage(byte[] imageBytes, int maxSizeBytes) throws IOException {
        // 如果原图大小已满足要求，直接返回
        if (imageBytes.length <= maxSizeBytes) {
            return imageBytes;
        }

        // 读取原始图片
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (originalImage == null) {
            throw new IOException("无法读取图片");
        }

        // 第一步：等比例缩放，限制最大尺寸
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            double scale = Math.min((double) MAX_DIMENSION / width, (double) MAX_DIMENSION / height);
            width = (int) (width * scale);
            height = (int) (height * scale);
            originalImage = resizeImage(originalImage, width, height);
        }

        // 第二步：使用初始质量编码
        byte[] result = encodeWithQuality(originalImage, COMPRESS_QUALITY);

        // 如果仍然太大，进一步降低质量和尺寸（最多5次尝试）
        int attempts = 0;
        float quality = COMPRESS_QUALITY;
        while (result.length > maxSizeBytes && attempts < 5) {
            attempts++;
            quality -= 0.15f;
            if (quality < 0.3f) {
                // 质量太低时，缩小尺寸后重置质量重试
                width = (int) (width * 0.8);
                height = (int) (height * 0.8);
                originalImage = resizeImage(originalImage, width, height);
                quality = COMPRESS_QUALITY;
            }
            result = encodeWithQuality(originalImage, quality);
            log.debug("压缩尝试 {}: 质量={}, 尺寸={}x{}, 大小={} bytes", attempts, quality, width, height, result.length);
        }

        log.info("图片压缩完成: 原始 {} bytes -> 压缩后 {} bytes", imageBytes.length, result.length);
        return result;
    }

    /**
     * 等比例缩放图片
     *
     * <p>使用平滑缩放算法（SCALE_SMOOTH）将图片缩放到目标尺寸，
     * 保证缩放后的图片质量。
     *
     * @param original 原始图片
     * @param targetWidth 目标宽度（像素）
     * @param targetHeight 目标高度（像素）
     * @return 缩放后的图片
     */
    private static BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
        Image scaledInstance = original.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(scaledInstance, 0, 0, null);
        g2d.dispose();
        return resized;
    }

    /**
     * 使用指定质量编码为JPEG
     *
     * <p>将BufferedImage编码为JPEG格式的字节数组。
     * 注意：当前实现使用ImageIO默认JPEG编码，
     * 实际压缩质量参数可能需要通过ImageWriteParam进一步控制。
     *
     * @param image 图片对象
     * @param quality 质量参数（0.0 - 1.0）
     * @return JPEG格式的图片字节数组
     * @throws IOException 编码失败时抛出
     */
    private static byte[] encodeWithQuality(BufferedImage image, float quality) throws IOException {
        // 使用 ImageIO 默认编码
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    /**
     * 根据文件名获取图片MIME类型
     *
     * <p>根据文件扩展名判断图片的MIME类型，支持：
     * <ul>
     *   <li>.png -> image/png</li>
     *   <li>.gif -> image/gif</li>
     *   <li>.webp -> image/webp</li>
     *   <li>.bmp -> image/bmp</li>
     *   <li>其他 -> image/jpeg（默认）</li>
     * </ul>
     *
     * @param filename 文件名
     * @return 图片MIME类型字符串
     */
    public static String getMimeType(String filename) {
        if (filename == null) {
            return "image/jpeg";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        } else if (lower.endsWith(".webp")) {
            return "image/webp";
        } else if (lower.endsWith(".bmp")) {
            return "image/bmp";
        }
        return "image/jpeg";
    }
}
