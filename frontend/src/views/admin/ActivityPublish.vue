<template>
  <div class="activity-publish">
    <div class="page-header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="font-bold text-lg">{{ pageTitle }}</span>
        </template>
      </el-page-header>
    </div>

    <div class="main-content" v-loading="loading">
      <el-card shadow="never" class="form-card">
        <el-form :model="form" label-width="120px" size="large" ref="formRef" :rules="rules">

          <div class="section-title"><span class="step-num">01</span> 基本信息</div>

          <el-form-item label="通知标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入活动标题 (例如：2025春季图书馆志愿服务)" />
          </el-form-item>

          <template v-if="isInternal">
            <el-form-item label="举办形式" prop="format">
              <el-radio-group v-model="form.format">
                <el-radio-button label="offline"><el-icon class="mr-1"><Location /></el-icon> 线下</el-radio-button>
                <el-radio-button label="online"><el-icon class="mr-1"><Monitor /></el-icon> 线上</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item v-if="form.format === 'offline'" label="活动地点" prop="location">
              <el-input v-model="form.location" placeholder="请填写入场地点 (如：图书馆一楼大厅)">
                <template #append><el-checkbox v-model="form.needPhoto" label="需传现场图" /></template>
              </el-input>
            </el-form-item>

            <div class="section-title mt-8"><span class="step-num">02</span> 时间与名额</div>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="报名时间" required>
                  <div class="flex items-center w-full">
                    <el-form-item prop="regStartTime" class="flex-1 mb-0">
                      <el-date-picker
                        v-model="form.regStartTime"
                        type="datetime"
                        placeholder="开始"
                        format="YYYY-MM-DD HH:mm"
                        value-format="YYYY-MM-DD HH:mm:ss"
                        style="width: 100%"
                      />
                    </el-form-item>
                    <span class="mx-2 text-gray-400">至</span>
                    <el-form-item prop="regEndTime" class="flex-1 mb-0">
                      <el-date-picker
                        v-model="form.regEndTime"
                        type="datetime"
                        placeholder="截止"
                        format="YYYY-MM-DD HH:mm"
                        value-format="YYYY-MM-DD HH:mm:ss"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </div>
                </el-form-item>
              </el-col>

              <el-col :span="12">
                <el-form-item label="活动时间" required>
                  <div class="flex items-center w-full">
                    <el-form-item prop="activityStartTime" class="flex-1 mb-0">
                      <el-date-picker
                        v-model="form.activityStartTime"
                        type="datetime"
                        placeholder="开始"
                        format="YYYY-MM-DD HH:mm"
                        value-format="YYYY-MM-DD HH:mm:ss"
                        style="width: 100%"
                      />
                    </el-form-item>
                    <span class="mx-2 text-gray-400">至</span>
                    <el-form-item prop="activityEndTime" class="flex-1 mb-0">
                      <el-date-picker
                        v-model="form.activityEndTime"
                        type="datetime"
                        placeholder="结束"
                        format="YYYY-MM-DD HH:mm"
                        value-format="YYYY-MM-DD HH:mm:ss"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="认定工时" prop="hours">
                  <el-input-number v-model="form.hours" :step="0.5" :min="0" class="w-full">
                    <template #suffix>小时</template>
                  </el-input-number>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="名额限制" prop="quota">
                  <div class="flex items-center w-full">
                    <el-input-number
                      v-model="form.quota"
                      :min="1"
                      class="flex-1"
                      :disabled="form.isUnlimited"
                      placeholder="人数"
                    />
                    <el-checkbox v-model="form.isUnlimited" label="不限人数" class="ml-3" border />
                  </div>
                </el-form-item>
              </el-col>
            </el-row>

            <div class="section-title mt-8"><span class="step-num">03</span> 报名限制 (可见范围)</div>

            <el-form-item label="限制校区">
              <el-checkbox-group v-model="form.limitCampus">
                <el-checkbox label="蒲河校区" />
                <el-checkbox label="崇山校区" />
                <el-checkbox label="武圣校区" />
              </el-checkbox-group>
              <div class="text-xs text-gray-400 ml-2 mt-1">* 不选则默认面向全校</div>
            </el-form-item>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="限制学院">
                  <el-select v-model="form.limitCollege" multiple placeholder="请选择学院 (默认全选)" collapse-tags clearable class="w-full">
                    <el-option v-for="c in collegeOptions" :key="c" :label="c" :value="c" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="限制年级">
                  <el-select v-model="form.limitGrade" multiple placeholder="请选择年级 (默认全选)" collapse-tags clearable class="w-full">
                    <el-option v-for="g in gradeOptions" :key="g" :label="g + '级'" :value="g" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <div class="section-title mt-8">
            <span class="step-num">{{ isInternal ? '04' : '02' }}</span> 详细内容
          </div>
          <el-form-item prop="desc">
            <el-input type="textarea" :rows="8" v-model="form.desc" placeholder="请输入活动详细介绍、注意事项等..." />
          </el-form-item>

          <div class="form-actions">
            <el-button size="large" @click="router.back()">取消</el-button>
            <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
              {{ isEditMode ? '保存修改' : '立即发布' }}
            </el-button>
          </div>

        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Location, Monitor } from '@element-plus/icons-vue'
