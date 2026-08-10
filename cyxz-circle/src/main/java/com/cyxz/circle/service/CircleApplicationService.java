package com.cyxz.circle.service;

import com.cyxz.common.base.PageResult;
import com.cyxz.circle.dto.CreateCircleApplicationRequest;
import com.cyxz.circle.vo.CircleApplicationVO;

/**
 * 圈子创建申请服务
 */
public interface CircleApplicationService {

    /**
     * 用户提交建圈申请
     *
     * @param applicantId 申请人用户 ID
     * @param request     申请请求
     * @return 申请记录 ID
     */
    Long createApplication(Long applicantId, CreateCircleApplicationRequest request);

    /**
     * 查询我的建圈申请（按申请人筛选，分页）
     *
     * @param applicantId 申请人用户 ID
     * @param page        页码
     * @param size        每页条数
     */
    PageResult<CircleApplicationVO> listByApplicant(Long applicantId, int page, int size);

    /**
     * 管理端建圈申请列表（按状态筛选，分页）
     *
     * @param status 状态筛选（null=全部）
     * @param page   页码
     * @param size   每页条数
     */
    PageResult<CircleApplicationVO> listForAdmin(String status, int page, int size);

    /**
     * 申请详情
     *
     * @param id 申请记录 ID
     */
    CircleApplicationVO getDetail(Long id);

    /**
     * 通过建圈申请，直接调用 {@link CircleService#createCircle} 建圈
     *
     * @param id          申请记录 ID
     * @param reviewerId  审核人用户 ID
     * @param reviewNote  审核意见
     */
    void approveApplication(Long id, Long reviewerId, String reviewNote);

    /**
     * 驳回建圈申请
     *
     * @param id          申请记录 ID
     * @param reviewerId  审核人用户 ID
     * @param reviewNote  审核意见
     */
    void rejectApplication(Long id, Long reviewerId, String reviewNote);
}
