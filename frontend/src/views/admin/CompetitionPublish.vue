<template>
  <div class="comp-publish">
    <div class="page-header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="font-bold text-lg">{{ pageTitle }}</span>
        </template>
      </el-page-header>
    </div>

    <div class="main-content" v-loading="loading">
      <el-card shadow="never">
        <el-form :model="form" label-width="120px" size="large" ref="formRef" :rules="rules">

          <div class="section-title">1. 赛事基本信息</div>
          <el-form-item label="竞赛名称" prop="title">
            <el-input v-model="form.title" placeholder="请输入赛事名称" />
          </el-form-item>

          <el-form-item label="赛事级别">
            <el-select v-model="form.level">
              <el-option label="国家级" value="nation" />
              <el-option label="省级" value="province" />
              <el-option label="校级" value="school" />
            </el-select>
          </el-form-item>

          <el-form-item label="报名时间" prop="timeRange">
            <el-date-picker
              v-model="form.timeRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="报名开始"
              end-placeholder="报名截止"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 440px"
            />
          </el-form-item>

          <el-form-item v-if="isInternal" label="竞赛/提交时间" prop="compTimeRange">
            <el-date-picker
              v-model="form.compTimeRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="提交开始"
              end-placeholder="提交截止"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 440px"
            />
            <div class="form-tip">到达“提交截止时间”后，系统将自动关闭上传通道并进入评审阶段。</div>
          </el-form-item>

          <div class="section-title mt-6">2. 发布范围限制 (留空则全校可见)</div>
          <el-form-item label="限制学院">
            <el-select v-model="form.limitCollege" placeholder="不限" clearable>
              <el-option label="信息学院" value="信息学院" />
              <el-option label="文学院" value="文学院" />
              <el-option label="物理学院" value="物理学院" />
            </el-select>
          </el-form-item>
          <el-form-item label="限制年级">
            <el-select v-model="form.limitGrade" placeholder="不限" clearable>
              <el-option label="2022级" value="2022级" />
              <el-option label="2023级" value="2023级" />
              <el-option label="2024级" value="2024级" />
              <el-option label="2025级" value="2025级" />
            </el-select>
          </el-form-item>
          <template v-if="!isInternal">
            <div class="section-title mt-6">3. 外部报名设置</div>
            <el-form-item label="官网链接" prop="externalLink">
              <el-input v-model="form.externalLink" placeholder="请输入外部赛事官网地址 (https://...)" />
            </el-form-item>
          </template>

          <template v-else>
            <div class="section-title mt-6">3. 参赛规则设置</div>
            <el-form-item label="参赛对象">
              <el-radio-group v-model="form.mode">
                <el-radio label="individual">个人赛</el-radio>
                <el-radio label="team">团队赛</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="比赛形式">
              <el-radio-group v-model="form.format">
                <el-radio label="online">线上作品赛</el-radio>
                <el-radio label="offline">线下现场赛</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item v-if="form.format === 'offline'" label="比赛地点" prop="location">
              <el-input v-model="form.location" placeholder="例如：体育馆" />
            </el-form-item>

            <el-form-item label="是否纳入综测">
              <el-switch v-model="form.isQualified" active-text="是" inactive-text="否" />
            </el-form-item>
          </template>

          <div class="section-title mt-6">4. 详细说明</div>
          <el-form-item label="赛事详情" prop="description">
            <el-input type="textarea" :rows="4" v-model="form.description" placeholder="请输入详细规则、通知内容..." />
          </el-form-item>

          <el-form-item>
            <el-button type="warning" size="large" :loading="submitting" @click="handleSubmit" class="w-32">
              {{ isEditMode ? '保存修改' : '立即发布' }}
            </el-button>
            <el-button size="large" @click="router.back()">取消</el-button>
          </el-form-item>

        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, watch, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user' // 🟢 引入 Store
import { ElMessage } from 'element-plus'
import { competitionApi } from '@/api/competition'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore() // 🟢 获取用户 ID
const formRef = ref()
const loading = ref(false)
const submitting = ref(false)

const compId = route.query.id
const isEditMode = computed(() => !!compId)
const isInternal = computed(() => route.query.type === 'internal')
const pageTitle = computed(() => isInternal.value ? '发布校内学科竞赛' : '发布官方赛事通知')

const form = reactive<any>({
  title: '',
  level: 'school',
  timeRange: [],
  compTimeRange: [],
  sourceType: route.query.type || 'internal',
  externalLink: '',
  mode: 'team',
  format: 'online',
  location: '',
  isQualified: true,
  description: '',
  limitCollege: '', // 🟢 新增字段
  limitGrade: ''    // 🟢 新增字段
})

watch(isInternal, (val) => { if (val) form.level = 'school' }, { immediate: true })

const rules = computed(() => {
  const baseRules = {
    title: [{ required: true, message: '请输入名称', trigger: 'blur' }],
    timeRange: [{ required: true, message: '请选择报名时间', trigger: 'change' }]
  }
  if (isInternal.value) {
    return {
      ...baseRules,
      compTimeRange: [{ required: true, message: '请选择竞赛/提交时间', trigger: 'change' }]
    }
  }
  return baseRules
})

onMounted(async () => {
  if (isEditMode.value) {
    loading.value = true
    try {
      const data = await competitionApi.getDetail(compId as string)
      if (data) {
        Object.assign(form, data)
        if (data.regStartTime && data.regEndTime) form.timeRange = [data.regStartTime, data.regEndTime]
        if (data.compStartTime && data.compEndTime) form.compTimeRange = [data.compStartTime, data.compEndTime]
      }
    } finally {
      loading.value = false
    }
  }
})

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      if (isInternal.value && form.timeRange && form.compTimeRange) {
        const regEnd = new Date(form.timeRange[1]).getTime()
        const compStart = new Date(form.compTimeRange[0]).getTime()
        if (compStart < regEnd - 60000) {
          ElMessage.warning('建议设置：竞赛开始时间 >= 报名截止时间')
        }
      }

      submitting.value = true
      try {
        const submitData = {
          ...form,
          regStartTime: form.timeRange?.[0],
          regEndTime: form.timeRange?.[1],
          compStartTime: form.compTimeRange?.[0],
          compEndTime: form.compTimeRange?.[1]
        }

        if (isEditMode.value) {
          await competitionApi.update(compId as string, submitData)
          ElMessage.success('修改成功')
        } else {
          // 🟢 核心：创建时传入 userId，后端会自动判断部门权限
          await competitionApi.create(submitData, userStore.id)
          ElMessage.success('发布成功')
        }
        router.back()
      } catch(e: any) {
        console.error(e)
      } finally {
        submitting.value = false
      }
    } else {
      ElMessage.error('请完善表单信息')
    }
  })
}
</script>

<style scoped>
.comp-publish { background: #f5f7fa; min-height: 100vh; padding-bottom: 40px; }
.page-header { background: #fff; padding: 20px; border-bottom: 1px solid #e4e7ed; margin-bottom: 20px; }
.main-content { max-width: 900px; margin: 0 auto; }
.section-title { font-size: 16px; font-weight: bold; border-left: 4px solid #E6A23C; padding-left: 10px; margin-bottom: 20px; color: #303133; }
.mt-6 { margin-top: 1.5rem; }
.w-32 { width: 8rem; }
.text-gray-400 { color: #9ca3af; }
.text-xs { font-size: 12px; }
.form-tip { font-size: 12px; color: #909399; margin-top: 5px; line-height: 1.4; }
</style>
