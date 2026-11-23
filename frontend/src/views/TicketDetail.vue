<template>
  <div v-loading="loading" class="page detail-page">
    <template v-if="ticket">
      <div class="detail-head">
        <el-button text :icon="Back" class="back-btn" @click="$router.back()">返回列表</el-button>
        <div class="head-main">
          <div class="head-row">
            <span class="ticket-no">{{ ticket.ticketNo }}</span>
            <el-tag :type="statusType(ticket.status)" effect="light" round>{{ statusLabel(ticket.status) }}</el-tag>
            <el-tag :type="priorityType(ticket.priority)" effect="plain" round>
              {{ priorityLabel(ticket.priority) }}优先级
            </el-tag>
            <el-tag effect="plain" round>{{ categoryLabel(ticket.category) }}</el-tag>
          </div>
          <h2>{{ ticket.title }}</h2>
          <div class="head-meta">
            <span><el-icon><Location /></el-icon>{{ locationText(ticket) }}</span>
            <span><el-icon><User /></el-icon>{{ ticket.studentName || '-' }}</span>
            <span><el-icon><Clock /></el-icon>{{ ticket.createdAt }}</span>
          </div>
        </div>
      </div>

      <div class="detail-grid">
        <div class="main-col">
          <el-card shadow="never">
            <template #header><span class="card-title">工单信息</span></template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="报修人">{{ ticket.studentName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="学号">{{ ticket.studentNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ ticket.contactPhone || ticket.studentPhone || '-' }}</el-descriptions-item>
              <el-descriptions-item label="报修位置">{{ locationText(ticket) }}</el-descriptions-item>
              <el-descriptions-item label="维修人员">
                <span v-if="ticket.assigneeName">{{ ticket.assigneeName }}</span>
                <span v-else class="text-muted">尚未派单</span>
              </el-descriptions-item>
              <el-descriptions-item label="预约上门时间">{{ ticket.scheduledAt || '-' }}</el-descriptions-item>
              <el-descriptions-item label="提交时间">{{ ticket.createdAt }}</el-descriptions-item>
              <el-descriptions-item label="维修完成时间">{{ ticket.finishedAt || '-' }}</el-descriptions-item>
              <el-descriptions-item label="验收完成时间">{{ ticket.completedAt || '-' }}</el-descriptions-item>
              <el-descriptions-item label="最后更新">{{ ticket.updatedAt }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card shadow="never">
            <template #header><span class="card-title">故障描述</span></template>
            <p class="description">{{ ticket.description }}</p>
          </el-card>

          <el-card v-if="ticket.attachments?.length" shadow="never">
            <template #header><span class="card-title">现场图片</span></template>
            <div class="image-list">
              <el-image
                v-for="item in ticket.attachments"
                :key="item.id"
                :src="attachmentUrls[item.id]"
                :preview-src-list="attachmentPreviewList"
                :initial-index="previewIndex(item.id)"
                fit="cover"
                class="image-item"
                preview-teleported
              />
            </div>
          </el-card>

          <el-card shadow="never">
            <template #header><span class="card-title">处理记录</span></template>
            <el-timeline v-if="ticket.events?.length">
              <el-timeline-item
                v-for="event in ticket.events"
                :key="event.id"
                :timestamp="event.createdAt"
                :color="eventColor(event.action)"
                placement="top"
              >
                <div class="event-line">
                  <span class="event-actor">{{ event.actorName || '系统' }}</span>
                  <span class="event-role">{{ event.actorRole }}</span>
                  <span class="event-action">{{ actionLabel(event.action) }}</span>
                  <span v-if="event.fromStatus !== event.toStatus" class="event-transition">
                    {{ statusLabel(event.fromStatus) }} → {{ statusLabel(event.toStatus) }}
                  </span>
                </div>
                <div v-if="event.note" class="event-note">{{ event.note }}</div>
              </el-timeline-item>
            </el-timeline>
            <div v-else class="empty-tip">暂无处理记录</div>
          </el-card>
        </div>

        <div class="side-col">
          <el-card shadow="never" class="action-card">
            <template #header><span class="card-title">工单操作</span></template>

            <template v-if="hasAnyAction">
              <div v-if="canAssign" class="action-block">
                <el-button type="primary" class="block-btn" @click="openAssign">
                  {{ ticket.assigneeId ? '重新派单' : '派单给维修人员' }}
                </el-button>
                <p class="action-tip">选择维修人员并填写预约上门时间</p>
              </div>

              <div v-if="canStart" class="action-block">
                <el-button type="primary" class="block-btn" @click="confirmAction('start', '确认已到达现场并开始维修？', '开始维修')">
                  开始维修
                </el-button>
              </div>

              <div v-if="canFinish" class="action-block">
                <el-button type="success" class="block-btn" @click="openNote('finish', '提交完工', '请填写维修说明，方便学生验收')">
                  提交完工
                </el-button>
              </div>

              <div v-if="canAccept" class="action-block">
                <el-button type="success" class="block-btn" @click="confirmAction('accept', '确认维修已完成并验收通过？', '验收通过')">
                  验收通过
                </el-button>
              </div>

              <div v-if="canReject" class="action-block">
                <el-button type="warning" class="block-btn" @click="openReject">申请返修</el-button>
              </div>

              <div v-if="canEvaluate" class="action-block">
                <el-button type="primary" class="block-btn" @click="openEvaluate">评价本次维修</el-button>
              </div>

              <div v-if="canCancel" class="action-block">
                <el-button class="block-btn" @click="confirmAction('cancel', '确定要取消这条报修吗？', '取消报修')">取消报修</el-button>
              </div>

              <div v-if="canClassify" class="action-block">
                <el-button class="block-btn" @click="openClassify">修改报修分类</el-button>
              </div>

              <div class="action-block">
                <el-button text type="primary" @click="openNote('comment', '添加备注', '备注会记录在工单处理记录中')">
                  添加备注
                </el-button>
              </div>
            </template>

            <div v-else class="empty-tip">当前状态暂无可执行操作</div>
          </el-card>

          <el-card shadow="never">
            <template #header><span class="card-title">处理进度</span></template>
            <el-steps v-if="ticket.status !== 'cancelled'" :active="stepActive" direction="vertical" finish-status="success">
              <el-step title="提交报修" :description="ticket.createdAt" />
              <el-step title="管理员派单" :description="ticket.assigneeName ? `维修人员：${ticket.assigneeName}` : '等待派单'" />
              <el-step title="维修处理" :description="ticket.finishedAt ? '维修已完成' : '等待维修'" />
              <el-step title="学生验收" :description="ticket.completedAt ? '验收完成' : '等待验收'" />
            </el-steps>
            <div v-else class="cancelled-tip">
              <el-icon :size="28"><CircleClose /></el-icon>
              <p>该工单已取消</p>
            </div>
          </el-card>

          <el-card v-if="ticket.evaluation" shadow="never" class="eval-card">
            <template #header><span class="card-title">学生评价</span></template>
            <div class="eval-score">
              <el-rate :model-value="ticket.evaluation.score" disabled show-score text-color="#f59e0b" />
            </div>
            <p class="eval-comment">{{ ticket.evaluation.comment || '学生未填写评价内容' }}</p>
            <div class="eval-meta">{{ ticket.evaluation.studentName }} · {{ ticket.evaluation.createdAt }}</div>
          </el-card>
        </div>
      </div>
    </template>

    <el-dialog v-model="assignVisible" title="派单 / 改派" width="460px">
      <el-form label-position="top">
        <el-form-item label="维修人员" required>
          <el-select v-model="assignForm.assigneeId" placeholder="请选择维修人员" style="width: 100%">
            <el-option
              v-for="tech in technicians"
              :key="tech.id"
              :label="`${tech.realName}（${tech.phone || '无电话'}）`"
              :value="tech.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="预约上门时间">
          <el-date-picker
            v-model="assignForm.scheduledAt"
            type="datetime"
            placeholder="选择日期时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="派单说明">
          <el-input v-model="assignForm.note" type="textarea" :rows="3" placeholder="例如：请携带水管接头配件" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAssign">确认派单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="classifyVisible" title="修改报修分类" width="420px">
      <el-form label-position="top">
        <el-form-item label="报修类别">
          <el-select v-model="classifyForm.category" style="width: 100%">
            <el-option v-for="item in CATEGORY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="classifyForm.note" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classifyVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitClassify">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="noteVisible" :title="noteForm.title" width="440px">
      <el-input
        v-model="noteForm.note"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        :placeholder="noteForm.placeholder"
      />
      <template #footer>
        <el-button @click="noteVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitNote">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="申请返修" width="440px">
      <el-input
        v-model="rejectForm.note"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="请说明需要返修的问题，便于维修人员处理"
      />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="warning" :loading="submitting" @click="submitReject">提交返修</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="evaluateVisible" title="评价本次维修" width="440px">
      <div class="eval-form">
        <el-rate v-model="evaluateForm.score" :texts="['很差', '较差', '一般', '满意', '非常满意']" show-text />
        <el-input
          v-model="evaluateForm.comment"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="说说本次维修体验（选填）"
        />
      </div>
      <template #footer>
        <el-button @click="evaluateVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEvaluate">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Location, User, Clock, CircleClose } from '@element-plus/icons-vue'
