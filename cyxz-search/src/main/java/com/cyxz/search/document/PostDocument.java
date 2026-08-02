package com.cyxz.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.List;

/**
 * ES 帖子文档，索引名 post
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "post")
@Setting(shards = 1, replicas = 0)
public class PostDocument {

    @Id
    private Long id;

    private Long userId;

    @Field(type = FieldType.Long)
    private Long circleId;

    @Field(type = FieldType.Long)
    private Long sectionId;

    @Field(type = FieldType.Keyword)
    private String postType;

    /** 标题：ik_max_word 分词，支持中文全文检索 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    /** 正文：ik_max_word 分词 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    @Field(type = FieldType.Keyword, index = false)
    private String cover;

    /** 标签：keyword 不分词，精确匹配 */
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Integer)
    private Integer status;

    @Field(type = FieldType.Integer)
    private Integer likes;

    @Field(type = FieldType.Integer)
    private Integer comments;

    @Field(type = FieldType.Integer)
    private Integer views;

    @Field(type = FieldType.Integer)
    private Integer collections;

    /** 创建时间，用于排序 */
    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Long createTime;
}
