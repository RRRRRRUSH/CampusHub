<template>
  <div class="my-application">
    <el-card shadow="never">
      <el-tabs v-model="activeTab" class="custom-tabs">

        <el-tab-pane label="我的认定记录" name="list">
          <div class="filter-bar mb-3 flex justify-end">
            <el-button link type="primary" @click="fetchData">
              <el-icon class="mr-1"><Refresh /></el-icon> 刷新
            </el-button>
          </div>
          <el-table :data="list" stripe v-loading="loading">
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.type === 'competition'" type="primary" effect="light">竞赛</el-tag>
                <el-tag v-else type="success" effect="light">志愿</el-tag>
              </template>
            </el-table-column>

            <el-table-column label="来源" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.sourceType === 'internal'" type="info" effect="plain" size="small">校内自动</el-tag>
                <el-tag v-else type="warning" effect="plain" size="small">校外申请</el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="title" label="项目名称" min-width="200" show-overflow-tooltip />

            <el-table-column label="成果/工时" width="150">
              <template #default="{ row }">
                <span v-if="row.type === 'competition'">{{ row.awardLevel || '无' }}</span>
                <span v-else class="text-green-600 font-bold">+{{ row.score }}h</span>
              </template>
            </el-table-column>

            <el-table-column prop="score" label="拟计学分" width="100" align="center">
              <template #default="{ row }">
                <span v-if="row.type === 'competition'" class="font-bold text-blue-600">{{ row.score }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>

            <el-table-column prop="status" label="状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button v-if="row.proofUrl" link type="primary" @click="viewProof(row.proofUrl)">查看凭证</el-button>
                <span v-else class="text-gray-400 text-xs">无附件</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="🏅 校外赛事认定申请" name="apply_comp">
          <div class="apply-form-box">
            <el-alert title="重要提示：仅限申请“官方赛事通知”中发布的校外赛事。如未找到对应赛事，请联系管理员发布通知。" type="warning" show-icon :closable="false" class="mb-4"/>

            <el-form :model="compForm" label-width="100px" ref="compFormRef" :rules="compRules">
              <el-form-item label="选择赛事" prop="originId">
                <el-select
                  v-model="compForm.originId"
                  placeholder="请选择官方发布的校外赛事"
                  style="width: 100%"
                  filterable
                  @change="handleCompetitionSelect"
                >
                  <el-option
                    v-for="comp in externalCompetitions"
                    :key="comp.id"
                    :label="comp.title"
                    :value="comp.id"
                  >
                    <span style="float: left">{{ comp.title }}</span>
                    <span style="float: right; color: #8492a6; font-size: 13px">{{ comp.level }}</span>
                  </el-option>
                </el-select>
              </el-form-item>

              <el-form-item label="获奖等级" prop="awardLevel">
                <el-select v-model="compForm.awardLevel" placeholder="请选择获奖等级" style="width: 100%">
                  <el-option label="国家级一等奖" value="国家级一等奖" />
                  <el-option label="国家级二等奖" value="国家级二等奖" />
                  <el-option label="国家级三等奖" value="国家级三等奖" />
                  <el-option label="省级一等奖" value="省级一等奖" />
                  <el-option label="省级二等奖" value="省级二等奖" />
                  <el-option label="省级三等奖" value="省级三等奖" />
                  <el-option label="校级奖项" value="校级奖项" />
                  <el-option label="参与奖/其他" value="参与奖" />
                </el-select>
              </el-form-item>

              <el-form-item label="证明材料" prop="proofUrl">
                <el-upload
                  class="upload-demo"
                  drag
                  action=""
                  :auto-upload="false"
                  :limit="1"
                  :on-change="(f) => handleFile(f, 'competition')"
                  :show-file-list="false"
                >
                  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                  <div class="el-upload__text">拖拽证书截图到此处，或 <em>点击上传</em></div>
                </el-upload>
                <div v-if="compForm.proofUrl" class="mt-2 text-green-600 text-sm flex items-center">
                  <el-icon class="mr-1"><CircleCheckFilled /></el-icon> 文件已就绪
                </div>
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="submitCompApply" :loading="submitting" class="w-full">提交申请</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="❤️ 校外志愿工时认定" name="apply_vol">
          <div class="apply-form-box">
            <el-alert
              title="自主申报说明"
              type="success"
              :closable="false"
              class="mb-4"
            >
              <template #default>
                <div class="text-xs mt-1 text-gray-500 leading-relaxed">
                  1. 适用于学生<b>自主参加</b>的社区服务、公益活动等（非学校统一组织）。<br>
                  2. 请务必上传带有<b>服务单位盖章</b>的证明材料或志愿北京/志愿中国截图。<br>
                  3. 审核流程：班长初审 -> 认定专员复核 -> 入账。
                </div>
              </template>
            </el-alert>

            <el-form :model="volForm" :rules="volRules" label-width="100px" ref="volFormRef">
              <el-form-item label="活动名称" prop="title">
                <el-input v-model="volForm.title" placeholder="例如：沈阳北站春运引导志愿服务" />
              </el-form-item>

              <el-form-item label="组织单位" prop="organizer">
                <el-input v-model="volForm.organizer" placeholder="例如：沈阳市青年志愿者协会" />
              </el-form-item>

              <el-row :gutter="20">
                <el-col :span="14">
                  <el-form-item label="服务时间" prop="timeRange">
                    <el-date-picker
                      v-model="volForm.timeRange"
                      type="datetimerange"
                      range-separator="至"
                      start-placeholder="开始"
                      end-placeholder="结束"
                      format="YYYY-MM-DD HH:mm"
                      value-format="YYYY-MM-DD HH:mm:ss"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="10">
                  <el-form-item label="申请工时" prop="hours">
                    <el-input-number v-model="volForm.hours" :min="0.5" :step="0.5" style="width: 100%">
                      <template #suffix>小时</template>
                    </el-input-number>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="活动内容" prop="description">
                <el-input
                  v-model="volForm.description"
                  type="textarea"
                  :rows="3"
                  placeholder="简述您的工作内容..."
                />
              </el-form-item>

              <el-form-item label="证明材料" prop="proofUrl">
                <el-upload
                  class="upload-demo"
                  drag
                  action=""
                  :auto-upload="false"
                  :limit="1"
                  :on-change="(f) => handleFile(f, 'volunteer')"
                  :show-file-list="false"
                >
                  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                  <div class="el-upload__text">拖拽证明/盖章文件到此处</div>
                </el-upload>
                <div v-if="volForm.proofUrl" class="mt-2 text-green-600 text-sm flex items-center">
                  <el-icon class="mr-1"><CircleCheckFilled /></el-icon> 文件已就绪
                </div>
              </el-form-item>

              <el-form-item>
                <el-button type="primary" :loading="submitting" @click="submitVolApply" class="w-full">提交申请</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { auditApi } from '@/api/audit'
import { competitionApi } from '@/api/competition'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, CircleCheckFilled, Refresh } from '@element-plus/icons-vue'

