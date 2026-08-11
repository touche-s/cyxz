package com.cyxz.message.service;

import com.cyxz.common.base.PageResult;
import com.cyxz.message.vo.ChatMessageVO;
import com.cyxz.message.vo.ConversationVO;

import java.util.List;

/**
 * 私信服务接口
 */
public interface ChatService {

    /**
     * 查询当前用户的会话列表
     */
    List<ConversationVO> listConversations(Long userId);

    /**
     * 分页查询会话历史消息
     */
    PageResult<ChatMessageVO> listMessages(Long userId, Long conversationId, int page, int size);

    /**
     * 发送私信（落库 + WebSocket 推送在线对方）
     */
    ChatMessageVO sendMessage(Long senderId, Long receiverId, String content);

    /**
     * 标记会话已读
     */
    void markRead(Long userId, Long conversationId);

    /**
     * 查询当前用户的私信总未读数
     * @param userId 当前用户 ID
     * @return 未读总数
     */
    int unreadTotal(Long userId);
}
