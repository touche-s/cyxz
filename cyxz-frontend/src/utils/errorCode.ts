/**
 * 统一错误码常量与工具函数
 * <p>与后端 {@code com.cyxz.common.base.ErrorCode} 枚举一一对应。
 * 6 位编码 = HTTP 状态码(前 3 位) + 业务码(后 3 位)。
 * 后 3 位区分业务模块：000 通用 / 100 认证 / 200 帖子 / 300 评论 / 400 圈子 / 500 用户 / 600 消息 / 700 上传
 */

/** 错误码常量 */
export const ErrorCode = {
  // ==================== 通用段 (000) ====================
  /** 操作成功 */
  SUCCESS: 200000,
  /** 操作失败 */
  FAIL: 500000,
  /** 系统异常 */
  SYSTEM_ERROR: 500001,
  /** 参数错误 */
  PARAM_ERROR: 400000,
  /** 缺少必填参数 */
  PARAM_MISSING: 400001,
  /** 未登录 */
  UNAUTHORIZED: 401000,
  /** 缺少 Token */
  TOKEN_MISSING: 401001,
  /** Token 无效 */
  TOKEN_INVALID: 401002,
  /** 登录已过期 */
  TOKEN_EXPIRED: 401003,
  /** 没有权限 */
  FORBIDDEN: 403000,
  /** 资源不存在 */
  NOT_FOUND: 404000,
  /** 资源已存在 */
  ALREADY_EXISTS: 409000,

  // ==================== 认证模块 (100) ====================
  /** 密码错误 */
  PASSWORD_ERROR: 400100,
  /** 验证码错误 */
  CAPTCHA_ERROR: 400101,
  /** 验证码已过期 */
  CAPTCHA_EXPIRED: 400102,
  /** 账号已被禁用 */
  USER_DISABLED: 403100,
  /** 登录失败次数过多 */
  LOGIN_TOO_MANY: 403101,
  /** 用户不存在 */
  USER_NOT_FOUND: 404100,
  /** 账号已存在 */
  USERNAME_EXISTS: 409100,

  // ==================== 帖子模块 (200) ====================
  /** 帖子不存在 */
  POST_NOT_FOUND: 404200,
  /** 帖子已删除 */
  POST_DELETED: 404201,
  /** 帖子审核未通过 */
  POST_REJECTED: 404202,
  /** 帖子待审核 */
  POST_PENDING: 404203,
  /** 帖子状态不可交互（如草稿仅作者可见） */
  POST_NOT_INTERACTABLE: 403201,
  /** 不是帖子作者 */
  NOT_POST_OWNER: 403202,
  /** 内容包含敏感词 */
  CONTENT_SENSITIVE: 400201,
  /** 帖子状态变更非法 */
  POST_STATUS_TRANSITION_INVALID: 400202,
  /** 帖子状态已被修改（并发冲突） */
  POST_STATUS_CONFLICT: 400203,

  // ==================== 评论模块 (300) ====================
  /** 评论不存在 */
  COMMENT_NOT_FOUND: 404300,
  /** 父评论不存在 */
  COMMENT_PARENT_NOT_FOUND: 404301,
  /** 不支持多级回复 */
  COMMENT_NO_MULTI_LEVEL: 400301,
  /** 不是评论作者或帖子作者 */
  NOT_COMMENT_OWNER: 403301,

  // ==================== 圈子模块 (400) ====================
  /** 圈子不存在 */
  CIRCLE_NOT_FOUND: 404400,
  /** 圈子已停用 */
  CIRCLE_DISABLED: 403401,
  /** 未加入圈子 */
  NOT_CIRCLE_MEMBER: 403402,
  /** 板块模板不存在 */
  SECTION_TEMPLATE_NOT_FOUND: 404410,

  // ==================== 用户模块 (500) ====================
  /** 不能对自己执行此操作 */
  SELF_OPERATION_FORBIDDEN: 403501,

  // ==================== 消息模块 (600) ====================
  /** 会话不存在 */
  CONVERSATION_NOT_FOUND: 404600,
  /** 无权操作此会话 */
  NOT_CONVERSATION_MEMBER: 403601,
  /** 需互相关注才能私信 */
  NOT_MUTUAL_FOLLOW: 403602,

  // ==================== 上传模块 (700) ====================
  /** 文件上传失败 */
  UPLOAD_FAILED: 500700,
  /** 文件删除失败 */
  UPLOAD_DELETE_FAILED: 500701,
  /** 文件格式不支持 */
  UPLOAD_TYPE_INVALID: 400701,
  /** 文件内容不合法 */
  UPLOAD_CONTENT_INVALID: 400702,
  /** 文件像素过大 */
  UPLOAD_PIXEL_TOO_LARGE: 400703,
} as const

export type ErrorCodeValue = typeof ErrorCode[keyof typeof ErrorCode]

/** 判断是否为成功码 */
export function isSuccessCode(code: number | undefined | null): boolean {
  return code === ErrorCode.SUCCESS
}

/** 取 HTTP 状态码段（前 3 位） */
export function httpStatusOf(code: number | undefined | null): number {
  if (code == null) return 0
  return Math.floor(code / 1000)
}

/** 是否为认证类错误（401 段：未登录 / Token 失效） */
export function isAuthError(code: number | undefined | null): boolean {
  return httpStatusOf(code) === 401
}

/** 是否为权限不足（403 段） */
export function isForbiddenError(code: number | undefined | null): boolean {
  return httpStatusOf(code) === 403
}

/** 是否为资源不存在（404 段） */
export function isNotFoundError(code: number | undefined | null): boolean {
  return httpStatusOf(code) === 404
}

/**
 * 从异常中提取后端返回的错误消息，回退到默认文案。
 * <p>统一各调用点的错误提示逻辑：优先显示后端 message，没有时用 defaultMsg。
 */
export function pickApiMessage(e: unknown, defaultMsg: string): string {
  if (e && typeof e === 'object' && 'message' in e) {
    const msg = (e as { message?: string }).message
    if (msg) return msg
  }
  return defaultMsg
}

