package com.xucheng.aicareer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import com.xucheng.aicareer.service.model.KnowledgeReference;

/** 面向前端历史记录的职业规划聊天消息视图。 */
@Data
@Builder
@Schema(description = "职业规划聊天消息")
public class CareerChatMessageVO {

    private Long id;
    private String clientMessageId;

    @Schema(description = "消息角色", allowableValues = {"user", "assistant"})
    private String role;

    private String content;
    @Schema(description = "服务端持久化的来源快照；null 表示旧消息未经验证")
    private List<KnowledgeReference> references;

    @Schema(description = "消息状态：0处理中，1已完成，2失败")
    private Integer status;

    private Integer messageOrder;
    private LocalDateTime createTime;
}