import {
  getTicket,
  ticketAction,
  evaluateTicket,
  fetchAttachmentUrl
} from '../api/ticket'
import { getTechnicians } from '../api/user'
import { userStore } from '../store/user'
import {
  STATUS_MAP,
  CATEGORY_OPTIONS,
  ACTION_LABELS,
  locationText,
  statusLabel,
  statusType,
  categoryLabel,
  priorityLabel,
  priorityType
} from '../utils/constants'

const route = useRoute()
const ticket = ref(null)
const loading = ref(false)
const submitting = ref(false)
const technicians = ref([])
const attachmentUrls = reactive({})
let objectUrls = []

const assignVisible = ref(false)
const classifyVisible = ref(false)
const noteVisible = ref(false)
const rejectVisible = ref(false)
const evaluateVisible = ref(false)

const assignForm = reactive({ assigneeId: null, scheduledAt: '', note: '' })
const classifyForm = reactive({ category: '', note: '' })
const noteForm = reactive({ action: 'comment', title: '', placeholder: '', note: '' })
const rejectForm = reactive({ note: '' })
const evaluateForm = reactive({ score: 5, comment: '' })

const activeStatuses = ['pending', 'assigned', 'working', 'review']

const canAssign = computed(() => userStore.isAdmin && ['pending', 'assigned', 'working'].includes(ticket.value?.status))
const canClassify = computed(() => userStore.isAdmin && activeStatuses.includes(ticket.value?.status))
const canStart = computed(() => userStore.isTechnician && ticket.value?.status === 'assigned')
const canFinish = computed(() => userStore.isTechnician && ticket.value?.status === 'working')
const canCancel = computed(() => userStore.isStudent && ticket.value?.status === 'pending')
const canAccept = computed(() => userStore.isStudent && ticket.value?.status === 'review')
const canReject = computed(() => userStore.isStudent && ticket.value?.status === 'review')
const canEvaluate = computed(
  () => userStore.isStudent && ticket.value?.status === 'completed' && !ticket.value?.evaluation
)

