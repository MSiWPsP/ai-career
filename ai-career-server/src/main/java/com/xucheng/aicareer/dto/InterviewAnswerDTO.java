package com.xucheng.aicareer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InterviewAnswerDTO {

    @NotBlank(message = "面试回答不能为空")
    @Size(max = 5000, message = "面试回答长度不能超过5000个字符")
    private String answer;
}
