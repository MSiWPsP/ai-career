package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InterviewMessageVO {

    private Long id;
    private String role;
    private String content;
    private String questionCategory;
    private String questionLevel;
    private Integer messageOrder;
    private LocalDateTime createTime;
}
