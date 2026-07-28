package com.example.medicinebox.dto.response;

import com.example.medicinebox.entity.Command;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备指令详情响应DTO
 * 用于返回单个设备控制指令的详细信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandResponse {
    // 指令ID
    private Integer id;
    // 所属用户ID
    private Integer userId;
    // 设备编号
    private String deviceId;
    // 指令动作
    private String action;
    // 指令参数（JSON格式字符串）
    private String params;
    // 指令发送时间
    private String time;
    // 指令状态
    private String status;

    /**
     * 从实体对象转换为响应DTO
     * @param command 指令实体
     * @return 指令响应DTO
     */
    public static CommandResponse fromEntity(Command command) {
        CommandResponse response = new CommandResponse();
        response.setId(command.getId());
        response.setUserId(command.getUserId());
        response.setDeviceId(command.getDeviceId());
        response.setAction(command.getAction());
        response.setParams(command.getParams());
        response.setTime(command.getTime());
        response.setStatus(command.getStatus());
        return response;
    }
}
