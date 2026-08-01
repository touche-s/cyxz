package com.cyxz.search.controller;

import com.cyxz.common.base.PageResult;
import com.cyxz.search.document.PostDocument;
import com.cyxz.search.repository.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索接口
 */
@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final ElasticsearchOperations esOps;
    private final PostSearchRepository repository;

    @GetMapping("/post")
    public PageResult<PostDocument> search(
            @RequestParam String keyword,
            @RequestParam(required = false) Long circleId,
            @RequestParam(defaultValue = "hot") String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (keyword.isBlank()) {
            return PageResult.empty(page, size);
        }

        // 分页参数防御：page 最小 1，size 限制 1~50 防止拉取过多数据
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 50);

        // 构建高亮
        HighlightQuery highlightQuery = new HighlightQuery(
                new Highlight(List.of(
                        new HighlightField("title"),
                        new HighlightField("content"))),
                PostDocument.class);

        // 构建查询条件
        Criteria criteria = new Criteria("title").matches(keyword)
                .or(new Criteria("content").matches(keyword));

        if (circleId != null && circleId > 0) {
            criteria = criteria.and(new Criteria("circleId").is(circleId));
        }

        // 排序
        Sort sort = "hot".equals(sortBy)
                ? Sort.by(Sort.Direction.DESC, "likes")
                : Sort.by(Sort.Direction.DESC, "createTime");

        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(PageRequest.of(page - 1, size, sort));
        query.setHighlightQuery(highlightQuery);

        SearchHits<PostDocument> hits = esOps.search(query, PostDocument.class);

        List<PostDocument> results = new ArrayList<>();
        for (SearchHit<PostDocument> hit : hits) {
            PostDocument doc = hit.getContent();
            // 填充高亮到 title/content
            List<String> titleHighlights = hit.getHighlightField("title");
            if (titleHighlights != null && !titleHighlights.isEmpty()) {
                doc.setTitle(titleHighlights.get(0));
            }
            List<String> contentHighlights = hit.getHighlightField("content");
            if (contentHighlights != null && !contentHighlights.isEmpty()) {
                doc.setContent(contentHighlights.get(0));
            }
            results.add(doc);
        }

        return PageResult.of(results, hits.getTotalHits(), page, size);
    }
}
