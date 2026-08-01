package com.cyxz.message.controller;

import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.web.CurrentUser;
import com.cyxz.message.api.dto.SendMessageRequest;
import com.cyxz.message.api.vo.ChatMessageVO;
import com.cyxz.message.api.vo.ConversationVO;
import com.cyxz.message.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 私信控制器
 * <p>提供会话列表、历史消息、发送消息、标记已读、未读总数等接口。
 */
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 会话列表
     */
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> conversations(@CurrentUser Long userId) {
        return Result.success(chatService.listConversations(userId));
    }

    /**
     * 历史消息（分页）
     */
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
    @PostMapping("/send")
    public Result<ChatMessageVO> send(@CurrentUser Long userId, @RequestBody SendMessageRequest request) {
        return Result.success(chatService.sendMessage(userId, request.getReceiverId(), request.getContent()));
    }

    /**
     * 标记会话已读
     */
    @PutMapping("/conversations/{conversationId}/read")
    public Result<Void> markRead(@CurrentUser Long userId, @PathVariable Long conversationId) {
        chatService.markRead(userId, conversationId);
        return Result.success();
    }

    /**
     * 私信总未读数
     */
    @GetMapping("/unread-total")
    public Result<Integer> unreadTotal(@CurrentUser Long userId) {
        return Result.success(chatService.unreadTotal(userId));
    }
}
