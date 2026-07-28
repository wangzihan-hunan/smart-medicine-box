package com.example.medicinebox.dto.request;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备指令创建请求DTO
 * 用于接收创建设备控制指令时提交的指令信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandCreateRequest {

    // 指令动作
    @NotBlank(message = "指令动作不能为空")
    private String action;

    // 设备编号
    @NotBlank(message = "设备编号不能为空")
    private String deviceId;

    // 指令参数（JSON格式字符串）
    private String params;
}