const userStore = useUserStore()
const activeTab = ref('list')
const list = ref<any[]>([])
const externalCompetitions = ref<any[]>([])
const loading = ref(false)
const submitting = ref(false)

const compFormRef = ref()
const compForm = reactive({
  type: 'competition',
  originId: undefined,
  title: '',
  awardLevel: '',
  proofUrl: ''
})
const compRules = {
  originId: [{ required: true, message: '请选择赛事', trigger: 'change' }],
  awardLevel: [{ required: true, message: '请选择等级', trigger: 'change' }]
}

const volFormRef = ref()
const volForm = reactive({
  type: 'volunteer',
  title: '',
  organizer: '',
  timeRange: [],
  hours: 2.0,
  description: '',
  proofUrl: ''
})
const volRules = {
  title: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  hours: [{ required: true, message: '工时不能为空', trigger: 'blur' }]
}

// 1. 获取我的记录 (🟢 修复：调用 getMyList)
const fetchData = async () => {
  if (!userStore.id) return
  loading.value = true
  try {
    // 使用 getMyList 而不是 getList('personal')，匹配后端 /audit/list/personal
    // @ts-ignore
    const res = await auditApi.getMyList(userStore.id)
    list.value = Array.isArray(res) ? res : (res.data || [])
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

const fetchExternalCompetitions = async () => {
  try {
    const res = await competitionApi.getStudentList('external', userStore.id)
    const all = Array.isArray(res) ? res : (res.data || [])
    externalCompetitions.value = all.filter((c: any) => c.status !== 'finished')
  } catch(e) { console.error(e) }
}

const handleCompetitionSelect = (val: number) => {
  const target = externalCompetitions.value.find(c => c.id === val)
  if (target) compForm.title = target.title
}

watch(activeTab, (val) => {
  if (val === 'list') fetchData()
  if (val === 'apply_comp') fetchExternalCompetitions()
}, { immediate: true })

const handleFile = (file: any, type: 'competition' | 'volunteer') => {
  const mockUrl = 'https://via.placeholder.com/600x400?text=Uploaded+Proof'
  if (type === 'competition') compForm.proofUrl = mockUrl
  else volForm.proofUrl = mockUrl
  ElMessage.success('文件读取成功 (模拟)')
}

// 提交竞赛申请
const submitCompApply = async () => {
  if (!compFormRef.value) return
  await compFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      if (!compForm.proofUrl) return ElMessage.warning('请上传证明材料')
      submitting.value = true
      try {
        // 🟢 修复：将 userId 放入请求对象
        await auditApi.apply({
          ...compForm,
          userId: userStore.id
        })
        ElMessage.success('申请提交成功')
        compFormRef.value.resetFields()
        compForm.proofUrl = ''
        activeTab.value = 'list'
      } catch(e: any) { ElMessage.error(e.message || '提交失败') }
      finally { submitting.value = false }
    }
  })
}

