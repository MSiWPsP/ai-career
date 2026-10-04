package com.xucheng.aicareer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserSkillBatchDTO {

    @Valid
    // PUT 是全量替换：空集合表示用户明确清空技能；缺失/null 仍属于非法请求。
    @NotNull(message = "技能列表不能为null")
    @Size(max = 100, message = "一次最多保存100项技能")
    private List<UserSkillItemDTO> skills;
}
