package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProfileCompletionVO {

    private Boolean completed;
    private Integer score;
    private List<String> missingFields;
}
