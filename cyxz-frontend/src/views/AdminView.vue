<template>
  <div class="admin-container">
    <aside class="admin-sidebar">
      <div class="sidebar-user" v-if="userStore.userInfo">
        <div class="sidebar-user-avatar">
          <img v-if="userStore.userInfo.avatar" :src="userStore.userInfo.avatar" />
          <span v-else class="sidebar-user-fb">{{ (userStore.userInfo.nickname || '?').charAt(0) }}</span>
        </div>
        <div class="sidebar-user-info">
          <span class="sidebar-user-hi">欢迎回来</span>
          <span class="sidebar-user-name">{{ userStore.userInfo.nickname || userStore.userInfo.userId }}</span>
          <span class="sidebar-user-role" :class="userStore.isAdmin ? 'role-admin' : 'role-user'">{{ roleLabel(userStore.role) }}</span>
        </div>
        <button class="sidebar-dark-btn" @click="toggleDarkMode" :title="isDark ? '切到日间模式' : '切到夜间模式'">
          <Icon :icon="isDark ? 'ph:sun' : 'ph:moon'" />
        </button>
      </div>
      <nav class="sidebar-nav">
        <button class="nav-item" :class="{ active: activeTab === 'circles' }" @click="activeTab = 'circles'">
          <Icon icon="ph:circles-three" class="nav-icon" />
          <span>圈子管理</span>
        </button>
        <button class="nav-item" :class="{ active: activeTab === 'sectionTemplates' }" @click="activeTab = 'sectionTemplates'">
          <Icon icon="ph:stack" class="nav-icon" />
          <span>板块模板</span>
        </button>
        <button class="nav-item" :class="{ active: activeTab === 'review' }" @click="activeTab = 'review'">
          <Icon icon="ph:shield-check" class="nav-icon" />
          <span>内容审核</span>
        </button>
        <button class="nav-item" :class="{ active: activeTab === 'users' }" @click="activeTab = 'users'">
          <Icon icon="ph:users" class="nav-icon" />
          <span>用户管理</span>
        </button>
        <button class="nav-item" :class="{ active: activeTab === 'posts' }" @click="activeTab = 'posts'">
          <Icon icon="ph:article" class="nav-icon" />
          <span>帖子管理</span>
        </button>
        <button class="nav-item" :class="{ active: activeTab === 'reports' }" @click="activeTab = 'reports'">
          <Icon icon="ph:flag" class="nav-icon" />
          <span>举报管理</span>
        </button>
        <button class="nav-item" :class="{ active: activeTab === 'audit' }" @click="activeTab = 'audit'">
          <Icon icon="ph:clipboard-text" class="nav-icon" />
          <span>审计日志</span>
        </button>
        <button class="nav-item" :class="{ active: activeTab === 'dashboard' }" @click="activeTab = 'dashboard'">
          <Icon icon="ph:chart-bar" class="nav-icon" />
          <span>数据看板</span>
        </button>
        <button class="nav-item" :class="{ active: activeTab === 'circleApps' }" @click="activeTab = 'circleApps'">
          <Icon icon="ph:buildings" class="nav-icon" />
          <span>建圈申请</span>
        </button>
        <button class="nav-item" :class="{ active: activeTab === 'joinApps' }" @click="activeTab = 'joinApps'">
          <Icon icon="ph:user-plus" class="nav-icon" />
          <span>入圈申请</span>
        </button>
        <button class="nav-item" :class="{ active: activeTab === 'roles' }" @click="activeTab = 'roles'">
          <Icon icon="ph:shield-star" class="nav-icon" />
          <span>角色权限</span>
        </button>
      </nav>
      <div class="sidebar-footer">
        <button class="back-btn" @click="to('/')">
          <Icon icon="ph:arrow-left" />
          返回前台
        </button>
      </div>
    </aside>

    <main class="admin-main">
      <!-- 圈子管理 -->
      <section v-if="activeTab === 'circles'" class="admin-section">
        <div class="section-head">
          <div>
            <h2>圈子管理</h2>
            <p class="section-desc">共 {{ filteredCircles.length }} 个圈子</p>
          </div>
          <div class="section-head-right">
            <button class="toolbar-btn" @click="refreshCurrentTab" title="刷新"><Icon icon="ph:arrows-clockwise" /></button>
            <button class="create-btn" @click="openCreateCircle">
              <Icon icon="ph:plus" />
              新建圈子
            </button>
          </div>
        </div>
        <div class="section-search">
          <SearchInput v-model="searchKeyword" variant="inline" :placeholder="searchPlaceholder" />
        </div>

        <LoadingSpinner v-if="circleLoading && circles.length === 0" />
        <div v-else class="table-wrap">
          <table v-if="filteredCircles.length > 0" class="data-table">
            <thead>
              <tr>
                <th style="width: 48px;"></th>
                <th>名称</th>
                <th>简介</th>
                <th style="width: 80px;">成员</th>
                <th style="width: 80px;">帖子</th>
                <th style="width: 70px;">状态</th>
                <th style="width: 160px;">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in filteredCircles" :key="c.id">
                <td>
                  <div class="avatar-32">
                    <img v-if="c.avatar" :src="c.avatar" />
                    <span v-else class="avatar-32-fb">{{ c.name?.charAt(0) }}</span>
                  </div>
                </td>
                <td class="td-name">{{ c.name }}</td>
                <td class="td-intro" :title="c.intro">{{ c.intro?.slice(0, 40) || '-' }}</td>
                <td>{{ c.memberCount ?? 0 }}</td>
                <td>{{ (c as any).postCount ?? 0 }}</td>
                <td>
                  <span class="tag" :class="(c as any).status === 1 ? 'tag-green' : 'tag-red'">
                    {{ (c as any).status === 1 ? '启用' : '禁用' }}
                  </span>
                </td>
                <td class="td-actions">
                  <button class="op-link" @click="openEditCircle(c)">编辑</button>
                  <button class="op-link op-link-purple" @click="openSectionConfig(c)">板块</button>
                  <button class="op-link" @click="toggleCircleStatus(c)">{{ (c as any).status === 1 ? '禁用' : '启用' }}</button>
                  <button class="op-link op-link-danger" @click="deleteCircle(c)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
          <EmptyState v-if="filteredCircles.length === 0" title="暂无圈子，点击上方按钮创建" />
        </div>
      </section>

      <!-- 板块模板管理 -->
      <section v-if="activeTab === 'sectionTemplates'" class="admin-section">
        <div class="section-head">
          <div>
            <h2>板块模板管理</h2>
            <p class="section-desc">共 {{ sectionTemplates.length }} 个模板</p>
          </div>
          <div class="section-head-right">
            <button class="toolbar-btn" @click="refreshCurrentTab" title="刷新"><Icon icon="ph:arrows-clockwise" /></button>
            <button class="create-btn" @click="showSectionTemplateForm = true" v-if="!showSectionTemplateForm">
              <Icon icon="ph:plus" />
              新建模板
            </button>
          </div>
        </div>
        <div class="section-search">
          <SearchInput v-model="searchKeyword" variant="inline" :placeholder="searchPlaceholder" />
        </div>

        <div v-if="showSectionTemplateForm" class="form-card">
          <h3>{{ editingSectionTemplate ? '编辑模板' : '新建模板' }}</h3>
          <div class="form-row">
            <label>模板名称</label>
            <input v-model="sectionTemplateForm.name" class="form-input" placeholder="输入板块名称" />
          </div>
          <div class="form-row">
            <label>适用类型</label>
            <select v-model="sectionTemplateForm.applicableType" class="form-input">
              <option value="ALL">全部</option>
              <option value="NORMAL">图文</option>
              <option value="ARTICLE">长文</option>
            </select>
          </div>
          <div class="form-row">
            <label>描述</label>
            <input v-model="sectionTemplateForm.description" class="form-input" placeholder="模板描述（可选）" />
          </div>
          <div class="form-row">
            <label>排序</label>
            <input v-model="sectionTemplateForm.sortOrder" type="number" class="form-input" placeholder="数字越小越靠前" />
          </div>
          <div class="form-actions">
            <button class="cancel-btn" @click="cancelSectionTemplateForm">取消</button>
            <button class="submit-btn" @click="submitSectionTemplate" :disabled="sectionTemplateLoading">
              {{ sectionTemplateLoading ? '提交中...' : (editingSectionTemplate ? '保存修改' : '创建模板') }}
            </button>
          </div>
        </div>

        <LoadingSpinner v-if="sectionTemplateLoading && sectionTemplates.length === 0" />
        <div v-else class="table-wrap">
          <table v-if="filteredSectionTemplates.length > 0" class="data-table">
            <thead>
              <tr>
                <th style="width: 100px;">ID</th>
                <th>名称</th>
                <th style="width: 80px;">类型</th>
                <th>描述</th>
                <th style="width: 60px;">排序</th>
                <th style="width: 120px;">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in filteredSectionTemplates" :key="t.id">
                <td class="td-id">{{ t.id }}</td>
                <td class="td-name">{{ t.name }}</td>
                <td><span class="type-tag">{{ typeLabel(t.applicableType) }}</span></td>
                <td class="td-intro" :title="t.description || undefined">{{ t.description || '-' }}</td>
                <td>{{ t.sortOrder }}</td>
                <td class="td-actions">
                  <button class="op-link" @click="editSectionTemplate(t)">编辑</button>
                  <button class="op-link op-link-danger" @click="deleteSectionTemplate(t)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
          <EmptyState v-if="filteredSectionTemplates.length === 0" title="暂无板块模板" />
        </div>
      </section>

      <!-- 内容审核 -->
      <section v-if="activeTab === 'review'" class="admin-section">
        <div class="section-head">
          <div>
            <h2>内容审核</h2>
            <p class="section-desc">共 {{ reviewPosts.length }} 条待审核</p>
          </div>
          <div class="section-head-right">
            <button class="toolbar-btn" @click="refreshCurrentTab" title="刷新"><Icon icon="ph:arrows-clockwise" /></button>
          </div>
        </div>
        <div class="section-search">
          <SearchInput v-model="searchKeyword" variant="inline" :placeholder="searchPlaceholder" />
        </div>
        <LoadingSpinner v-if="reviewLoading && reviewPosts.length === 0" />
        <div v-else class="table-wrap">
          <table v-if="filteredReviewPosts.length > 0" class="data-table">
            <thead>
              <tr>
                <th style="width: 100px;">ID</th>
                <th>标题</th>
                <th style="width: 100px;">作者</th>
                <th style="width: 80px;">状态</th>
                <th style="width: 110px;">创建时间</th>
                <th style="width: 120px;">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in filteredReviewPosts" :key="p.id">
                <td class="td-id">{{ p.id }}</td>
                <td class="td-name" :title="p.title">{{ p.title }}</td>
                <td>{{ p.authorName }}</td>
                <td><span class="tag tag-blue">待审核</span></td>
                <td class="td-time">{{ p.createTime?.slice(0, 10) }}</td>
                <td class="td-actions">
                  <button class="op-link op-link-green" @click="handleApprove(p)">通过</button>
                  <button class="op-link op-link-danger" @click="openRejectDialog(p)">拒绝</button>
                </td>
              </tr>
            </tbody>
          </table>
          <EmptyState v-if="filteredReviewPosts.length === 0" title="暂无待审核内容" />
        </div>
      </section>

      <!-- 用户管理 -->
      <section v-if="activeTab === 'users'" class="admin-section">
        <div class="section-head">
          <div>
            <h2>用户管理</h2>
            <p class="section-desc">共 {{ users.length }} 个用户</p>
          </div>
          <div class="section-head-right">
            <button class="toolbar-btn" @click="refreshCurrentTab" title="刷新"><Icon icon="ph:arrows-clockwise" /></button>
          </div>
        </div>
        <div class="section-search">
          <SearchInput v-model="searchKeyword" variant="inline" :placeholder="searchPlaceholder" />
        </div>
        <LoadingSpinner v-if="userLoading" />
        <div v-else class="table-wrap">
          <table v-if="filteredUsers.length > 0" class="data-table">
            <thead>
              <tr>
                <th style="width: 100px;">ID</th>
                <th>用户名</th>
                <th style="width: 130px;">昵称</th>
                <th style="width: 100px;">角色</th>
                <th style="width: 80px;">状态</th>
                <th style="width: 80px;">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in filteredUsers" :key="u.id">
                <td class="td-id">{{ u.id }}</td>
                <td>{{ u.username }}</td>
                <td>{{ u.nickname || '-' }}</td>
                <td><span class="tag" :class="['SITE_OWNER', 'PLATFORM_ADMIN'].includes(u.role) ? 'tag-amber' : 'tag-gray'">{{ roleLabel(u.role) }}</span></td>
                <td><span class="tag" :class="u.status === 1 ? 'tag-green' : 'tag-red'">{{ u.status === 1 ? '正常' : '禁用' }}</span></td>
                <td class="td-actions">
                  <button class="op-link op-link-purple" @click="openRoleAssign(u)">分配角色</button>
                  <button v-if="u.status === 1" class="op-link op-link-danger" @click="disableUser(u)">禁用</button>
                  <button v-else class="op-link op-link-green" @click="enableUser(u)">启用</button>
                </td>
              </tr>
            </tbody>
          </table>
          <EmptyState v-if="filteredUsers.length === 0" title="暂无用户数据" />
        </div>
      </section>

      <!-- 帖子管理 -->
      <section v-if="activeTab === 'posts'" class="admin-section">
        <div class="section-head">
          <div>
            <h2>帖子管理</h2>
            <p class="section-desc">共 {{ adminPostTotal }} 篇帖子</p>
          </div>
          <div class="section-head-right">
            <button class="toolbar-btn" @click="loadAdminPosts" title="刷新"><Icon icon="ph:arrows-clockwise" /></button>
          </div>
        </div>
        <div class="section-search">
          <select v-model="adminPostStatusFilter" class="filter-select" @change="adminPostPage = 1; loadAdminPosts()">
            <option :value="null">全部状态</option>
            <option :value="0">草稿</option>
            <option :value="1">待审核</option>
            <option :value="2">已通过</option>
            <option :value="3">已拒绝</option>
            <option :value="4">已删除</option>
          </select>
          <SearchInput v-model="searchKeyword" variant="inline" :placeholder="searchPlaceholder" />
        </div>
        <LoadingSpinner v-if="adminPostLoading && adminPosts.length === 0" />
        <div v-else class="table-wrap">
          <table v-if="filteredAdminPosts.length > 0" class="data-table">
            <thead>
              <tr>
                <th style="width: 100px;">ID</th>
                <th>标题</th>
                <th style="width: 100px;">作者</th>
                <th style="width: 80px;">状态</th>
                <th style="width: 110px;">创建时间</th>
                <th style="width: 80px;">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in filteredAdminPosts" :key="p.id">
                <td class="td-id">{{ p.id }}</td>
                <td class="td-name" :title="p.title">{{ p.title }}</td>
                <td>{{ p.authorName || '-' }}</td>
                <td><span class="tag" :class="statusTagClass[p.status]">{{ statusLabels[p.status] }}</span></td>
                <td class="td-time">{{ p.createTime?.slice(0, 10) }}</td>
                <td class="td-actions">
                  <button class="op-link op-link-danger" @click="adminDeletePost(p)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
          <EmptyState v-if="filteredAdminPosts.length === 0" title="暂无帖子" />
        </div>
        <div v-if="adminPostTotal > 20" class="pagination">
          <button class="page-btn" :disabled="adminPostPage === 1" @click="adminPostPage--; loadAdminPosts()">上一页</button>
          <span class="page-info">第 {{ adminPostPage }} 页</span>
          <button class="page-btn" :disabled="adminPosts.length < 20" @click="adminPostPage++; loadAdminPosts()">下一页</button>
        </div>
      </section>

      <!-- 角色权限管理 -->
      <section v-if="activeTab === 'roles'" class="admin-section">
        <div class="section-head">
          <div>
            <h2>角色权限管理</h2>
            <p class="section-desc">选择角色，勾选权限点后保存</p>
          </div>
        </div>
        <div class="section-search">
          <SearchInput v-model="searchKeyword" variant="inline" :placeholder="searchPlaceholder" />
        </div>
        <LoadingSpinner v-if="roleLoading" />
        <div v-else class="rbac-layout">
          <!-- 左侧角色列表 -->
          <div class="rbac-roles">
            <div class="rbac-roles-hd">角色列表</div>
            <div class="rbac-role-list">
              <button
                v-for="r in filteredRoles"
                :key="r.id"
                class="rbac-role-item"
                :class="{ active: selectedRoleId === r.id }"
                @click="selectRole(r.id)"
              >
                <div class="rbac-role-info">
                  <span class="rbac-role-label">{{ r.label }}</span>
                  <span class="rbac-role-code">{{ r.code }}</span>
                </div>
                <span class="rbac-role-scope" :class="r.scope === 'GLOBAL' ? 'scope-global' : 'scope-circle'">
                  {{ r.scope === 'GLOBAL' ? '全局' : '圈子' }}
                </span>
              </button>
            </div>
          </div>
          <!-- 右侧权限分配 -->
          <div class="rbac-perms">
            <div class="rbac-perms-hd">
              <span>权限点分配</span>
              <button class="submit-btn" :disabled="rolePermSaving" @click="saveRolePermissions">
                {{ rolePermSaving ? '保存中...' : '保存权限' }}
              </button>
            </div>
            <LoadingSpinner v-if="rolePermLoading" text="加载中..." />
            <div v-else class="rbac-perm-groups-scroll">
              <div v-for="resource in permGroups" :key="resource" class="rbac-perm-group">
                <div class="rbac-perm-group-hd" @click="togglePermGroup(resource)">
                  <Icon :icon="collapsedPermGroups.has(resource) ? 'ph:caret-right' : 'ph:caret-down'" class="rbac-group-arrow" />
                  {{ resource }}
                </div>
                <div v-show="!collapsedPermGroups.has(resource)" class="rbac-perm-items">
                  <div
                    v-for="p in permissionsByResource[resource]"
                    :key="p.id"
                    class="rbac-perm-toggle"
                    @click="togglePermission(p.id)"
                  >
                    <div class="rbac-toggle-info">
                      <span class="rbac-toggle-label">{{ p.label }}</span>
                      <span class="rbac-toggle-code">{{ p.code }}</span>
                    </div>
                    <div class="rbac-toggle-switch" :class="{ on: rolePermissionIds.has(p.id) }">
                      <div class="rbac-toggle-knob"></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
      <!-- 举报管理 -->
      <ReportManagement v-if="activeTab === 'reports'" v-model:search-keyword="searchKeyword" />

      <!-- 审计日志 -->
      <AuditLog v-if="activeTab === 'audit'" v-model:search-keyword="searchKeyword" />

      <!-- 数据看板 -->
      <AnalyticsDashboard v-if="activeTab === 'dashboard'" :search-keyword="searchKeyword" />

      <!-- 建圈申请审核 -->
      <CircleApplicationReview v-if="activeTab === 'circleApps'" v-model:search-keyword="searchKeyword" />

      <!-- 入圈申请审核 -->
      <CircleJoinApplicationReview v-if="activeTab === 'joinApps'" v-model:search-keyword="searchKeyword" />
    </main>

    <!-- 圈子编辑/新建弹窗 -->
    <Teleport to="body">
      <div class="modal-overlay" v-if="showCircleModal" @click.self="showCircleModal = false">
        <div class="modal-card" style="width: 500px;">
          <div class="modal-header">
            <h3>{{ editingCircleId ? '编辑圈子' : '新建圈子' }}</h3>
            <button class="modal-close" @click="showCircleModal = false">&times;</button>
          </div>
          <div class="modal-body">
            <div class="form-row">
              <label>圈子名称</label>
              <input v-model="circleForm.name" class="form-input" placeholder="输入圈子名称" />
            </div>
            <div class="form-row">
              <label>简介</label>
              <textarea v-model="circleForm.intro" class="form-textarea" rows="2" placeholder="一句话简介"></textarea>
            </div>
            <div class="form-row">
              <label>头像</label>
              <div class="upload-row">
                <div class="upload-preview avatar-preview">
                  <img v-if="circleAvatarPreview || circleForm.avatar" :src="circleAvatarPreview || circleForm.avatar" />
                  <span v-else class="avatar-fallback">{{ circleForm.name?.charAt(0) || '?' }}</span>
                </div>
                <label class="upload-btn">
                  选择图片
                  <input type="file" accept="image/*" hidden @change="onCircleAvatarFile" />
                </label>
                <button v-if="circleAvatarPreview" class="undo-btn" @click="circleAvatarPreview = ''">撤销</button>
              </div>
            </div>
            <div class="form-row">
              <label>封面</label>
              <div class="upload-row">
                <div class="upload-preview cover-preview-box">
                  <img v-if="circleCoverPreview || circleForm.cover" :src="circleCoverPreview || circleForm.cover" />
                  <div v-else class="cover-fallback"></div>
                </div>
                <label class="upload-btn">
                  选择图片
                  <input type="file" accept="image/*" hidden @change="onCircleCoverFile" />
                </label>
                <button v-if="circleCoverPreview" class="undo-btn" @click="circleCoverPreview = ''">撤销</button>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="cancel-btn" @click="showCircleModal = false">取消</button>
            <button class="submit-btn" @click="submitCircle" :disabled="circleModalLoading">
              {{ circleModalLoading ? '提交中...' : (editingCircleId ? '保存修改' : '创建圈子') }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 圈子板块配置弹窗 -->
    <Teleport to="body">
      <div class="modal-overlay" v-if="showSectionConfigModal" @click.self="showSectionConfigModal = false">
        <div class="modal-card" style="width: 520px;">
          <div class="modal-header">
            <h3>配置板块 - {{ sectionConfigCircle?.name }}</h3>
            <button class="modal-close" @click="showSectionConfigModal = false">&times;</button>
          </div>
          <div class="modal-body">
            <LoadingSpinner v-if="sectionConfigLoading" text="加载中..." />
            <template v-else>
              <p class="modal-hint">勾选该圈子要启用的板块，并选择一个作为默认板块。</p>
              <div class="section-config-list">
                <label
                  v-for="t in sectionTemplates"
                  :key="t.id"
                  class="section-config-item"
                >
                  <span class="section-config-left" @click="toggleSectionCheck(t.id)">
                    <span class="section-config-name">{{ t.name }}</span>
                    <span class="section-config-type">{{ typeLabel(t.applicableType) }}</span>
                  </span>
                  <span class="section-config-right">
                    <div class="sc-toggle-switch" :class="{ on: sectionConfigChecked.has(t.id) }" @click.stop="toggleSectionCheck(t.id)">
                      <div class="sc-toggle-knob"></div>
                    </div>
                    <span class="sc-default-wrap" @click.stop>
                      <input type="radio" name="defaultSection" :checked="sectionConfigDefault === t.id"
                        :disabled="!sectionConfigChecked.has(t.id)" @change="sectionConfigDefault = t.id" />
                      <span class="default-label">默认</span>
                    </span>
                  </span>
                </label>
              </div>
            </template>
          </div>
          <div class="modal-footer">
            <button class="cancel-btn" @click="showSectionConfigModal = false">取消</button>
            <button class="submit-btn" @click="saveSectionConfig" :disabled="sectionConfigLoading">
              {{ sectionConfigLoading ? '保存中...' : '保存配置' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 拒绝原因弹窗 -->
    <Teleport to="body">
      <div class="modal-overlay" v-if="showRejectDialog" @click.self="showRejectDialog = false">
        <div class="modal-card" style="max-width: 420px;">
          <div class="modal-header">
            <h3>拒绝帖子 - {{ rejectTarget?.title }}</h3>
            <button class="modal-close" @click="showRejectDialog = false">&times;</button>
          </div>
          <div class="modal-body">
            <div class="form-row">
              <label>拒绝原因</label>
              <textarea v-model="rejectReason" class="form-textarea" rows="3" placeholder="请填写拒绝原因，会展示给作者"></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button class="cancel-btn" @click="showRejectDialog = false">取消</button>
            <button class="submit-btn op-btn-danger-bg" @click="handleReject">确认拒绝</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 分配角色弹窗 -->
    <Teleport to="body">
      <div class="modal-overlay" v-if="showRoleAssignModal" @click.self="showRoleAssignModal = false">
        <div class="modal-card" style="max-width: 420px;">
          <div class="modal-header">
            <h3>分配角色 - {{ roleAssignTarget?.username }}</h3>
            <button class="modal-close" @click="showRoleAssignModal = false">&times;</button>
          </div>
          <div class="modal-body">
            <div class="form-row">
              <label>全局角色</label>
              <select v-model="roleAssignRoleId" class="form-input">
                <option v-for="r in globalRoles" :key="r.id" :value="r.id">{{ r.label }}（{{ r.code }}）</option>
              </select>
            </div>
          </div>
          <div class="modal-footer">
            <button class="cancel-btn" @click="showRoleAssignModal = false">取消</button>
            <button class="submit-btn" @click="submitRoleAssign">确认分配</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { Icon } from '@iconify/vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useNavigate } from '@/composables/useNavigate'
import { useUserStore } from '@/stores/user'
import { getAdminCircleList, updateCircle as apiUpdateCircle } from '@/api/circle'
import { uploadCircleResource } from '@/api/upload'
import type { CircleVO } from '@/api/circle'
import { listPendingReview, approvePost, rejectPost } from '@/api/post'
import type { PostVO } from '@/api/post'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'
import SearchInput from '@/components/SearchInput.vue'
import ReportManagement from '@/views/admin/ReportManagement.vue'
import AuditLog from '@/views/admin/AuditLog.vue'
import AnalyticsDashboard from '@/views/admin/AnalyticsDashboard.vue'
import CircleApplicationReview from '@/views/admin/CircleApplicationReview.vue'
import CircleJoinApplicationReview from '@/views/admin/CircleJoinApplicationReview.vue'
import request from '@/utils/request'
import { pickApiMessage } from '@/utils/errorCode'

const { to } = useNavigate()
const userStore = useUserStore()

const activeTab = ref<'circles' | 'sectionTemplates' | 'review' | 'users' | 'posts' | 'roles' | 'reports' | 'audit' | 'dashboard' | 'circleApps' | 'joinApps'>('circles')

// 暗色模式
const isDark = ref(false)
function toggleDarkMode() {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('darkMode', isDark.value ? '1' : '0')
}

// ===== 统一搜索 =====
const searchKeyword = ref('')
watch(activeTab, () => { searchKeyword.value = '' })

const searchPlaceholder = computed(() => {
  const map: Record<string, string> = {
    circles: '搜索圈子名称...',
    sectionTemplates: '搜索模板名称...',
    review: '搜索帖子标题或作者...',
    users: '搜索用户名或昵称...',
    posts: '搜索帖子标题...',
    roles: '搜索角色...',
    reports: '搜索举报原因...',
    audit: '搜索操作人或详情...',
    circleApps: '搜索圈子名称...',
    joinApps: '搜索申请理由...',
  }
  return map[activeTab.value] || '搜索...'
})

function refreshCurrentTab() {
  const loader: Record<string, () => void> = {
    circles: loadCircles,
    sectionTemplates: loadSectionTemplates,
    review: loadReviewPosts,
    users: loadUsers,
    posts: loadAdminPosts,
    roles: () => { if (roles.value.length === 0) loadRoles() },
    reports: () => {},
    audit: () => {},
    dashboard: () => {},
    circleApps: () => {},
    joinApps: () => {},
  }
  loader[activeTab.value]?.()
}

// ===== 圈子管理 =====
const circles = ref<CircleVO[]>([])
const circleLoading = ref(false)

const filteredCircles = computed(() => {
  const q = searchKeyword.value.toLowerCase().trim()
  if (!q) return circles.value
  return circles.value.filter(c => c.name.toLowerCase().includes(q))
})

const showCircleModal = ref(false)
const editingCircleId = ref<number | null>(null)
const circleModalLoading = ref(false)
const circleForm = ref({ name: '', intro: '', avatar: '', cover: '' })
const circleAvatarPreview = ref('')
const circleCoverPreview = ref('')

async function loadCircles() {
  circleLoading.value = true
  try { circles.value = await getAdminCircleList() } catch { /* ignore */ }
  finally { circleLoading.value = false }
}

function openCreateCircle() {
  editingCircleId.value = null
  circleForm.value = { name: '', intro: '', avatar: '', cover: '' }
  circleAvatarPreview.value = ''
  circleCoverPreview.value = ''
  showCircleModal.value = true
}

function openEditCircle(c: CircleVO) {
  editingCircleId.value = c.id
  circleForm.value = { name: c.name, intro: c.intro, avatar: c.avatar || '', cover: c.cover || '' }
  circleAvatarPreview.value = ''
  circleCoverPreview.value = ''
  showCircleModal.value = true
}

async function onCircleAvatarFile(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  circleAvatarPreview.value = URL.createObjectURL(file)
  circleModalLoading.value = true
  try {
    const tempId = editingCircleId.value ?? 0
    const url = await uploadCircleResource(file, tempId, 'avatar') as string
    circleForm.value.avatar = url
  } catch { ElMessage.error('头像上传失败') }
  finally { circleModalLoading.value = false }
}

async function onCircleCoverFile(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  circleCoverPreview.value = URL.createObjectURL(file)
  circleModalLoading.value = true
  try {
    const tempId = editingCircleId.value ?? 0
    const url = await uploadCircleResource(file, tempId, 'cover') as string
    circleForm.value.cover = url
  } catch (e) { ElMessage.error(pickApiMessage(e, '封面上传失败')) }
  finally { circleModalLoading.value = false }
}

async function submitCircle() {
  if (!circleForm.value.name) {
    ElMessage.warning('请输入圈子名称')
    return
  }
  circleModalLoading.value = true
  try {
    if (editingCircleId.value) {
      await apiUpdateCircle(editingCircleId.value, {
        name: circleForm.value.name,
        intro: circleForm.value.intro || undefined,
        avatar: circleForm.value.avatar || undefined,
        cover: circleForm.value.cover || undefined,
      })
      ElMessage.success('修改成功')
    } else {
      await request.post('/circle', null, { params: circleForm.value })
      ElMessage.success('创建成功')
    }
    showCircleModal.value = false
    await loadCircles()
  } catch { ElMessage.error('操作失败') }
  finally { circleModalLoading.value = false }
}

async function deleteCircle(c: CircleVO) {
  try {
    await ElMessageBox.confirm(`确定删除圈子"${c.name}"吗？`, '确认删除', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    circleLoading.value = true
    await request.delete(`/circle/${c.id}`)
    ElMessage.success('删除成功')
    await loadCircles()
  } catch (action: any) {
    if (action !== 'cancel') ElMessage.error('删除失败')
  }
  finally { circleLoading.value = false }
}

// ===== 板块模板管理 =====
interface SectionTemplate {
  id: number; name: string; applicableType: string; description: string; sortOrder: number
}
const sectionTemplates = ref<SectionTemplate[]>([])
const sectionTemplateLoading = ref(false)

const filteredSectionTemplates = computed(() => {
  const q = searchKeyword.value.toLowerCase().trim()
  if (!q) return sectionTemplates.value
  return sectionTemplates.value.filter(t => t.name.toLowerCase().includes(q))
})
const showSectionTemplateForm = ref(false)
const editingSectionTemplate = ref<SectionTemplate | null>(null)
const sectionTemplateForm = ref({ name: '', applicableType: 'ALL', description: '', sortOrder: 0 })

async function loadSectionTemplates() {
  sectionTemplateLoading.value = true
  try { sectionTemplates.value = await request.get('/admin/section-template/list') as any || [] } catch { /* ignore */ }
  finally { sectionTemplateLoading.value = false }
}

function editSectionTemplate(t: SectionTemplate) {
  editingSectionTemplate.value = t
  sectionTemplateForm.value = { name: t.name, applicableType: t.applicableType, description: t.description || '', sortOrder: t.sortOrder }
  showSectionTemplateForm.value = true
}

function cancelSectionTemplateForm() {
  showSectionTemplateForm.value = false
  editingSectionTemplate.value = null
  sectionTemplateForm.value = { name: '', applicableType: 'ALL', description: '', sortOrder: 0 }
}

async function submitSectionTemplate() {
  if (!sectionTemplateForm.value.name) { ElMessage.warning('请输入模板名称'); return }
  sectionTemplateLoading.value = true
  try {
    if (editingSectionTemplate.value) {
      await request.put(`/admin/section-template/${editingSectionTemplate.value.id}`, sectionTemplateForm.value)
      ElMessage.success('修改成功')
    } else {
      await request.post('/admin/section-template', sectionTemplateForm.value)
      ElMessage.success('创建成功')
    }
    cancelSectionTemplateForm()
    await loadSectionTemplates()
  } catch { ElMessage.error('操作失败') }
  finally { sectionTemplateLoading.value = false }
}

async function deleteSectionTemplate(t: SectionTemplate) {
  try {
    await ElMessageBox.confirm(`确定删除板块模板"${t.name}"吗？`, '确认删除', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    sectionTemplateLoading.value = true
    await request.delete(`/admin/section-template/${t.id}`)
    ElMessage.success('删除成功')
    await loadSectionTemplates()
  } catch (action: any) {
    if (action !== 'cancel') ElMessage.error('删除失败')
  }
  finally { sectionTemplateLoading.value = false }
}

// ===== 圈子板块配置 =====
const showSectionConfigModal = ref(false)
const sectionConfigCircle = ref<CircleVO | null>(null)
const sectionConfigLoading = ref(false)
const sectionConfigChecked = ref(new Set<number>())
const sectionConfigDefault = ref<number | null>(null)

function typeLabel(type: string) {
  if (type === 'ALL') return '全部'
  if (type === 'NORMAL') return '图文'
  if (type === 'ARTICLE') return '长文'
  return type
}

function roleLabel(role: string) {
  if (role === 'SITE_OWNER') return '站主'
  if (role === 'PLATFORM_ADMIN') return '平台管理员'
  if (role === 'USER') return '普通用户'
  return role || '普通用户'
}

function toggleSectionCheck(templateId: number) {
  const next = new Set(sectionConfigChecked.value)
  if (next.has(templateId)) {
    next.delete(templateId)
    if (sectionConfigDefault.value === templateId) sectionConfigDefault.value = null
  } else {
    next.add(templateId)
    if (sectionConfigDefault.value === null) sectionConfigDefault.value = templateId
  }
  sectionConfigChecked.value = next
}

async function openSectionConfig(circle: CircleVO) {
  sectionConfigCircle.value = circle
  showSectionConfigModal.value = true
  sectionConfigLoading.value = true
  try {
    if (sectionTemplates.value.length === 0) await loadSectionTemplates()
    const sections = await request.get(`/circle/${circle.id}/sections`) as any || []
    sectionConfigChecked.value = new Set(sections.map((s: any) => s.templateId))
    sectionConfigDefault.value = sections.find((s: any) => s.isDefault === 1)?.templateId ?? null
  } catch {
    sectionConfigChecked.value = new Set()
    sectionConfigDefault.value = null
  }
  finally { sectionConfigLoading.value = false }
}

async function saveSectionConfig() {
  if (!sectionConfigCircle.value) return
  if (sectionConfigChecked.value.size === 0) { ElMessage.warning('请至少启用一个板块'); return }
  if (sectionConfigDefault.value === null) { ElMessage.warning('请选择一个默认板块'); return }
  sectionConfigLoading.value = true
  try {
    const configs = sectionTemplates.value
      .filter(t => sectionConfigChecked.value.has(t.id))
      .map((t, i) => ({ templateId: t.id, isDefault: t.id === sectionConfigDefault.value ? 1 : 0, sortOrder: i, status: 1 }))
    await request.put(`/circle/${sectionConfigCircle.value.id}/sections`, configs)
    ElMessage.success('板块配置保存成功')
    showSectionConfigModal.value = false
  } catch { ElMessage.error('保存失败') }
  finally { sectionConfigLoading.value = false }
}

// ===== 内容审核 =====
const reviewPosts = ref<PostVO[]>([])
const reviewLoading = ref(false)

const filteredReviewPosts = computed(() => {
  const q = searchKeyword.value.toLowerCase().trim()
  if (!q) return reviewPosts.value
  return reviewPosts.value.filter(p => p.title?.toLowerCase().includes(q) || p.authorName?.toLowerCase().includes(q))
})
const rejectTarget = ref<PostVO | null>(null)
const rejectReason = ref('')
const showRejectDialog = ref(false)

async function loadReviewPosts() {
  reviewLoading.value = true
  try { reviewPosts.value = (await listPendingReview({ page: 1, size: 100 })).records || [] } catch { /* ignore */ }
  finally { reviewLoading.value = false }
}

async function handleApprove(p: PostVO) {
  try { await approvePost(p.id); ElMessage.success('审核通过'); await loadReviewPosts() } catch { ElMessage.error('操作失败') }
}

function openRejectDialog(p: PostVO) { rejectTarget.value = p; rejectReason.value = ''; showRejectDialog.value = true }

async function handleReject() {
  if (!rejectTarget.value || !rejectReason.value.trim()) { ElMessage.warning('请填写拒绝原因'); return }
  try { await rejectPost(rejectTarget.value.id, rejectReason.value.trim()); ElMessage.success('已拒绝'); showRejectDialog.value = false; rejectTarget.value = null; await loadReviewPosts() } catch { ElMessage.error('操作失败') }
}

// ===== 用户管理 =====
interface AdminUser { id: string; username: string; nickname: string; role: string; status: number }
const users = ref<AdminUser[]>([])
const userLoading = ref(false)

const filteredUsers = computed(() => {
  const q = searchKeyword.value.toLowerCase().trim()
  if (!q) return users.value
  return users.value.filter(u => u.username.toLowerCase().includes(q) || (u.nickname || '').toLowerCase().includes(q))
})

async function loadUsers() {
  userLoading.value = true
  try { users.value = await request.get('/auth/admin/list') as any || [] } catch { users.value = [] }
  finally { userLoading.value = false }
}

async function disableUser(u: AdminUser) {
  try {
    await ElMessageBox.confirm(`确定禁用用户"${u.username}"吗？`, '确认禁用', { confirmButtonText: '禁用', cancelButtonText: '取消', type: 'warning' })
    await request.put(`/auth/admin/${u.id}/disable`)
    ElMessage.success('已禁用'); await loadUsers()
  } catch (action: any) { if (action !== 'cancel') ElMessage.error('操作失败') }
}

async function enableUser(u: AdminUser) {
  try { await request.put(`/auth/admin/${u.id}/enable`); ElMessage.success('已启用'); await loadUsers() } catch { ElMessage.error('操作失败') }
}

// ===== 帖子管理 =====
interface AdminPost {
  id: number; title: string; authorName: string; circleId: number;
  circleName?: string; status: number; createTime: string
}
const adminPosts = ref<AdminPost[]>([])
const adminPostLoading = ref(false)
const adminPostTotal = ref(0)
const adminPostPage = ref(1)
const adminPostStatusFilter = ref<number | null>(null)

const statusLabels: Record<number, string> = {
  0: '草稿', 1: '待审核', 2: '已通过', 3: '已拒绝', 4: '已删除'
}
const statusTagClass: Record<number, string> = {
  0: 'tag-gray', 1: 'tag-blue', 2: 'tag-green', 3: 'tag-red', 4: 'tag-gray'
}

const filteredAdminPosts = computed(() => {
  const q = searchKeyword.value.toLowerCase().trim()
  if (!q) return adminPosts.value
  return adminPosts.value.filter(p => p.title?.toLowerCase().includes(q))
})

async function loadAdminPosts() {
  adminPostLoading.value = true
  try {
    const res = await request.get('/admin/post/list', {
      params: { status: adminPostStatusFilter.value, page: adminPostPage.value, size: 20 }
    }) as any
    adminPosts.value = res.records || []
    adminPostTotal.value = res.total || 0
  } catch { adminPosts.value = [] }
  finally { adminPostLoading.value = false }
}

async function adminDeletePost(p: AdminPost) {
  try {
    await ElMessageBox.confirm(`确定删除帖子"${p.title}"吗？`, '确认删除', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    await request.delete(`/admin/post/${p.id}`)
    ElMessage.success('删除成功')
    await loadAdminPosts()
  } catch (action: any) { if (action !== 'cancel') ElMessage.error('删除失败') }
}

// ===== 角色权限管理 =====
interface Role { id: number; code: string; label: string; scope: string; description: string; builtIn: number; sort: number }
interface Permission { id: number; code: string; label: string; resource: string; action: string }
const roles = ref<Role[]>([])
const permissions = ref<Permission[]>([])
const roleLoading = ref(false)
const selectedRoleId = ref<number | null>(null)
const rolePermissionIds = ref<Set<number>>(new Set())
const rolePermLoading = ref(false)
const rolePermSaving = ref(false)
const collapsedPermGroups = ref(new Set<string>())

function togglePermGroup(resource: string) {
  if (collapsedPermGroups.value.has(resource)) {
    collapsedPermGroups.value.delete(resource)
  } else {
    collapsedPermGroups.value.add(resource)
  }
}

const globalRoles = computed(() => roles.value.filter(r => r.scope === 'GLOBAL'))

const permGroups = computed(() => {
  const groups = new Set<string>()
  permissions.value.forEach(p => groups.add(p.resource))
  return Array.from(groups)
})

const permissionsByResource = computed(() => {
  const map: Record<string, Permission[]> = {}
  permissions.value.forEach(p => {
    if (!map[p.resource]) map[p.resource] = []
    map[p.resource].push(p)
  })
  return map
})

const filteredRoles = computed(() => {
  const q = searchKeyword.value.toLowerCase().trim()
  if (!q) return roles.value
  return roles.value.filter(r => r.label.toLowerCase().includes(q) || r.code.toLowerCase().includes(q))
})

async function loadRoles() {
  roleLoading.value = true
  try {
    const [roleList, permList] = await Promise.all([
      request.get('/auth/admin/roles') as any,
      request.get('/auth/admin/permissions') as any
    ])
    roles.value = roleList || []
    permissions.value = permList || []
    if (roles.value.length > 0 && selectedRoleId.value === null) {
      await selectRole(roles.value[0].id)
    }
  } catch { /* ignore */ }
  finally { roleLoading.value = false }
}

async function selectRole(roleId: number) {
  selectedRoleId.value = roleId
  rolePermLoading.value = true
  try {
    const ids = await request.get(`/auth/admin/roles/${roleId}/permissions`) as any
    rolePermissionIds.value = new Set(ids || [])
  } catch { rolePermissionIds.value = new Set() }
  finally { rolePermLoading.value = false }
}

function togglePermission(permId: number) {
  const next = new Set(rolePermissionIds.value)
  if (next.has(permId)) next.delete(permId)
  else next.add(permId)
  rolePermissionIds.value = next
}

async function saveRolePermissions() {
  if (selectedRoleId.value === null) return
  rolePermSaving.value = true
  try {
    await request.put(`/auth/admin/roles/${selectedRoleId.value}/permissions`, {
      permissionIds: Array.from(rolePermissionIds.value)
    })
    ElMessage.success('权限分配保存成功')
  } catch { ElMessage.error('保存失败') }
  finally { rolePermSaving.value = false }
}

// ===== 用户角色分配 =====
const showRoleAssignModal = ref(false)
const roleAssignTarget = ref<AdminUser | null>(null)
const roleAssignRoleId = ref<number | null>(null)

function openRoleAssign(u: AdminUser) {
  roleAssignTarget.value = u
  const currentRole = roles.value.find(r => r.code === u.role)
  roleAssignRoleId.value = currentRole?.id ?? null
  showRoleAssignModal.value = true
}

async function submitRoleAssign() {
  if (!roleAssignTarget.value || roleAssignRoleId.value === null) return
  try {
    await request.put(`/auth/admin/users/${roleAssignTarget.value.id}/role`, { roleId: roleAssignRoleId.value })
    ElMessage.success('角色分配成功')
    showRoleAssignModal.value = false
    await loadUsers()
  } catch { ElMessage.error('角色分配失败') }
}

// ===== 圈子禁用/启用 =====
async function toggleCircleStatus(c: CircleVO) {
  const action = (c as any).status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}圈子"${c.name}"吗？`, `确认${action}`, { confirmButtonText: action, cancelButtonText: '取消', type: 'warning' })
    await request.put(`/admin/circle/${c.id}/status`, { status: (c as any).status === 1 ? 0 : 1 })
    ElMessage.success(`${action}成功`)
    await loadCircles()
  } catch (action: any) { if (action !== 'cancel') ElMessage.error('操作失败') }
}

// ===== 懒加载 =====
watch(activeTab, (tab) => {
  const loaders: Record<string, () => void> = {
    circles: () => { if (circles.value.length === 0) loadCircles() },
    sectionTemplates: () => { if (sectionTemplates.value.length === 0) loadSectionTemplates() },
    review: () => { if (reviewPosts.value.length === 0) loadReviewPosts() },
    users: () => { if (users.value.length === 0) loadUsers() },
    posts: loadAdminPosts,
    roles: () => { if (roles.value.length === 0) loadRoles() },
    reports: () => {},
    audit: () => {},
    dashboard: () => {},
    circleApps: () => {},
    joinApps: () => {},
  }
  loaders[tab]?.()
})

onMounted(() => {
  isDark.value = localStorage.getItem('darkMode') === '1'
  document.documentElement.classList.toggle('dark', isDark.value)
  loadCircles()
})
</script>

<style scoped>
/* ===== 布局 ===== */
.admin-container {
  display: flex;
  min-height: 100vh;
  background: var(--bg);
}

.admin-sidebar {
  width: 220px;
  background: var(--card);
  border-right: 1px solid var(--border-light);
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  z-index: 100;
}

.sidebar-user {
  padding: 20px 16px;
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  gap: 12px;
}

.sidebar-user-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--gradient-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.sidebar-user-avatar img { width: 100%; height: 100%; object-fit: cover; }

.sidebar-user-fb {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}

.sidebar-user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.sidebar-user-hi { font-size: 11px; color: var(--text-dim); }
.sidebar-user-name { font-size: 14px; font-weight: 700; color: var(--text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sidebar-user-role { font-size: 11px; padding: 1px 8px; border-radius: 6px; font-weight: 500; width: fit-content; }
.role-admin { background: rgba(255, 107, 157, 0.12); color: var(--pink); }
.role-user { background: var(--pink-bg); color: var(--text-dim); }

.sidebar-nav {
  padding: 12px 12px 20px;
  flex: 1;
}

.nav-item {
  width: 100%;
  padding: 10px 14px;
  border-radius: 10px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-dim);
  transition: all 0.2s;
  margin-bottom: 2px;
}

.nav-item:hover {
  background: var(--pink-bg);
  color: var(--pink);
}

.nav-item.active {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(180, 132, 255, 0.1));
  color: var(--pink);
  font-weight: 600;
}

.nav-icon { width: 18px; height: 18px; }

.sidebar-dark-btn {
  margin-left: auto;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: var(--text-dim);
  transition: all 0.2s;
  padding: 0;
}
.sidebar-dark-btn:hover { border-color: var(--pink); color: var(--pink); }

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid var(--border-light);
}

.back-btn {
  width: 100%;
  padding: 8px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-dim);
  transition: all 0.2s;
}

.back-btn:hover { border-color: var(--pink); color: var(--pink); }

.admin-main {
  flex: 1;
  margin-left: 220px;
  padding: 32px;
  min-height: 100vh;
}

.admin-section { }

/* ===== 标题 ===== */
.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-head h2 {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
}

.section-desc {
  font-size: 13px;
  color: var(--text-dim);
  margin: 6px 0 0;
}

.section-head-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 12px;
  background: var(--brand-gradient);
  color: var(--white);
  font-size: 13px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.create-btn:hover { opacity: 0.9; transform: translateY(-1px); }

.toolbar-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--card);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: var(--text-dim);
  transition: all 0.15s;
}

.toolbar-btn:hover { border-color: var(--pink); color: var(--pink); }

/* ===== 表格 ===== */
.table-wrap {
  background: var(--card);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 13px;
}

.data-table thead th {
  padding: 14px 16px;
  text-align: left;
  font-weight: 600;
  color: #fff !important;
  font-size: 12px;
  background: var(--brand) !important;
  letter-spacing: 0.2px;
  white-space: nowrap;
}

.data-table td {
  padding: 12px 16px;
  color: var(--text);
  border-bottom: 1px solid var(--border-light);
  vertical-align: middle;
  white-space: nowrap;
}

/* 斑马纹 */
.data-table tbody tr:nth-child(even) {
  background: rgba(255, 107, 157, 0.02);
}

.data-table tbody tr {
  transition: background 0.12s, transform 0.12s;
  position: relative;
}

.data-table tbody tr:hover {
  background: rgba(255, 107, 157, 0.06);
  transform: scale(1.002);
}

.data-table tbody tr:last-child td { border-bottom: none; }

.td-id {
  color: var(--text-dim);
  font-size: 12px;
  font-family: 'SF Mono', 'Menlo', monospace;
}

.td-name { font-weight: 600; }

.td-intro {
  color: var(--text-dim);
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.td-time { color: var(--text-dim); font-size: 12px; }

.td-actions { display: flex; gap: 12px; }

/* 操作链接 */
.op-link {
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: var(--pink);
  transition: color 0.12s;
}

.op-link:hover { color: var(--purple); }

.op-link-purple { color: #8b5cf6; }
.op-link-purple:hover { color: #7c3aed; }

.op-link-green { color: var(--success); }
.op-link-green:hover { color: #16a34a; }

.op-link-danger { color: var(--error); }
.op-link-danger:hover { color: #dc2626; }

/* 标签 */
.tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.tag-blue { background: rgba(59, 130, 246, 0.12); color: #3b82f6; }
.tag-green { background: rgba(22, 163, 74, 0.1); color: var(--success); }
.tag-red { background: rgba(239, 68, 68, 0.1); color: var(--error); }
.tag-amber { background: rgba(245, 158, 11, 0.12); color: #f59e0b; }
.tag-gray { background: var(--pink-bg); color: var(--text-dim); }

.type-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 500;
  background: var(--bg-secondary, #f3f4f6);
  color: var(--text-dim);
}

/* 32px 头像 */
.avatar-32 {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--gradient-tag);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-32 img { width: 100%; height: 100%; object-fit: cover; }

.avatar-32-fb {
  font-size: 14px;
  font-weight: 700;
  color: #fff;
}

/* ===== 表单卡片 ===== */
.form-card {
  background: var(--card);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  padding: 24px;
  margin-bottom: 20px;
}

.form-card h3 { font-size: 16px; font-weight: 700; color: var(--text); margin: 0 0 18px; }

.form-row { margin-bottom: 16px; }

.form-row label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 6px;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid var(--border);
  font-size: 14px;
  color: var(--text);
  background: var(--bg);
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-input:focus { outline: none; border-color: var(--pink); }

.form-textarea {
  width: 100%;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid var(--border);
  font-size: 14px;
  color: var(--text);
  background: var(--bg);
  resize: vertical;
  font-family: inherit;
  box-sizing: border-box;
}

.form-textarea:focus { outline: none; border-color: var(--pink); }

.form-actions { display: flex; gap: 10px; justify-content: flex-end; padding-top: 8px; }

.cancel-btn {
  padding: 8px 20px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-dim);
}

.submit-btn {
  padding: 8px 24px;
  border-radius: 8px;
  background: var(--gradient-brand);
  color: var(--white);
  font-size: 13px;
  font-weight: 600;
  border: none;
  cursor: pointer;
}

.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }

/* ===== 上传 ===== */
.upload-row { display: flex; align-items: center; gap: 12px; }

.upload-preview {
  border-radius: 10px;
  overflow: hidden;
  background: var(--gradient-tag);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-preview {
  width: 72px;
  height: 72px;
  border-radius: 50%;
}

.cover-preview-box {
  width: 160px;
  height: 80px;
  border-radius: 10px;
}

.upload-preview img { width: 100%; height: 100%; object-fit: cover; }

.cover-fallback {
  width: 100%;
  height: 100%;
  background: var(--gradient-brand);
}

.avatar-fallback {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.upload-btn {
  display: inline-block;
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid var(--border);
  font-size: 13px;
  color: var(--text-dim);
  cursor: pointer;
  transition: all 0.2s;
  background: transparent;
}

.upload-btn:hover { border-color: var(--pink); color: var(--pink); }

.undo-btn {
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: transparent;
  cursor: pointer;
  font-size: 12px;
  color: var(--text-dim);
}

.undo-btn:hover { border-color: var(--error); color: var(--error); }

/* ===== 弹窗 ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-card {
  background: var(--card);
  border-radius: 16px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--border-light);
}

.modal-header h3 { font-size: 16px; font-weight: 700; color: var(--text); margin: 0; }

.modal-close {
  background: none;
  border: none;
  font-size: 22px;
  color: var(--text-dim);
  cursor: pointer;
  line-height: 1;
}

.modal-body { padding: 20px 24px; overflow-y: auto; flex: 1; }

.modal-hint { font-size: 12px; color: var(--text-dim); margin: 0 0 16px; }

.modal-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding: 16px 24px;
  border-top: 1px solid var(--border-light);
}

/* 板块配置列表 */
.section-config-list { display: flex; flex-direction: column; gap: 4px; }

.section-config-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid var(--border-light);
  transition: border-color 0.15s;
  cursor: default;
}

.section-config-item:hover { border-color: var(--purple); background: rgba(180, 132, 255, 0.04); }

.section-config-left { display: flex; align-items: center; gap: 10px; cursor: pointer; flex: 1; }

.section-config-name { font-size: 14px; font-weight: 600; color: var(--text); }

.section-config-type {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  background: var(--bg-secondary, #f3f4f6);
  color: var(--text-dim);
}

.section-config-right { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }

/* 板块拨动开关 */
.sc-toggle-switch {
  width: 40px;
  height: 22px;
  border-radius: 11px;
  background: var(--border);
  position: relative;
  transition: background 0.2s;
  cursor: pointer;
  flex-shrink: 0;
}

.sc-toggle-switch.on { background: var(--gradient-brand); }

.sc-toggle-knob {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  position: absolute;
  top: 2px;
  left: 2px;
  transition: transform 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.15);
}

.sc-toggle-switch.on .sc-toggle-knob { transform: translateX(18px); }

.sc-default-wrap { display: flex; align-items: center; gap: 4px; cursor: pointer; }
.sc-default-wrap input[type="radio"] { accent-color: var(--purple); }

.default-label { font-size: 12px; color: var(--text-dim); }

.op-btn-danger-bg { background: var(--error) !important; }

/* ===== 筛选下拉框 ===== */
.filter-select {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--card);
  font-size: 13px;
  color: var(--text);
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s;
}

.filter-select:focus { border-color: var(--pink); }

/* ===== 分页 ===== */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 20px;
}

.page-btn {
  padding: 6px 16px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--card);
  cursor: pointer;
  font-size: 13px;
  color: var(--text-dim);
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) { border-color: var(--pink); color: var(--pink); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.page-info { font-size: 13px; color: var(--text-dim); }

/* ===== RBAC 角色权限管理 ===== */
.rbac-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  height: calc(100vh - 160px);
}

.rbac-roles {
  width: 280px;
  background: var(--card);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  max-height: 100%;
}

.rbac-roles-hd {
  padding: 14px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: var(--brand);
  border-bottom: none;
  flex-shrink: 0;
}

.rbac-role-list {
  overflow-y: auto;
  flex: 1;
}

.rbac-role-item {
  width: 100%;
  padding: 12px 16px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  text-align: left;
  transition: background 0.15s;
  border-bottom: 1px solid var(--border-light);
}

.rbac-role-item:last-child { border-bottom: none; }
.rbac-role-item:hover { background: var(--pink-bg); }
.rbac-role-item.active { background: linear-gradient(135deg, rgba(255, 107, 157, 0.08), rgba(180, 132, 255, 0.08)); }

.rbac-role-info { display: flex; flex-direction: column; gap: 2px; }
.rbac-role-label { font-size: 14px; font-weight: 600; color: var(--text); }
.rbac-role-code { font-size: 11px; color: var(--text-dim); font-family: 'SF Mono', monospace; }

.rbac-role-scope {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  font-weight: 500;
}

.scope-global { background: rgba(245, 158, 11, 0.12); color: #f59e0b; }
.scope-circle { background: rgba(99, 102, 241, 0.12); color: #6366f1; }

.rbac-perms {
  flex: 1;
  background: var(--card);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: 100%;
}

.rbac-perms-hd {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: var(--brand);
  border-bottom: none;
  flex-shrink: 0;
}

.rbac-perm-groups-scroll {
  overflow-y: auto;
  flex: 1;
  padding: 16px;
}

.rbac-perm-groups { padding: 16px; }

.rbac-perm-group { margin-bottom: 20px; }
.rbac-perm-group:last-child { margin-bottom: 0; }

.rbac-perm-group-hd {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--border-light);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  user-select: none;
  transition: color 0.15s;
}

.rbac-perm-group-hd:hover { color: var(--pink); }

.rbac-group-arrow { font-size: 14px; flex-shrink: 0; }

.rbac-perm-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rbac-perm-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid var(--border-light);
  cursor: pointer;
  transition: all 0.15s;
}

.rbac-perm-toggle:hover { border-color: var(--pink); background: rgba(255, 107, 157, 0.03); }

.rbac-toggle-info { display: flex; align-items: center; gap: 10px; }
.rbac-toggle-label { font-size: 13px; font-weight: 500; color: var(--text); }
.rbac-toggle-code { font-size: 11px; color: var(--text-dim); font-family: 'SF Mono', monospace; }

.rbac-toggle-switch {
  width: 40px;
  height: 22px;
  border-radius: 11px;
  background: var(--border);
  position: relative;
  transition: background 0.2s;
  flex-shrink: 0;
}

.rbac-toggle-switch.on { background: var(--gradient-brand); }

.rbac-toggle-knob {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  position: absolute;
  top: 2px;
  left: 2px;
  transition: transform 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.15);
}

.rbac-toggle-switch.on .rbac-toggle-knob { transform: translateX(18px); }

/* 夜间模式 */
html.dark .op-link-purple { color: #a78bfa; }
html.dark .op-link-purple:hover { color: #c4b5fd; }
html.dark .op-link-green:hover { color: #22c55e; }
html.dark .op-link-danger:hover { color: #f87171; }
html.dark .section-config-item:hover { background: rgba(180, 132, 255, 0.08); }
html.dark .sc-toggle-knob,
html.dark .rbac-toggle-knob { background: #d4d0e8; }
html.dark .sc-toggle-switch { background: #3d3d5c; }
html.dark .rbac-toggle-switch { background: #3d3d5c; }
html.dark .modal-overlay { background: rgba(0, 0, 0, 0.6); }
html.dark .admin-sidebar { border-right-color: var(--border); }
html.dark .cancel-btn:hover { border-color: var(--text-dim); color: var(--text); }
html.dark .upload-btn:hover { border-color: var(--pink); color: var(--pink); }
html.dark .sidebar-dark-btn:hover { border-color: var(--pink-light); color: var(--pink-light); }
html.dark .avatar-32-fb { color: #fff; }
html.dark .sc-default-wrap { color: var(--text-dim); }

</style>
