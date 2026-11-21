package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.dto.ChatMessageDTO;
import com.aiapp.activityassistant.service.ChatService;
import com.aiapp.activityassistant.vo.ChatMessageVO;
import com.aiapp.activityassistant.ws.ChatSocketHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 活动群聊：历史记录走 REST，实时消息走 WebSocket 广播
 */
@Tag(name = "活动群聊")
@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
@RequiresPermissions("activity:view")
public class ChatController {

    private final ChatService chatService;
    private final ChatSocketHandler chatSocketHandler;

    @Operation(summary = "拉取活动聊天记录（按时间正序）")
    @GetMapping("/{id}/chat/messages")
    public Result<List<ChatMessageVO>> list(@PathVariable Long id,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(chatService.list(id, limit));
    }

    @Operation(summary = "发送聊天消息（保存后通过 WebSocket 广播给在线成员）")
    @PostMapping("/{id}/chat/messages")
    public Result<ChatMessageVO> send(@PathVariable Long id, @Valid @RequestBody ChatMessageDTO dto) {
        ChatMessageVO message = chatService.send(id, dto.getContent().trim());
        chatSocketHandler.broadcast(id, message);
        return Result.success(message);
    }
}
