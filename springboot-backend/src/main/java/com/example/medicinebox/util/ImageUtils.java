package com.example.medicinebox.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;

/**
 * 图片处理工具类
 *
 * <p>提供图片压缩、缩放、格式转换、Base64编码等常用图片处理功能。
 * 集成 TwelveMonkeys ImageIO 插件，支持 WebP/BMP/TIFF 等扩展格式的读取，
 * 并统一转换为 JPEG 格式以确保 AI API 兼容性。
 *
 * @author medicine-box
 * @since 1.0.0
 */
public class ImageUtils {

    private static final Logger log = LoggerFactory.getLogger(ImageUtils.class);

    /** 最大图片尺寸（像素） */
    private static final int MAX_DIMENSION = 1920;
    /** 压缩后目标质量（0.0 - 1.0） */
    private static final float COMPRESS_QUALITY = 0.85f;
    /** AI API 兼容的输出格式（JPEG 兼容性最好） */
    private static final String AI_OUTPUT_FORMAT = "jpg";
    /** 需要转换为 JPEG 的格式（这些格式的 AI API 兼容性不确定） */
    private static final String[] AI_UNSAFE_FORMATS = {"webp", "bmp", "tiff", "tif"};

    /**
     * 预处理图片并转为 Base64（用于 AI 识别）
     *
     * <p>读取任意格式图片（依赖 TwelveMonkeys ImageIO），统一转换为 JPEG 格式，
     * 压缩到指定大小以内，返回 Base64 编码和对应的 MIME 类型。
     *
     * @param imageBytes 原始图片字节数组
     * @param maxSizeBytes 压缩后最大大小（字节）
     * @param originalFilename 原始文件名，用于判断格式
     * @return PreprocessResult 包含 Base64 字符串和 MIME 类型
     */
    public static PreprocessResult preprocessForAI(byte[] imageBytes, int maxSizeBytes, String originalFilename) {
        String mimeType = getMimeType(originalFilename);
        boolean needsConversion = needsConversion(mimeType);

        try {
            byte[] processed;
            if (needsConversion) {
                // 非 JPEG/PNG 格式：先读取为 BufferedImage，再编码为 JPEG
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
                if (image == null) {
                    log.warn("无法读取图片格式: {}, 使用原始字节", mimeType);
                    return new PreprocessResult(
                            Base64.getEncoder().encodeToString(imageBytes), mimeType);
                }
                // 确保图片为 RGB 格式（JPEG 不支持 alpha 通道，需移除透明层）
                image = ensureRGB(image);
                processed = compressBufferedImage(image, maxSizeBytes);
                log.info("图片格式转换: {} -> JPEG, 原始 {} bytes, 处理后 {} bytes",
                        mimeType, imageBytes.length, processed.length);
                return new PreprocessResult(
                        Base64.getEncoder().encodeToString(processed), "image/jpeg");
            } else {
                // JPEG/PNG/GIF：直接压缩
                processed = compressImage(imageBytes, maxSizeBytes);
                return new PreprocessResult(
                        Base64.getEncoder().encodeToString(processed), mimeType);
            }
        } catch (Exception e) {
            log.warn("图片预处理失败，使用原始字节: {}", e.getMessage());
            return new PreprocessResult(
                    Base64.getEncoder().encodeToString(imageBytes), mimeType);
        }
    }

    /**
     * 确保 BufferedImage 为 RGB 颜色空间（无 alpha 通道）
     *
     * <p>JPEG 格式不支持透明通道，如果图片包含 alpha 通道（如 WebP 转码后的 ARGB 格式），
     * 直接编码为 JPEG 会导致图片数据损坏，API 无法解码。此方法在编码前将图片转换为标准 RGB 格式。
     */
    static BufferedImage ensureRGB(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }
        // 创建 RGB 格式的新图片，用白色背景填充（移除透明层）
        BufferedImage rgbImage = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = rgbImage.createGraphics();
        g2d.setColor(java.awt.Color.WHITE);
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        log.debug("图片色彩空间转换: type={} -> TYPE_INT_RGB", image.getType());
        return rgbImage;
    }

    /**
     * 判断 MIME 类型是否需要转换为 JPEG
     */
    private static boolean needsConversion(String mimeType) {
        if (mimeType == null) return true;
        String lower = mimeType.toLowerCase();
        for (String format : AI_UNSAFE_FORMATS) {
            if (lower.contains(format)) return true;
        }
        return false;
    }

    /**
     * 压缩图片并转为 Base64 字符串（兼容旧接口）
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
     * 压缩 BufferedImage 到指定大小以下
     */
    private static byte[] compressBufferedImage(BufferedImage image, int maxSizeBytes) throws IOException {
        int width = image.getWidth();
        int height = image.getHeight();

        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            double scale = Math.min((double) MAX_DIMENSION / width, (double) MAX_DIMENSION / height);
            width = (int) (width * scale);
            height = (int) (height * scale);
            image = resizeImage(image, width, height);
        }

        byte[] result = encodeAsJpeg(image, COMPRESS_QUALITY);

        int attempts = 0;
        float quality = COMPRESS_QUALITY;
        while (result.length > maxSizeBytes && attempts < 5) {
            attempts++;
            quality -= 0.15f;
            if (quality < 0.3f) {
                width = (int) (width * 0.8);
                height = (int) (height * 0.8);
                image = resizeImage(image, width, height);
                quality = COMPRESS_QUALITY;
            }
            result = encodeAsJpeg(image, quality);
        }

        return result;
    }

    /**
     * 压缩图片到指定大小以下（兼容旧接口）
     */
    public static byte[] compressImage(byte[] imageBytes, int maxSizeBytes) throws IOException {
        if (imageBytes.length <= maxSizeBytes) {
            return imageBytes;
        }

        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (originalImage == null) {
            throw new IOException("无法读取图片，格式不受支持");
        }

        return compressBufferedImage(originalImage, maxSizeBytes);
    }

    /**
     * 等比例缩放图片
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
     * 使用指定质量编码为 JPEG（通过 ImageWriteParam 精确控制质量）
     */
    private static byte[] encodeAsJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            // 回退：使用默认 JPEG 编码
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            return baos.toByteArray();
        }

        ImageWriter writer = writers.next();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    /**
     * 根据文件名获取图片 MIME 类型
     */
    public static String getMimeType(String filename) {
        if (filename == null) {
            return "image/jpeg";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".tiff") || lower.endsWith(".tif")) return "image/tiff";
        return "image/jpeg";
    }

    /**
     * 图片预处理结果
     */
    public static class PreprocessResult {
        private final String base64;
        private final String mimeType;

        public PreprocessResult(String base64, String mimeType) {
            this.base64 = base64;
            this.mimeType = mimeType;
        }

        public String getBase64() { return base64; }
        public String getMimeType() { return mimeType; }
    }
}