const hasAnyAction = computed(
  () => canAssign.value || canClassify.value || canStart.value || canFinish.value ||
    canCancel.value || canAccept.value || canReject.value || canEvaluate.value
)

const stepActive = computed(() => {
  const map = { pending: 0, assigned: 1, working: 2, review: 3, completed: 4, cancelled: 0 }
  return map[ticket.value?.status] ?? 0
})

const attachmentPreviewList = computed(() =>
  (ticket.value?.attachments || []).map((item) => attachmentUrls[item.id]).filter(Boolean)
)

function previewIndex(id) {
  return attachmentPreviewList.value.indexOf(attachmentUrls[id])
}

function actionLabel(action) {
  return ACTION_LABELS[action] || action
}

function eventColor(action) {
  const map = {
    create: '#1f5fd6',
    assign: '#f59e0b',
    start: '#d97706',
    finish: '#7c3aed',
    accept: '#0f9f6e',
    reject: '#ef4444',
    cancel: '#94a3b8',
    comment: '#94a3b8',
    classify: '#12b3a8',
    evaluate: '#eab308'
  }
  return map[action] || '#c6d1e2'
}

async function loadTicket() {
  loading.value = true
  try {
    ticket.value = await getTicket(route.params.id)
    await loadAttachmentUrls()
  } finally {
    loading.value = false
  }
}

async function loadAttachmentUrls() {
  Object.values(attachmentUrls).forEach((url) => URL.revokeObjectURL(url))
  Object.keys(attachmentUrls).forEach((key) => delete attachmentUrls[key])
  objectUrls = []
  for (const item of ticket.value?.attachments || []) {
    try {
      const url = await fetchAttachmentUrl(item.id)
      attachmentUrls[item.id] = url
      objectUrls.push(url)
    } catch (e) {
      // 单张图片加载失败不影响页面
    }
  }
}

async function doAction(action, payload = {}) {
  submitting.value = true
  try {
    const data = await ticketAction(ticket.value.id, {
      action,
      version: ticket.value.version,
      ...payload
    })
    ticket.value = data
    ElMessage.success('操作成功')
    await loadAttachmentUrls()
  } catch (e) {
    await loadTicket()
  } finally {
    submitting.value = false
  }
}

