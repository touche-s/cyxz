package com.cyxz.governance.service;

import com.cyxz.common.base.PageResult;
import com.cyxz.governance.dto.CreateReportRequest;
import com.cyxz.governance.vo.ReportVO;

/**
 * 举报服务
 */
public interface ReportService {

    /**
     * 用户提交举报
     *
     * @param reporterId 举报人用户 ID
     * @param request    举报请求
     * @return 举报记录 ID
     */
    Long createReport(Long reporterId, CreateReportRequest request);

    /**
     * 管理端举报列表（按状态/类型筛选，分页）
     *
     * @param status     状态筛选（null=全部）
     * @param targetType 类型筛选（null=全部）
     * @param page       页码
     * @param size       每页条数
     * @return 举报分页列表
     */
    PageResult<ReportVO> listForAdmin(String status, String targetType, int page, int size);

    /**
     * 举报详情
     *
     * @param id 举报记录 ID
     * @return 举报详情
     */
    ReportVO getReportDetail(Long id);

    /**
     * 通过举报（发布内容处置事件，由 post/comment 消费删除内容）
     *
     * @param id        举报记录 ID
     * @param handlerId 处理人用户 ID
     * @param note      处理意见
     */
    void approveReport(Long id, Long handlerId, String note);

    /**
     * 驳回举报
     *
     * @param id        举报记录 ID
     * @param handlerId 处理人用户 ID
     * @param note      处理意见
     */
    void rejectReport(Long id, Long handlerId, String note);
}
