package com.xucheng.aicareer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserSkillBatchDTO {

    @Valid
    @NotEmpty(message = "技能列表不能为空")
    @Size(max = 100, message = "一次最多保存100项技能")
    private List<UserSkillItemDTO> skills;
}
