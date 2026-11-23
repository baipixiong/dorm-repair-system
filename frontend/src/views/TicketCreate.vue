<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">提交报修</h2>
        <p class="page-desc">请尽量详细描述故障情况，方便维修人员提前准备工具与配件。</p>
      </div>
      <el-button :icon="Back" @click="$router.back()">返回</el-button>
    </div>

    <div class="create-grid">
      <el-card shadow="never">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="报修标题" prop="title">
            <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="例如：洗手台水龙头漏水" />
          </el-form-item>

          <div class="form-row">
            <el-form-item label="报修类别" prop="category">
              <el-select v-model="form.category" placeholder="请选择类别" style="width: 100%">
                <el-option v-for="item in CATEGORY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="紧急程度" prop="priority">
              <el-select v-model="form.priority" style="width: 100%">
                <el-option v-for="item in PRIORITY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </div>

          <div class="form-row">
            <el-form-item label="所在楼栋" prop="buildingId">
              <el-select v-model="form.buildingId" placeholder="请选择楼栋" style="width: 100%" @change="onBuildingChange">
                <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="宿舍房间" prop="roomId">
              <el-select v-model="form.roomId" placeholder="请选择房间" style="width: 100%" :disabled="!form.buildingId">
                <el-option v-for="r in rooms" :key="r.id" :label="r.roomNo" :value="r.id" />
              </el-select>
            </el-form-item>
          </div>

          <el-form-item label="故障描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="6"
              maxlength="1000"
              show-word-limit
              placeholder="请描述故障现象、发生时间、影响范围等，例如：水龙头关不紧一直滴水，晚上声音较大。"
            />
          </el-form-item>

          <el-form-item label="联系电话" prop="contactPhone">
            <el-input v-model="form.contactPhone" placeholder="方便维修人员联系" />
          </el-form-item>

          <el-form-item label="现场图片（选填，最多 5 张）">
            <el-upload
              v-model:file-list="fileList"
              list-type="picture-card"
              accept="image/*"
              :auto-upload="false"
              :limit="5"
              :on-exceed="handleExceed"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>

          <div class="form-actions">
            <el-button @click="$router.back()">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="submit">提交报修</el-button>
          </div>
        </el-form>
      </el-card>

      <el-card class="tips-card" shadow="never">
        <div class="tips-title">报修须知</div>
        <ul class="tips-list">
          <li>请准确填写楼栋与房间号，方便维修人员上门。</li>
          <li>水电类紧急故障请选择“紧急”或“特急”，我们会优先处理。</li>
          <li>提交后可在“我的报修”中查看派单与维修进度。</li>
          <li>维修完成后请及时验收，并可对服务进行评价。</li>
          <li>如遇安全隐患（漏电、爆管等），请先联系宿管并注意人身安全。</li>
        </ul>
        <div class="tips-contact">
          <div class="tips-contact-title">后勤服务热线</div>
          <div class="tips-phone">020-3936 6000</div>
          <div class="tips-time">服务时间：08:00 - 22:00</div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Plus } from '@element-plus/icons-vue'
import { createTicket, uploadAttachment } from '../api/ticket'
import { getLocations } from '../api/location'
import { userStore } from '../store/user'
import { CATEGORY_OPTIONS, PRIORITY_OPTIONS } from '../utils/constants'

const router = useRouter()
const formRef = ref()
const buildings = ref([])
const fileList = ref([])
const submitting = ref(false)

const form = reactive({
  title: '',
  category: '',
  priority: 'normal',
  buildingId: null,
  roomId: null,
  description: '',
  contactPhone: ''
})

const rooms = computed(() => {
  const building = buildings.value.find((item) => item.id === form.buildingId)
  return building?.rooms || []
})

const rules = {
  title: [{ required: true, message: '请输入报修标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择报修类别', trigger: 'change' }],
  buildingId: [{ required: true, message: '请选择楼栋', trigger: 'change' }],
  roomId: [{ required: true, message: '请选择房间', trigger: 'change' }],
  description: [{ required: true, message: '请填写故障描述', trigger: 'blur' }]
}

function onBuildingChange() {
  form.roomId = null
}

function handleExceed() {
  ElMessage.warning('最多上传 5 张图片')
}

async function loadLocations() {
  try {
    buildings.value = await getLocations()
    const me = userStore.user
    if (me?.buildingId) {
      form.buildingId = me.buildingId
      if (me?.roomId) {
        form.roomId = me.roomId
      }
    }
  } catch (e) {
    buildings.value = []
  }
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const ticket = await createTicket({
        title: form.title,
        category: form.category,
        priority: form.priority,
        roomId: form.roomId,
        description: form.description,
        contactPhone: form.contactPhone
      })

      const files = fileList.value.map((item) => item.raw).filter(Boolean)
      let failed = 0
      for (const file of files) {
        try {
          await uploadAttachment(ticket.id, file)
        } catch (e) {
          failed += 1
        }
      }

      if (failed > 0) {
        ElMessage.warning(`工单已提交，但有 ${failed} 张图片上传失败`)
      } else {
        ElMessage.success('报修提交成功')
      }
      router.push(`/tickets/${ticket.id}`)
    } finally {
      submitting.value = false
    }
  })
}

onMounted(() => {
  form.contactPhone = userStore.user?.phone || ''
  loadLocations()
})
</script>

<style scoped>
.create-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
  align-items: start;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 6px;
}

.tips-card {
  background: linear-gradient(180deg, #f7faff 0%, #ffffff 60%);
}

.tips-title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 14px;
  color: #1b2437;
}

.tips-list {
  margin: 0;
  padding-left: 18px;
  color: #6b778c;
  font-size: 12.5px;
  line-height: 2;
}

.tips-contact {
  margin-top: 20px;
  padding: 16px;
  border-radius: 12px;
  background: linear-gradient(120deg, #143a7a, #1f5fd6);
  color: #fff;
}

.tips-contact-title {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.tips-phone {
  font-size: 20px;
  font-weight: 700;
  margin: 6px 0 4px;
  letter-spacing: 1px;
}

.tips-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.65);
}

@media (max-width: 1080px) {
  .create-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .form-row {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
