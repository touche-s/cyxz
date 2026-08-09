package com.cyxz.message.controller;

import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.annotation.PreventRepeat;
import com.cyxz.common.web.CurrentUser;
import com.cyxz.message.dto.SendMessageRequest;
import com.cyxz.message.vo.ChatMessageVO;
import com.cyxz.message.vo.ConversationVO;
import com.cyxz.message.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 私信控制器
 * <p>提供会话列表、历史消息、发送消息、标记已读、未读总数等接口。
 */
@Tag(name = "私信服务", description = "私信控制器")
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 会话列表
     */
    @Operation(summary = "会话列表")
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> conversations(@CurrentUser Long userId) {
        return Result.success(chatService.listConversations(userId));
    }

    /**
     * 历史消息（分页）
     */
    @Operation(summary = "历史消息（分页）")
    @GetMapping("/conversations/{conversationId}/messages")
    public Result<PageResult<ChatMessageVO>> messages(@CurrentUser Long userId,
                                                       @PathVariable Long conversationId,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return Result.success(chatService.listMessages(userId, conversationId, page, size));
    }

    /**
     * 发送私信
     */
    @Operation(summary = "发送私信")
    @PreventRepeat(interval = 2)
    @PostMapping("/send")
    public Result<ChatMessageVO> send(@CurrentUser Long userId, @Valid @RequestBody SendMessageRequest request) {
        return Result.success(chatService.sendMessage(userId, request.getReceiverId(), request.getContent()));
    }

    /**
     * 标记会话已读
     */
    @Operation(summary = "标记会话已读")
    @PutMapping("/conversations/{conversationId}/read")
    public Result<Void> markRead(@CurrentUser Long userId, @PathVariable Long conversationId) {
        chatService.markRead(userId, conversationId);
        return Result.success();
    }

    /**
     * 私信总未读数
     */
    @Operation(summary = "私信总未读数")
    @GetMapping("/unread-total")
    public Result<Integer> unreadTotal(@CurrentUser Long userId) {
        return Result.success(chatService.unreadTotal(userId));
    }
}
