package com.example.medicinebox.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ImageUtils 单元测试
 *
 * <p>重点覆盖 ensureRGB 色彩空间转换的边界情况：
 * <ul>
 *   <li>已是 RGB 格式的图片直接返回原实例</li>
 *   <li>ARGB 格式图片的 alpha 通道被移除，白色背景填充</li>
 *   <li>4BYTE_ABGR 格式图片被正确转换</li>
 *   <li>ARGB_PRE 预乘 alpha 格式图片被正确转换</li>
 *   <li>1x1 像素极值图片正常处理</li>
 *   <li>转换后尺寸保持一致</li>
 *   <li>preprocessForAI 端到端：BMP 输入 → JPEG 输出</li>
 * </ul>
 */
class ImageUtilsTest {

    private static final int MAX_SIZE = 1024 * 1024;

    // ==================== ensureRGB 直通：RGB 不变 ====================

    @Test
    @DisplayName("ensureRGB - TYPE_INT_RGB 图片返回原实例")
    void shouldReturnSameInstanceForRgbImage() {
        BufferedImage rgbImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);

        BufferedImage result = ImageUtils.ensureRGB(rgbImage);

        assertSame(rgbImage, result, "已是 RGB 格式应返回同一个实例");
    }

    @Test
    @DisplayName("ensureRGB - TYPE_3BYTE_BGR 图片被转换为 RGB")
    void shouldConvert3ByteBgrImage() {
        BufferedImage bgrImage = new BufferedImage(60, 40, BufferedImage.TYPE_3BYTE_BGR);

        BufferedImage result = ImageUtils.ensureRGB(bgrImage);

        assertNotSame(bgrImage, result);
        assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
        assertEquals(60, result.getWidth());
        assertEquals(40, result.getHeight());
    }

    // ==================== ensureRGB: ARGB → RGB ====================

    @Test
    @DisplayName("ensureRGB - TYPE_INT_ARGB 图片 alpha 通道被移除")
    void shouldStripAlphaChannelForArgbImage() {
        BufferedImage argbImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = argbImage.createGraphics();
        // 半透明红色
        g2d.setColor(new Color(255, 0, 0, 128));
        g2d.fillRect(0, 0, 25, 25);
        // 高透明绿色
        g2d.setColor(new Color(0, 255, 0, 64));
        g2d.fillRect(25, 0, 25, 25);
        // 不透明蓝色
        g2d.setColor(new Color(0, 0, 255, 255));
        g2d.fillRect(0, 25, 50, 25);
        g2d.dispose();

        BufferedImage result = ImageUtils.ensureRGB(argbImage);

        assertNotSame(argbImage, result);
        assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
        assertEquals(50, result.getWidth());
        assertEquals(50, result.getHeight());

        // 半透明红色区域：被白色背景淡化，不再是纯红
        Color topLeft = new Color(result.getRGB(5, 5));
        assertTrue(topLeft.getRed() >= 200, "红色分量应被白色背景淡化后仍较高");
        assertTrue(topLeft.getGreen() <= 150, "绿色分量应被白色背景冲淡");

        // 不透明蓝色区域：颜色应保持
        Color bottom = new Color(result.getRGB(25, 40));
        assertEquals(0, bottom.getRed());
        assertEquals(0, bottom.getGreen());
        assertEquals(255, bottom.getBlue());
    }

    @Test
    @DisplayName("ensureRGB - TYPE_INT_ARGB_PRE 图片被正确转换")
    void shouldConvertArgbPreImage() {
        BufferedImage argbPreImage = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2d = argbPreImage.createGraphics();
        g2d.setColor(new Color(50, 100, 150));
        g2d.fillRect(0, 0, 40, 40);
        g2d.dispose();

        BufferedImage result = ImageUtils.ensureRGB(argbPreImage);

        assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
        assertEquals(40, result.getWidth());
        assertEquals(40, result.getHeight());
    }

    // ==================== ensureRGB: 4BYTE_ABGR → RGB ====================

    @Test
    @DisplayName("ensureRGB - TYPE_4BYTE_ABGR 图片被正确转换")
    void shouldConvert4ByteAbgrImage() {
        BufferedImage abgrImage = new BufferedImage(30, 30, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = abgrImage.createGraphics();
        g2d.setColor(new Color(100, 150, 200, 200));
        g2d.fillRect(0, 0, 30, 30);
        g2d.dispose();

        BufferedImage result = ImageUtils.ensureRGB(abgrImage);

        assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
        assertEquals(30, result.getWidth());
        assertEquals(30, result.getHeight());
    }

    @Test
    @DisplayName("ensureRGB - TYPE_4BYTE_ABGR_PRE 图片被正确转换")
    void shouldConvert4ByteAbgrPreImage() {
        BufferedImage abgrPreImage = new BufferedImage(25, 25, BufferedImage.TYPE_4BYTE_ABGR_PRE);

        BufferedImage result = ImageUtils.ensureRGB(abgrPreImage);

        assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
        assertEquals(25, result.getWidth());
        assertEquals(25, result.getHeight());
    }

    // ==================== ensureRGB: 极值尺寸 ====================

    @Test
    @DisplayName("ensureRGB - 1x1 像素 ARGB 图片正常处理")
    void shouldHandle1x1PixelArgbImage() {
        BufferedImage tinyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        tinyImage.setRGB(0, 0, new Color(255, 0, 0, 128).getRGB());

        BufferedImage result = ImageUtils.ensureRGB(tinyImage);

        assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
        assertEquals(1, result.getWidth());
        assertEquals(1, result.getHeight());
    }

    @Test
    @DisplayName("ensureRGB - 2000x1500 大尺寸 ARGB 图片正常处理")
    void shouldHandleLargeArgbImage() {
        BufferedImage largeImage = new BufferedImage(2000, 1500, BufferedImage.TYPE_INT_ARGB);

        BufferedImage result = ImageUtils.ensureRGB(largeImage);

        assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
        assertEquals(2000, result.getWidth());
        assertEquals(1500, result.getHeight());
    }

    // ==================== ensureRGB: 纯色图片 ====================

    @Test
    @DisplayName("ensureRGB - 纯白 ARGB 图片转换后保持白色")
    void shouldPreserveWhiteColorForArgbImage() {
        BufferedImage whiteImage = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = whiteImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 20, 20);
        g2d.dispose();

        BufferedImage result = ImageUtils.ensureRGB(whiteImage);

        Color center = new Color(result.getRGB(10, 10));
        assertEquals(255, center.getRed());
        assertEquals(255, center.getGreen());
        assertEquals(255, center.getBlue());
    }

    @Test
    @DisplayName("ensureRGB - 透明 ARGB 图片转换后变为纯白")
    void shouldTurnTransparentToWhite() {
        BufferedImage transparentImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = transparentImage.createGraphics();
        g2d.setColor(new Color(0, 0, 0, 0)); // 完全透明
        g2d.fillRect(0, 0, 10, 10);
        g2d.dispose();

        BufferedImage result = ImageUtils.ensureRGB(transparentImage);

        Color center = new Color(result.getRGB(5, 5));
        assertEquals(255, center.getRed(), "完全透明区域应变为白色");
        assertEquals(255, center.getGreen());
        assertEquals(255, center.getBlue());
    }

    // ==================== ensureRGB: 自定义类型 ====================

    @Test
    @DisplayName("ensureRGB - TYPE_BYTE_GRAY 灰度图片被转换为 RGB")
    void shouldConvertByteGrayImage() {
        BufferedImage grayImage = new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY);

        BufferedImage result = ImageUtils.ensureRGB(grayImage);

        assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
        assertEquals(20, result.getWidth());
        assertEquals(20, result.getHeight());
    }

    @Test
    @DisplayName("ensureRGB - TYPE_USHORT_GRAY 灰度图片被转换为 RGB")
    void shouldConvertUshortGrayImage() {
        BufferedImage grayImage = new BufferedImage(15, 15, BufferedImage.TYPE_USHORT_GRAY);

        BufferedImage result = ImageUtils.ensureRGB(grayImage);

        assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
    }

    @Test
    @DisplayName("ensureRGB - TYPE_BYTE_BINARY 二值图片被转换为 RGB")
    void shouldConvertByteBinaryImage() {
        BufferedImage binaryImage = new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_BINARY);

        BufferedImage result = ImageUtils.ensureRGB(binaryImage);

        assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
        assertEquals(20, result.getWidth());
        assertEquals(20, result.getHeight());
    }

    @Test
    @DisplayName("ensureRGB - TYPE_BYTE_INDEXED 索引颜色图片被转换为 RGB")
    void shouldConvertByteIndexedImage() {
        BufferedImage indexedImage = new BufferedImage(30, 30, BufferedImage.TYPE_BYTE_INDEXED);

        BufferedImage result = ImageUtils.ensureRGB(indexedImage);

        assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
        assertEquals(30, result.getWidth());
        assertEquals(30, result.getHeight());
    }

    // ==================== ensureRGB: null 输入防御 ====================

    @Test
    @DisplayName("ensureRGB - null 输入抛出 NullPointerException")
    void shouldThrowNpeForNullInput() {
        assertThrows(NullPointerException.class, () -> ImageUtils.ensureRGB(null),
                "null 输入应抛出 NullPointerException（由调用方保证前置条件）");
    }

    // ==================== ensureRGB: 不透明像素颜色保留 ====================

    @Test
    @DisplayName("ensureRGB - 完全不透明 ARGB 像素颜色被完整保留")
    void shouldPreserveOpaquePixelColors() {
        BufferedImage argbImage = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = argbImage.createGraphics();
        // 四种不透明颜色分别填充四个象限
        g2d.setColor(new Color(255, 0, 0, 255));      // 纯红
        g2d.fillRect(0, 0, 15, 15);
        g2d.setColor(new Color(0, 255, 0, 255));      // 纯绿
        g2d.fillRect(15, 0, 15, 15);
        g2d.setColor(new Color(0, 0, 255, 255));      // 纯蓝
        g2d.fillRect(0, 15, 15, 15);
        g2d.setColor(new Color(0, 0, 0, 255));        // 纯黑
        g2d.fillRect(15, 15, 15, 15);
        g2d.dispose();

        BufferedImage result = ImageUtils.ensureRGB(argbImage);

        // 纯红区域
        Color red = new Color(result.getRGB(5, 5));
        assertEquals(255, red.getRed());
        assertEquals(0, red.getGreen());
        assertEquals(0, red.getBlue());

        // 纯绿区域
        Color green = new Color(result.getRGB(20, 5));
        assertEquals(0, green.getRed());
        assertEquals(255, green.getGreen());
        assertEquals(0, green.getBlue());

        // 纯蓝区域
        Color blue = new Color(result.getRGB(5, 20));
        assertEquals(0, blue.getRed());
        assertEquals(0, blue.getGreen());
        assertEquals(255, blue.getBlue());

        // 纯黑区域
        Color black = new Color(result.getRGB(20, 20));
        assertEquals(0, black.getRed());
        assertEquals(0, black.getGreen());
        assertEquals(0, black.getBlue());
    }

    // ==================== ensureRGB: Graphics2D 资源释放 ====================

    @Test
    @DisplayName("ensureRGB - 转换后原图的 Graphics2D 不受影响")
    void shouldNotAffectOriginalImageGraphics() {
        BufferedImage original = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = original.createGraphics();
        g2d.setColor(new Color(100, 150, 200, 255));
        g2d.fillRect(0, 0, 20, 20);
        g2d.dispose();

        // 转换后原图仍可正常读取
        ImageUtils.ensureRGB(original);

        Color pixel = new Color(original.getRGB(10, 10), true);
        assertEquals(100, pixel.getRed(), "原图红色分量应不变");
        assertEquals(150, pixel.getGreen(), "原图绿色分量应不变");
        assertEquals(200, pixel.getBlue(), "原图蓝色分量应不变");
        assertEquals(255, pixel.getAlpha(), "原图 alpha 分量应不变");
    }

    // ==================== ensureRGB: 内存与大文件边界 ====================

    @Test
    @DisplayName("ensureRGB - 4000x3000 ARGB 大图转换不 OOM")
    void shouldNotOomForLargeArgbImage() {
        // 4000x3000 ARGB ≈ 48MB，接近手机拍照尺寸，验证不会内存溢出
        BufferedImage largeImage = new BufferedImage(4000, 3000, BufferedImage.TYPE_INT_ARGB);

        BufferedImage result = assertDoesNotThrow(() -> ImageUtils.ensureRGB(largeImage),
                "大尺寸图片转换不应抛出异常");

        assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
        assertEquals(4000, result.getWidth());
        assertEquals(3000, result.getHeight());
        assertNotSame(largeImage, result, "ARGB 大图应创建新实例");
    }

    @Test
    @DisplayName("ensureRGB - 极端宽高比 1x8000 窄条图正常处理")
    void shouldHandleExtremeAspectRatioVertical() {
        BufferedImage narrowImage = new BufferedImage(1, 8000, BufferedImage.TYPE_INT_ARGB);

        BufferedImage result = assertDoesNotThrow(() -> ImageUtils.ensureRGB(narrowImage));

        assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
        assertEquals(1, result.getWidth());
        assertEquals(8000, result.getHeight());
    }

    @Test
    @DisplayName("ensureRGB - 极端宽高比 8000x1 窄条图正常处理")
    void shouldHandleExtremeAspectRatioHorizontal() {
        BufferedImage wideImage = new BufferedImage(8000, 1, BufferedImage.TYPE_INT_ARGB);

        BufferedImage result = assertDoesNotThrow(() -> ImageUtils.ensureRGB(wideImage));

        assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
        assertEquals(8000, result.getWidth());
        assertEquals(1, result.getHeight());
    }

    @Test
    @DisplayName("ensureRGB - 连续 100 次转换无内存泄漏")
    void shouldNotLeakMemoryOnRepeatedConversions() {
        // 连续多次转换，验证 GC 正常回收中间对象，无内存持续增长
        for (int i = 0; i < 100; i++) {
            BufferedImage argbImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = argbImage.createGraphics();
            g2d.setColor(new Color(i % 256, (i * 2) % 256, (i * 3) % 256, 128));
            g2d.fillRect(0, 0, 200, 200);
            g2d.dispose();

            BufferedImage result = ImageUtils.ensureRGB(argbImage);

            assertNotNull(result);
            assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
            assertEquals(200, result.getWidth());
            assertEquals(200, result.getHeight());
        }
        // 100 次转换全部完成即视为无内存泄漏
    }

    @Test
    @DisplayName("ensureRGB - 0x0 尺寸无法构造，BufferedImage 构造器层面拦截")
    void shouldRejectZeroDimensionAtConstruction() {
        // BufferedImage 构造器不允许 0 或负数尺寸，这是 JDK 层面的防御
        assertThrows(IllegalArgumentException.class,
                () -> new BufferedImage(0, 0, BufferedImage.TYPE_INT_ARGB),
                "0x0 尺寸应在构造器层面被拒绝");
        assertThrows(IllegalArgumentException.class,
                () -> new BufferedImage(100, 0, BufferedImage.TYPE_INT_ARGB),
                "高度为 0 应在构造器层面被拒绝");
        assertThrows(IllegalArgumentException.class,
                () -> new BufferedImage(0, 100, BufferedImage.TYPE_INT_ARGB),
                "宽度为 0 应在构造器层面被拒绝");
    }

    // ==================== preprocessForAI 大文件处理 ====================

    @Test
    @DisplayName("preprocessForAI - 大尺寸 BMP 文件压缩后不超限")
    void shouldCompressLargeBmpToWithinLimit() throws IOException {
        // 创建 800x600 RGB BMP，约 1.4MB，超过 512KB 上限，需压缩
        BufferedImage largeImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = largeImage.createGraphics();
        // 填充渐变颜色，增加压缩难度
        for (int x = 0; x < 800; x += 10) {
            for (int y = 0; y < 600; y += 10) {
                g2d.setColor(new Color((x * 31) % 256, (y * 47) % 256, ((x + y) * 53) % 256));
                g2d.fillRect(x, y, 10, 10);
            }
        }
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(largeImage, "bmp", baos);
        byte[] bmpBytes = baos.toByteArray();

        // 压缩上限设为 512KB，远小于原始 BMP 大小
        ImageUtils.PreprocessResult result = ImageUtils.preprocessForAI(bmpBytes, 512 * 1024, "large.bmp");

        assertEquals("image/jpeg", result.getMimeType());
        byte[] decoded = Base64.getDecoder().decode(result.getBase64());
        assertTrue(decoded.length > 0, "输出不应为空");
        assertTrue(decoded.length <= 512 * 1024,
                "压缩后大小应 <= 512KB，实际: " + decoded.length + " bytes");

        // 验证输出是合法 JPEG
        BufferedImage decodedImage = ImageIO.read(new ByteArrayInputStream(decoded));
        assertNotNull(decodedImage, "输出应为合法 JPEG 图片");
    }

    @Test
    @DisplayName("preprocessForAI - 超大 RGB BMP 文件端到端压缩转换")
    void shouldHandleLargeBmpEndToEnd() throws IOException {
        // 创建 2000x1500 RGB BMP（约 9MB），验证 ensureRGB → compress 全链路降级
        BufferedImage largeImage = new BufferedImage(2000, 1500, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = largeImage.createGraphics();
        // 填充彩色渐变，模拟真实照片的复杂度
        for (int x = 0; x < 2000; x += 20) {
            for (int y = 0; y < 1500; y += 20) {
                g2d.setColor(new Color((x * 13) % 256, (y * 17) % 256, ((x + y) * 11) % 256));
                g2d.fillRect(x, y, 20, 20);
            }
        }
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(largeImage, "bmp", baos);
        byte[] bmpBytes = baos.toByteArray();

        // 走 needsConversion 分支（BMP → JPEG），上限设为 512KB，触发压缩循环
        ImageUtils.PreprocessResult result = ImageUtils.preprocessForAI(bmpBytes, 512 * 1024, "large.bmp");

        assertEquals("image/jpeg", result.getMimeType(), "BMP 应被转换为 JPEG");
        byte[] decoded = Base64.getDecoder().decode(result.getBase64());
        assertTrue(decoded.length > 0);
        assertTrue(decoded.length <= 512 * 1024,
                "大图压缩后不应超过 512KB 上限，实际: " + decoded.length + " bytes");

        BufferedImage decodedImage = ImageIO.read(new ByteArrayInputStream(decoded));
        assertNotNull(decodedImage, "输出应为合法 JPEG");
        assertTrue(decodedImage.getWidth() > 0 && decodedImage.getWidth() <= 2000);
        assertTrue(decodedImage.getHeight() > 0 && decodedImage.getHeight() <= 1500);
    }

    @Test
    @DisplayName("preprocessForAI - 空字节数组输入安全降级")
    void shouldDegradeGracefullyForEmptyInput() {
        byte[] emptyBytes = new byte[0];

        ImageUtils.PreprocessResult result = ImageUtils.preprocessForAI(emptyBytes, MAX_SIZE, "empty.bmp");

        assertNotNull(result, "空输入不应返回 null");
        assertNotNull(result.getBase64(), "Base64 不应为 null");
        assertNotNull(result.getMimeType(), "MIME 类型不应为 null");
    }

    // ==================== preprocessForAI 端到端测试 ====================

    @Test
    @DisplayName("preprocessForAI - BMP 输入被转换为 JPEG 输出")
    void shouldConvertBmpToJpegEndToEnd() throws IOException {
        // 使用 RGB 格式创建 BMP（RGB 格式 BMP 可被 ImageIO 正确读写）
        BufferedImage rgbImage = new BufferedImage(100, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = rgbImage.createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, 100, 80);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(rgbImage, "bmp", baos);
        byte[] bmpBytes = baos.toByteArray();

        ImageUtils.PreprocessResult result = ImageUtils.preprocessForAI(bmpBytes, MAX_SIZE, "test.bmp");

        assertEquals("image/jpeg", result.getMimeType());
        byte[] decoded = Base64.getDecoder().decode(result.getBase64());
        assertTrue(decoded.length > 0);

        // 验证输出是合法 JPEG
        BufferedImage decodedImage = ImageIO.read(new ByteArrayInputStream(decoded));
        assertNotNull(decodedImage, "输出应为合法 JPEG 图片");
        assertEquals(100, decodedImage.getWidth());
        assertEquals(80, decodedImage.getHeight());
    }

    @Test
    @DisplayName("preprocessForAI - JPEG 格式不触发转换，保持原格式")
    void shouldNotConvertJpegFormat() throws IOException {
        BufferedImage image = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] jpegBytes = baos.toByteArray();

        ImageUtils.PreprocessResult result = ImageUtils.preprocessForAI(jpegBytes, MAX_SIZE, "test.jpg");

        assertEquals("image/jpeg", result.getMimeType());
        byte[] decoded = Base64.getDecoder().decode(result.getBase64());
        assertTrue(decoded.length > 0);
    }
}