package com.xucheng.aicareer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_report")
public class InterviewReport {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long interviewId;
    private Long userId;
    private Integer totalScore;
    private String scores;
    private String advantages;
    private String weaknesses;
    private String suggestions;
    private String summary;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
