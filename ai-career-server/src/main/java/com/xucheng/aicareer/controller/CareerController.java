package com.xucheng.aicareer.controller;

import com.xucheng.aicareer.common.Result;
import com.xucheng.aicareer.dto.CareerChatDTO;
import com.xucheng.aicareer.dto.CareerChatSessionUpdateDTO;
import com.xucheng.aicareer.dto.CareerPlanRegenerateDTO;
import com.xucheng.aicareer.service.CareerChatService;
import com.xucheng.aicareer.service.CareerPlanService;
import com.xucheng.aicareer.vo.CareerChatVO;
import com.xucheng.aicareer.vo.CareerChatMessageVO;
import com.xucheng.aicareer.vo.CareerChatSessionVO;
import com.xucheng.aicareer.vo.CareerChatStreamVO;
import com.xucheng.aicareer.vo.CareerPlanVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 职业规划模块 HTTP 入口。
 *
 * <p>Controller 只负责参数校验、SSE 事件封装和统一响应，不包含 Agent 或数据库业务逻辑。</p>
 */
@RestController
@RequestMapping("/api/career")
@RequiredArgsConstructor
@Tag(name = "职业规划")
@SecurityRequirement(name = "BearerAuth")
public class CareerController {

    private final CareerPlanService careerPlanService;
    private final CareerChatService careerChatService;

    @PostMapping("/chat")
    @Operation(summary = "与AI职业规划师普通聊天", description = "读取最新职业画像、技能和近期记忆；启用RAG时按需检索职业知识，失败时降级为原有聊天")
    public Result<CareerChatVO> chat(@Valid @RequestBody CareerChatDTO chatDTO) {
        return Result.success(careerChatService.chat(chatDTO));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "与AI职业规划师流式聊天", description = "通过SSE依次返回delta、done或error事件，每次请求读取最新职业画像、技能和当前规划，并按当前用户保持近期对话记忆")
    public Flux<ServerSentEvent<CareerChatStreamVO>> chatStream(@Valid @RequestBody CareerChatDTO chatDTO) {
        // Service 返回领域事件，Controller 在传输边界转换为标准 SSE 事件。
        return careerChatService.chatStream(chatDTO)
                .map(item -> ServerSentEvent.<CareerChatStreamVO>builder()
                        .event(item.getType())
                        .data(item)
                        .build());
    }

    @PostMapping("/conversations")
    @Operation(summary = "新建职业规划聊天会话")
    public Result<CareerChatSessionVO> createConversation() {
        return Result.success("会话创建成功", careerChatService.createConversation());
    }

    @GetMapping("/conversations")
    @Operation(summary = "获取职业规划聊天会话列表")
    public Result<List<CareerChatSessionVO>> getConversations(
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return Result.success(careerChatService.getConversations(includeArchived));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "获取职业规划聊天历史消息")
    public Result<List<CareerChatMessageVO>> getConversationMessages(@PathVariable String conversationId) {
        return Result.success(careerChatService.getMessages(conversationId));
    }

    @PutMapping("/conversations/{conversationId}")
    @Operation(summary = "重命名、归档或恢复职业规划聊天会话")
    public Result<CareerChatSessionVO> updateConversation(
            @PathVariable String conversationId,
            @Valid @RequestBody CareerChatSessionUpdateDTO updateDTO) {
        return Result.success("会话更新成功", careerChatService.updateConversation(conversationId, updateDTO));
    }

    @DeleteMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "清空职业规划聊天会话内容")
    public Result<Void> clearConversation(@PathVariable String conversationId) {
        careerChatService.clearConversation(conversationId);
        return Result.success();
    }

    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "删除职业规划聊天会话")
    public Result<Void> deleteConversation(@PathVariable String conversationId) {
        careerChatService.deleteConversation(conversationId);
        return Result.success();
    }

    @PostMapping("/plan/generate")
    @Operation(summary = "生成首版职业规划", description = "读取当前用户职业画像和技能，调用CareerPlannerAgent生成结构化规划并同步创建成长任务")
    public Result<CareerPlanVO> generatePlan() {
        return Result.success("职业规划生成成功", careerPlanService.generatePlan());
    }

    @PostMapping("/plan/regenerate")
    @Operation(summary = "根据面试反馈重新规划", description = "读取指定面试报告、当前画像技能及旧规划任务进度，生成新版本并归档旧版本")
    public Result<CareerPlanVO> regeneratePlan(@Valid @RequestBody CareerPlanRegenerateDTO request) {
        return Result.success("职业规划已更新", careerPlanService.regeneratePlan(request));
    }

    @GetMapping("/plan/current")
    @Operation(summary = "获取当前职业规划")
    public Result<CareerPlanVO> getCurrentPlan() {
        return Result.success(careerPlanService.getCurrentPlan());
    }

    @GetMapping("/plan/history")
    @Operation(summary = "获取职业规划历史")
    public Result<List<CareerPlanVO>> getPlanHistory() {
        return Result.success(careerPlanService.getPlanHistory());
    }

    @GetMapping("/plan/{id}")
    @Operation(summary = "获取职业规划详情")
    public Result<CareerPlanVO> getPlanById(@PathVariable Long id) {
        return Result.success(careerPlanService.getPlanById(id));
    }
}