async function confirmAction(action, message, title) {
  try {
    await ElMessageBox.confirm(message, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await doAction(action)
  } catch (e) {
    // 用户取消或请求失败
  }
}

async function openAssign() {
  assignForm.assigneeId = ticket.value.assigneeId || null
  assignForm.scheduledAt = ticket.value.scheduledAt || ''
  assignForm.note = ''
  assignVisible.value = true
  if (technicians.value.length === 0) {
    try {
      technicians.value = await getTechnicians()
    } catch (e) {
      technicians.value = []
    }
  }
}

async function submitAssign() {
  if (!assignForm.assigneeId) {
    ElMessage.warning('请选择维修人员')
    return
  }
  await doAction('assign', {
    assigneeId: assignForm.assigneeId,
    scheduledAt: assignForm.scheduledAt || null,
    note: assignForm.note
  })
  assignVisible.value = false
}

function openClassify() {
  classifyForm.category = ticket.value.category
  classifyForm.note = ''
  classifyVisible.value = true
}

async function submitClassify() {
  await doAction('classify', { category: classifyForm.category, note: classifyForm.note })
  classifyVisible.value = false
}

function openNote(action, title, placeholder) {
  noteForm.action = action
  noteForm.title = title
  noteForm.placeholder = placeholder
  noteForm.note = ''
  noteVisible.value = true
}

async function submitNote() {
  if (!noteForm.note.trim()) {
    ElMessage.warning('请填写内容')
    return
  }
  await doAction(noteForm.action, { note: noteForm.note })
  noteVisible.value = false
}

function openReject() {
  rejectForm.note = ''
  rejectVisible.value = true
}

async function submitReject() {
  if (!rejectForm.note.trim()) {
    ElMessage.warning('请填写返修原因')
    return
  }
  await doAction('reject', { note: rejectForm.note })
  rejectVisible.value = false
}

function openEvaluate() {
  evaluateForm.score = 5
  evaluateForm.comment = ''
  evaluateVisible.value = true
}

async function submitEvaluate() {
  submitting.value = true
  try {
    await evaluateTicket(ticket.value.id, {
      score: evaluateForm.score,
      comment: evaluateForm.comment
    })
    ElMessage.success('评价成功，感谢你的反馈')
    evaluateVisible.value = false
    await loadTicket()
  } finally {
    submitting.value = false
  }
}

onMounted(loadTicket)

onBeforeUnmount(() => {
  objectUrls.forEach((url) => URL.revokeObjectURL(url))
})
</script>

<style scoped>
.detail-page {
  min-height: 60vh;
}

.back-btn {
  margin-bottom: 6px;
  color: #6b778c;
}

.detail-head {
  margin-bottom: 16px;
}

.head-main {
  background: #fff;
  border: 1px solid #e8eef7;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 6px 24px rgba(21, 44, 84, 0.05);
}

.head-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.ticket-no {
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 13px;
  font-weight: 700;
  color: #1f5fd6;
}

.head-main h2 {
  margin: 10px 0 8px;
  font-size: 21px;
  color: #17233a;
}

.head-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  font-size: 12.5px;
  color: #7a869c;
}

.head-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 310px;
  gap: 16px;
  align-items: start;
}

.main-col,
.side-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 700;
  color: #1b2437;
}

.description {
  margin: 0;
  line-height: 1.9;
  color: #43506a;
  white-space: pre-wrap;
  font-size: 13.5px;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-item {
  width: 132px;
  height: 132px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e8eef7;
}

.event-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 13px;
}

.event-actor {
  font-weight: 600;
  color: #1b2437;
}

.event-role {
  font-size: 11px;
  color: #7a869c;
  background: #f2f5fa;
  padding: 1px 7px;
  border-radius: 999px;
}

.event-action {
  color: #1f5fd6;
  font-weight: 500;
}

.event-transition {
  font-size: 11.5px;
  color: #93a0b5;
}

.event-note {
  margin-top: 6px;
  font-size: 12.5px;
  color: #6b778c;
  background: #f8faff;
  border-left: 3px solid #d5e2fa;
  padding: 8px 12px;
  border-radius: 0 8px 8px 0;
  white-space: pre-wrap;
}

.action-block {
  margin-bottom: 10px;
}

.block-btn {
  width: 100%;
}

.action-tip {
  margin: 6px 2px 0;
  font-size: 11.5px;
  color: #93a0b5;
}

.cancelled-tip {
  text-align: center;
  padding: 24px 0 10px;
  color: #93a0b5;
}

.cancelled-tip p {
  margin: 8px 0 0;
  font-size: 13px;
}

.eval-score {
  margin-bottom: 10px;
}

.eval-comment {
  margin: 0 0 10px;
  font-size: 13px;
  line-height: 1.8;
  color: #43506a;
}

.eval-meta {
  font-size: 11.5px;
  color: #93a0b5;
}

.eval-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@media (max-width: 1080px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .head-main {
    padding: 16px;
  }

  .head-main h2 {
    font-size: 18px;
  }

  .image-item {
    width: 96px;
    height: 96px;
  }
}
</style>
