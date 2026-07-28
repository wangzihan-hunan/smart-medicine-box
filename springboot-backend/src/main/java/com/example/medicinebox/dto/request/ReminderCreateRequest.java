package com.example.medicinebox.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 用药提醒创建请求DTO
 * 用于接收创建用药提醒时提交的提醒信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderCreateRequest {

    // 关联药品ID
    private Integer medicineId;

    // 药品名称
    @NotBlank(message = "药品名称不能为空")
    private String medicineName;

    // 关联处方ID
    private Integer prescriptionId;

    // 提醒时间
    @NotBlank(message = "提醒时间不能为空")
    private String time;

    // 用药剂量
    @NotBlank(message = "用药剂量不能为空")
    private String dosage;

    // 用药频次（默认值：每日一次）
    private String frequency = "每日一次";

    // 是否重复提醒
    @NotNull(message = "是否重复不能为空")
    private Boolean isRepeat;

    // 备注信息
    private String remark;

    // 提醒开始日期
    private LocalDate startDate;

    // 提醒结束日期
    private LocalDate endDate;

    // 重复结束类型：never(永不结束)、endDate(结束于日期)、count(重复N次)
    private String repeatEndType = "never";

    // 重复结束日期（repeatEndType=endDate时使用）
    private LocalDate repeatEndDate;

    // 重复次数（repeatEndType=count时使用）
    private Integer repeatCount = 0;

    // 提前通知时间（分钟），0表示准时通知
    private Integer advanceNotice = 0;

    // 提醒音效：default/gentle/urgent/melody/silent
    private String soundEffect = "default";
}
