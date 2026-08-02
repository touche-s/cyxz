package com.cyxz.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.message.vo.ChatMessageVO;
import com.cyxz.message.vo.ConversationVO;
import com.cyxz.message.entity.ConversationPO;
import com.cyxz.message.entity.PrivateMessagePO;
import com.cyxz.message.mapper.ConversationMapper;
import com.cyxz.message.mapper.PrivateMessageMapper;
import com.cyxz.message.service.ChatService;
import com.cyxz.message.websocket.WebSocketSessionManager;
import com.cyxz.user.feign.UserFeignClient;
import com.cyxz.user.utils.UserFeignHelper;
import com.cyxz.user.vo.UserProfileVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 私信服务实现
 * <p>HTTP 负责消息持久化，WebSocket 负责在线实时推送。
 * <p>发消息时校验互相关注，非互相关注直接拒绝。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationMapper conversationMapper;
    private final PrivateMessageMapper messageMapper;
    private final UserFeignClient userFeignClient;
    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @Override
    public List<ConversationVO> listConversations(Long userId) {
        LambdaQueryWrapper<ConversationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationPO::getUserId1, userId).or().eq(ConversationPO::getUserId2, userId);
        wrapper.orderByDesc(ConversationPO::getLastMessageAt);
        List<ConversationPO> conversations = conversationMapper.selectList(wrapper);
        if (conversations.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> peerIds = conversations.stream()
                .map(c -> getPeerId(c, userId))
                .collect(Collectors.toSet());
        Map<Long, UserProfileVO> userMap = UserFeignHelper.batchGetUsers(userFeignClient, peerIds);

        return conversations.stream()
                .map(c -> toConversationVO(c, userId, userMap))
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<ChatMessageVO> listMessages(Long userId, Long conversationId, int page, int size) {
        ConversationPO conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "会话不存在");
        }
        if (!conv.getUserId1().equals(userId) && !conv.getUserId2().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看此会话");
        }

        LambdaQueryWrapper<PrivateMessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrivateMessagePO::getConversationId, conversationId);
        wrapper.orderByDesc(PrivateMessagePO::getCreateTime);
        Page<PrivateMessagePO> pageResult = messageMapper.selectPage(PageConstants.pageOf(page, size), wrapper);

        List<ChatMessageVO> records = pageResult.getRecords().stream()
                .map(this::toMessageVO)
                .collect(Collectors.toList());
        return PageResult.of(records, pageResult.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendMessage(Long senderId, Long receiverId, String content) {
        if (senderId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能给自己发私信");
        }
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "消息内容不能为空");
        }

        // 校验互相关注
        Result<Boolean> mutualResult = userFeignClient.isMutualFollowing(senderId, receiverId);
        if (mutualResult == null || !Boolean.TRUE.equals(mutualResult.getData())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要互相关注后才能私信");
        }

        // 获取或创建会话
        ConversationPO conv = getOrCreateConversation(senderId, receiverId);

        // 消息落库
        PrivateMessagePO msg = new PrivateMessagePO();
        msg.setConversationId(conv.getId());
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setIsRead(0);
        messageMapper.insert(msg);

        // 更新会话最后消息 + 对方未读数
        conv.setLastMessage(content);
        conv.setLastMessageAt(msg.getCreateTime());
        if (conv.getUserId1().equals(receiverId)) {
            conv.setUnreadCount1(conv.getUnreadCount1() + 1);
        } else {
            conv.setUnreadCount2(conv.getUnreadCount2() + 1);
        }
        conversationMapper.updateById(conv);

        // WebSocket 推送在线对方
        ChatMessageVO vo = toMessageVO(msg);
        if (sessionManager.isOnline(receiverId)) {
            try {
                Map<String, Object> envelope = new HashMap<>();
                envelope.put("type", "message");
                envelope.put("data", vo);
                sessionManager.sendToUser(receiverId, objectMapper.writeValueAsString(envelope));
            } catch (Exception e) {
                log.warn("WebSocket 推送失败: receiverId={}", receiverId, e);
            }
        }

        log.info("私信发送: senderId={}, receiverId={}, msgId={}", senderId, receiverId, msg.getId());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long conversationId) {
        ConversationPO conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            return;
        }
        if (!conv.getUserId1().equals(userId) && !conv.getUserId2().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此会话");
        }

        // 标记消息已读
        LambdaUpdateWrapper<PrivateMessagePO> msgWrapper = new LambdaUpdateWrapper<>();
        msgWrapper.eq(PrivateMessagePO::getConversationId, conversationId)
                .eq(PrivateMessagePO::getReceiverId, userId)
                .eq(PrivateMessagePO::getIsRead, 0)
                .set(PrivateMessagePO::getIsRead, 1);
        messageMapper.update(msgWrapper);

        // 清零会话未读数
        if (conv.getUserId1().equals(userId)) {
            conv.setUnreadCount1(0);
        } else {
            conv.setUnreadCount2(0);
        }
        conversationMapper.updateById(conv);
    }

    @Override
    public int unreadTotal(Long userId) {
        LambdaQueryWrapper<ConversationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationPO::getUserId1, userId).or().eq(ConversationPO::getUserId2, userId);
        List<ConversationPO> conversations = conversationMapper.selectList(wrapper);
        int total = 0;
        for (ConversationPO conv : conversations) {
            if (conv.getUserId1().equals(userId)) {
                total += conv.getUnreadCount1();
            } else {
                total += conv.getUnreadCount2();
            }
        }
        return total;
    }

    private ConversationPO getOrCreateConversation(Long user1, Long user2) {
        Long minId = Math.min(user1, user2);
        Long maxId = Math.max(user1, user2);
        LambdaQueryWrapper<ConversationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationPO::getUserId1, minId).eq(ConversationPO::getUserId2, maxId);
        ConversationPO conv = conversationMapper.selectOne(wrapper);
        if (conv == null) {
            conv = new ConversationPO();
            conv.setUserId1(minId);
            conv.setUserId2(maxId);
            conv.setUnreadCount1(0);
            conv.setUnreadCount2(0);
            conversationMapper.insert(conv);
        }
        return conv;
    }

    private Long getPeerId(ConversationPO conv, Long currentUserId) {
        return conv.getUserId1().equals(currentUserId) ? conv.getUserId2() : conv.getUserId1();
    }

    private ConversationVO toConversationVO(ConversationPO conv, Long currentUserId, Map<Long, UserProfileVO> userMap) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conv.getId());
        Long peerId = getPeerId(conv, currentUserId);
        vo.setPeerId(peerId);
        vo.setLastMessage(conv.getLastMessage());
        vo.setLastMessageAt(conv.getLastMessageAt());
        vo.setUnreadCount(conv.getUserId1().equals(currentUserId)
                ? conv.getUnreadCount1() : conv.getUnreadCount2());
        UserProfileVO peer = userMap.get(peerId);
        if (peer != null) {
            vo.setPeerName(peer.getNickname());
            vo.setPeerAvatar(peer.getAvatar());
        }
        return vo;
    }

    private ChatMessageVO toMessageVO(PrivateMessagePO po) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(po.getId());
        vo.setConversationId(po.getConversationId());
        vo.setSenderId(po.getSenderId());
        vo.setReceiverId(po.getReceiverId());
        vo.setContent(po.getContent());
        vo.setRead(po.getIsRead() != null && po.getIsRead() == 1);
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
