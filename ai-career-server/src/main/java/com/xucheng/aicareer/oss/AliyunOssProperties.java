package com.xucheng.aicareer.oss;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliyunOssProperties {

    @NotBlank
    private String endpoint;

    @NotBlank
    private String bucketName;
}