// 提交志愿申请
const submitVolApply = async () => {
  if (!volFormRef.value) return
  await volFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      if (!volForm.proofUrl) return ElMessage.warning('请上传证明材料')
      submitting.value = true
      try {
        let desc = volForm.description || ''
        if (volForm.timeRange && volForm.timeRange.length === 2) {
          desc = `[服务时间: ${volForm.timeRange[0]} 至 ${volForm.timeRange[1]}] \n` + desc
        }

        // 🟢 修复：将 userId 放入请求对象
        const payload = {
          ...volForm,
          description: desc,
          userId: userStore.id
        }

        await auditApi.apply(payload)
        ElMessage.success('申请提交成功')
        volFormRef.value.resetFields()
        volForm.proofUrl = ''
        activeTab.value = 'list'
      } catch(e: any) { ElMessage.error(e.message || '提交失败') }
      finally { submitting.value = false }
    }
  })
}

const viewProof = (url: string) => {
  ElMessageBox.alert(`<img src="${url}" style="width:100%"/>`, '凭证预览', {
    dangerouslyUseHTMLString: true, customStyle: { maxWidth: '80%' }
  })
}

const getStatusLabel = (s: string) => ({ pending:'待审核', approved:'已通过', rejected:'已驳回' }[s] || s)
const getStatusType = (s: string) => ({ pending:'warning', approved:'success', rejected:'danger' }[s] || 'info')
</script>

<style scoped>
.my-application { max-width: 1200px; margin: 20px auto; padding: 0 20px; }
.apply-form-box { max-width: 600px; margin: 40px auto; }
.text-blue-600 { color: #2563eb; }
.font-bold { font-weight: 700; }
.mb-4 { margin-bottom: 16px; }
.mb-3 { margin-bottom: 12px; }
.mr-1 { margin-right: 4px; }
.w-full { width: 100%; }
.text-xs { font-size: 12px; }
.leading-relaxed { line-height: 1.6; }
.text-green-600 { color: #16a34a; }
</style>