import { activityApi } from '@/api/activity'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const loading = ref(false)
const submitting = ref(false)

const collegeOptions = ['信息学院', '商学院', '机械学院', '文学院', '理学院']
const gradeOptions = ['2021', '2022', '2023', '2024', '2025']

const activityId = route.query.id
const isEditMode = computed(() => !!activityId)
const isInternal = computed(() => route.query.type === 'internal')

const pageTitle = computed(() => {
  const action = isEditMode.value ? '编辑' : '发布'
  const type = isInternal.value ? '校内志愿活动' : '校外实践通知'
  return `${action}${type}`
})

const form = reactive<any>({
  title: '',
  sourceType: route.query.type || 'internal',
  format: 'offline',
  location: '',
  needPhoto: false,

  // 🟢 改为独立字段，不再使用数组
  regStartTime: '',
  regEndTime: '',
  activityStartTime: '',
  activityEndTime: '',

  hours: 2.0,
  quota: 50,
  isUnlimited: false,

  limitCampus: [],
  limitCollege: [],
  limitGrade: [],

  desc: ''
})

const rules = computed(() => {
  const base = { title: [{ required: true, message: '请输入标题', trigger: 'blur' }] }
  if (isInternal.value) {
    return {
      ...base,
      // 🟢 针对独立字段的校验规则
      regStartTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
      regEndTime: [{ required: true, message: '请选择截止时间', trigger: 'change' }],
      activityStartTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
      activityEndTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
      location: [{ required: form.format === 'offline', message: '线下活动请填写地点', trigger: 'blur' }]
    }
  }
  return base
})

onMounted(async () => {
  if (isEditMode.value) {
    loading.value = true
    try {
      const data = await activityApi.getDetail(activityId as string)
      if (data) {
        Object.assign(form, data)
        // 回显逻辑优化：直接赋值即可，无需拆解数组
        if (data.quota <= 0) {
          form.isUnlimited = true
          form.quota = 1
        }
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
      submitting.value = true
      try {
        const submitData = {
          ...form,
          // 处理名额
          quota: form.isUnlimited ? -1 : form.quota
        }

        if (isEditMode.value) {
          await activityApi.update(activityId as string, submitData)
          ElMessage.success('修改成功')
        } else {
          await activityApi.create(submitData)
          ElMessage.success('发布成功')
        }
        router.back()
      } finally {
        submitting.value = false
      }
    }
  })
}
</script>

<style scoped>
.activity-publish { background: #f5f7fa; min-height: 100vh; padding-bottom: 40px; }
.page-header { background: #fff; padding: 20px; border-bottom: 1px solid #e4e7ed; margin-bottom: 20px; }
.main-content { max-width: 900px; margin: 0 auto; }
.form-card { padding: 20px; }

.section-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 20px;
  border-bottom: 1px dashed #eee;
  padding-bottom: 10px;
  display: flex;
  align-items: center;
}
.step-num {
  background: #e6f0ff;
  color: #0056D2;
  font-size: 14px;
  padding: 2px 8px;
  border-radius: 4px;
  margin-right: 10px;
  font-family: monospace;
}

.mt-8 { margin-top: 2rem; }
.ml-3 { margin-left: 12px; }
.mx-2 { margin: 0 8px; }
.w-full { width: 100%; }
.flex-1 { flex: 1; }
.mb-0 { margin-bottom: 0 !important; }
.form-actions { margin-top: 40px; text-align: center; border-top: 1px solid #f0f0f0; padding-top: 20px; }
.mr-1 { margin-right: 4px; }
</style>
