package com.cyxz.search.service;

import com.cyxz.common.event.PostEsSyncEvent;
import com.cyxz.search.document.PostDocument;
import com.cyxz.search.repository.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ES 索引同步服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostIndexService {

    private final PostSearchRepository repository;

    /**
     * 同步帖子文档到 ES 索引
     * @param event 帖子 ES 同步事件，包含帖子 ID、操作类型（新增/更新/删除）及帖子字段
     */
    public void sync(PostEsSyncEvent event) {
        if ("DELETE".equals(event.getAction())) {
            repository.deleteById(event.getPostId());
            log.info("ES 删除文档: postId={}", event.getPostId());
            return;
        }

        List<String> tags = parseTags(event.getTags());

        PostDocument doc = PostDocument.builder()
                .id(event.getPostId())
                .userId(event.getUserId())
                .circleId(event.getCircleId())
                .sectionId(event.getSectionId())
                .postType(event.getPostType())
                .title(event.getTitle() != null ? event.getTitle() : "")
                .content(event.getContent() != null ? event.getContent() : "")
                .cover(event.getCover())
                .tags(tags)
                .status(event.getStatus())
                .likes(event.getLikes() != null ? event.getLikes() : 0)
                .comments(event.getComments() != null ? event.getComments() : 0)
                .views(event.getViews() != null ? event.getViews() : 0)
                .collections(event.getCollections() != null ? event.getCollections() : 0)
                .createTime(event.getCreateTime() != null ? event.getCreateTime() : System.currentTimeMillis())
                .build();

        repository.save(doc);
        log.info("ES 同步文档: postId={}, action={}", event.getPostId(), event.getAction());
    }

    private List<String> parseTags(String tagsStr) {
        if (tagsStr == null || tagsStr.isBlank()) return Collections.emptyList();
        return Arrays.asList(tagsStr.split(","));
    }
}
