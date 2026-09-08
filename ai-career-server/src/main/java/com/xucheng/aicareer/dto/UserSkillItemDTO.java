package com.xucheng.aicareer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserSkillItemDTO {

    @NotBlank(message = "技能名称不能为空")
    @Size(max = 100, message = "技能名称长度不能超过100个字符")
    private String skillName;

    @NotBlank(message = "技能分类不能为空")
    @Size(max = 100, message = "技能分类长度不能超过100个字符")
    private String skillCategory;

    @NotNull(message = "技能等级不能为空")
    @Min(value = 0, message = "技能等级不能小于0")
    @Max(value = 5, message = "技能等级不能大于5")
    private Integer level;
}
