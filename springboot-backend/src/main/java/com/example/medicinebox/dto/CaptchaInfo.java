package com.example.medicinebox.dto;

import java.io.Serializable;

/**
 * 验证码信息 DTO
 * 用于 Redis 存储验证码数据，包含验证码、过期时间和发送时间
 */
public class CaptchaInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String captcha;
    private long expireTime;
    private long sendTime;

    public CaptchaInfo() {
    }

    public CaptchaInfo(String captcha, long expireTime, long sendTime) {
        this.captcha = captcha;
        this.expireTime = expireTime;
        this.sendTime = sendTime;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public long getSendTime() {
        return sendTime;
    }

    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }
}