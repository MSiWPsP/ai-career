package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSkillVO {

    private Long id;
    private String skillName;
    private String skillCategory;
    private Integer level;
    private Integer score;
    private String source;
}
