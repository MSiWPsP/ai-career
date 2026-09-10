package com.xucheng.aicareer.controller;

import com.xucheng.aicareer.common.Result;
import com.xucheng.aicareer.dto.CareerChatDTO;
import com.xucheng.aicareer.service.CareerChatService;
import com.xucheng.aicareer.service.CareerPlanService;
import com.xucheng.aicareer.vo.CareerChatVO;
import com.xucheng.aicareer.vo.CareerChatStreamVO;
import com.xucheng.aicareer.vo.CareerPlanVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/career")
@RequiredArgsConstructor
@Tag(name = "职业规划")
@SecurityRequirement(name = "BearerAuth")
public class CareerController {

    private final CareerPlanService careerPlanService;
    private final CareerChatService careerChatService;

    @PostMapping("/chat")
    @Operation(summary = "与AI职业规划师普通聊天", description = "非流式兼容接口，已包含近期ChatMemory，暂不包含Tool Calling或RAG")
    public Result<CareerChatVO> chat(@Valid @RequestBody CareerChatDTO chatDTO) {
        return Result.success(careerChatService.chat(chatDTO));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "与AI职业规划师流式聊天", description = "通过SSE依次返回delta、done或error事件，并按当前用户保持近期对话记忆")
    public Flux<ServerSentEvent<CareerChatStreamVO>> chatStream(@Valid @RequestBody CareerChatDTO chatDTO) {
        return careerChatService.chatStream(chatDTO)
                .map(item -> ServerSentEvent.<CareerChatStreamVO>builder()
                        .event(item.getType())
                        .data(item)
                        .build());
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
