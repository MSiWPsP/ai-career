package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;

@Data
@Builder
public class InterviewMessageVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String role;
    private String content;
    private String questionCategory;
    private String questionLevel;
    private Integer messageOrder;
    private LocalDateTime createTime;
}
