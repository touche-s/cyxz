package com.cyxz.analytics.vo;

import lombok.Data;

/**
 * 看板概览 VO（当日各指标汇总）
 */
@Data
public class DashboardVO {

    /** 新增用户数 */
    private Integer newUsers;

    /** 新增帖子数 */
    private Integer newPosts;

    /** 帖子审核通过数 */
    private Integer approvedPosts;

    /** 帖子审核驳回数 */
    private Integer rejectedPosts;

    /** 新增圈子数 */
    private Integer newCircles;

    /** 新增加入圈子数 */
    private Integer newJoins;

    /** 举报处理数 */
    private Integer reportHandled;
}
