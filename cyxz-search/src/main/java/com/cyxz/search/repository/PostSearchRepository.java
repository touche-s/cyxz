package com.cyxz.search.repository;

import com.cyxz.search.document.PostDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 帖子 ES 搜索仓库
 */
public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, Long> {

    /** 标题或正文模糊搜索 + 按圈子过滤 */
    Page<PostDocument> findByTitleOrContentAndCircleId(
            String title, String content, Long circleId, Pageable pageable);

    /** 标题或正文模糊搜索（不限圈子） */
    Page<PostDocument> findByTitleOrContent(String title, String content, Pageable pageable);
}
