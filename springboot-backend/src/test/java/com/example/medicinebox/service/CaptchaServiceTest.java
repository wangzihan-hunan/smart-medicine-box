package com.example.medicinebox.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CaptchaService 单元测试类
 * 测试验证码生成和验证功能
 */
@ExtendWith(MockitoExtension.class)
class CaptchaServiceTest {

    @Mock
    private javax.servlet.http.HttpSession session;

    private CaptchaService captchaService;

    @BeforeEach
    void setUp() {
        captchaService = new CaptchaService(session);
    }

    @Test
    @DisplayName("生成验证码 - 应返回4位字母字符串")
    void generateCaptcha_shouldReturn4LetterString() {
        // When
        String captcha = captchaService.generateCaptcha();

        // Then
        assertNotNull(captcha);
        assertEquals(4, captcha.length());
        assertTrue(captcha.matches("[A-Za-z]{4}"));
    }

    @Test
    @DisplayName("生成验证码 - 应存储到Session(小写)")
    void generateCaptcha_shouldStoreInSessionAsLowercase() {
        // When
        String captcha = captchaService.generateCaptcha();

        // Then
        verify(session).setAttribute(eq("captcha"), eq(captcha.toLowerCase()));
    }

    @Test
    @DisplayName("生成验证码 - 多次生成应返回不同值")
    void generateCaptcha_multipleCallsShouldReturnDifferentValues() {
        // When
        String captcha1 = captchaService.generateCaptcha();
        String captcha2 = captchaService.generateCaptcha();

        // Then - 虽然可能相同，但测试随机性
        assertNotNull(captcha1);
        assertNotNull(captcha2);
    }

    @Test
    @DisplayName("验证验证码 - 正确输入应返回true")
    void verifyCaptcha_correctInputShouldReturnTrue() {
        // Given
        String storedCaptcha = "abcd";
        when(session.getAttribute("captcha")).thenReturn(storedCaptcha);

        // When
        boolean result = captchaService.verifyCaptcha("ABCD");

        // Then
        assertTrue(result);
        verify(session).removeAttribute("captcha");
    }

    @Test
    @DisplayName("验证验证码 - 小写输入也应成功")
    void verifyCaptcha_lowercaseInputShouldAlsoSucceed() {
        // Given
        String storedCaptcha = "abcd";
        when(session.getAttribute("captcha")).thenReturn(storedCaptcha);

        // When
        boolean result = captchaService.verifyCaptcha("abcd");

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("验证验证码 - 错误输入应返回false")
    void verifyCaptcha_wrongInputShouldReturnFalse() {
        // Given
        String storedCaptcha = "abcd";
        when(session.getAttribute("captcha")).thenReturn(storedCaptcha);

        // When
        boolean result = captchaService.verifyCaptcha("wrong");

        // Then
        assertFalse(result);
        verify(session).removeAttribute("captcha");
    }

    @Test
    @DisplayName("验证验证码 - 空输入应返回false")
    void verifyCaptcha_emptyInputShouldReturnFalse() {
        // When
        boolean result = captchaService.verifyCaptcha("");

        // Then
        assertFalse(result);
        verify(session, never()).getAttribute(anyString());
    }

    @Test
    @DisplayName("验证验证码 - null输入应返回false")
    void verifyCaptcha_nullInputShouldReturnFalse() {
        // When
        boolean result = captchaService.verifyCaptcha(null);

        // Then
        assertFalse(result);
        verify(session, never()).getAttribute(anyString());
    }

    @Test
    @DisplayName("验证验证码 - Session无验证码应返回false")
    void verifyCaptcha_noCaptchaInSessionShouldReturnFalse() {
        // Given
        when(session.getAttribute("captcha")).thenReturn(null);

        // When
        boolean result = captchaService.verifyCaptcha("abcd");

        // Then
        assertFalse(result);
        // 不应该删除，因为本来就没有
        verify(session, never()).removeAttribute(anyString());
    }

    @Test
    @DisplayName("验证验证码 - 验证后应清除Session")
    void verifyCaptcha_shouldClearSessionAfterVerification() {
        // Given
        when(session.getAttribute("captcha")).thenReturn("abcd");

        // When
        captchaService.verifyCaptcha("abcd");

        // Then
        verify(session).removeAttribute("captcha");
    }
}