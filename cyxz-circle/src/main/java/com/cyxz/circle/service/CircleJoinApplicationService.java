package com.cyxz.circle.service;

import com.cyxz.common.base.PageResult;
import com.cyxz.circle.dto.CreateCircleJoinRequest;
import com.cyxz.circle.vo.CircleJoinApplicationVO;

/**
 * 入圈申请服务
 */
public interface CircleJoinApplicationService {

    /**
     * 用户提交入圈申请
     *
     * @param applicantId 申请人用户 ID
     * @param request     申请请求
     * @return 申请记录 ID
     */
    Long createApplication(Long applicantId, CreateCircleJoinRequest request);

    /**
     * 查询我的入圈申请
     *
     * @param applicantId 申请人用户 ID
     * @param page        页码
     * @param size        每页条数
     */
    PageResult<CircleJoinApplicationVO> listByApplicant(Long applicantId, int page, int size);

    /**
     * 管理端入圈申请列表（按状态/圈子筛选，分页）
     *
     * @param status   状态筛选（null=全部）
     * @param circleId 圈子筛选（null=全部）
     * @param page     页码
     * @param size     每页条数
     */
    PageResult<CircleJoinApplicationVO> listForAdmin(String status, Long circleId, int page, int size);

    /**
     * 申请详情
     *
     * @param id 申请记录 ID
     */
    CircleJoinApplicationVO getDetail(Long id);

    /**
     * 通过入圈申请，直接调用 {@link CircleService#joinCircle} 加入成员
     *
     * @param id          申请记录 ID
     * @param reviewerId  审核人用户 ID
     * @param reviewNote  审核意见
     */
    void approveApplication(Long id, Long reviewerId, String reviewNote);

    /**
     * 驳回入圈申请
     *
     * @param id          申请记录 ID
     * @param reviewerId  审核人用户 ID
     * @param reviewNote  审核意见
     */
    void rejectApplication(Long id, Long reviewerId, String reviewNote);
}
