<script setup lang="ts">
import { ref } from 'vue'
import { CameraFilled, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type UploadProps, type UploadRequestOptions } from 'element-plus'
import { removeCurrentUserAvatar, uploadCurrentUserAvatar } from '../api/user'
import type { UserInfo } from '../types/api'

const props = defineProps<{
  avatar?: string
  nickname: string
}>()

const emit = defineEmits<{
  uploaded: [user: UserInfo]
}>()

const visible = ref(false)
const uploading = ref(false)
const removing = ref(false)

function open() {
  visible.value = true
}

const beforeUpload: UploadProps['beforeUpload'] = (rawFile) => {
  if (!['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(rawFile.type)) {
    ElMessage.warning('头像仅支持 JPG、PNG、WebP 或 GIF 格式')
    return false
  }
  if (rawFile.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像大小不能超过 5MB')
    return false
  }
  return true
}

async function upload(options: UploadRequestOptions) {
  uploading.value = true
  try {
    const updatedUser = await uploadCurrentUserAvatar(options.file)
    emit('uploaded', updatedUser)
    ElMessage.success('头像上传成功')
    visible.value = false
    return updatedUser
  } finally {
    uploading.value = false
  }
}

async function removeAvatar() {
  try {
    await ElMessageBox.confirm('移除后将恢复为昵称首字头像，确定继续吗？', '移除头像', {
      type: 'warning',
      confirmButtonText: '确定移除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  removing.value = true
  try {
    const updatedUser = await removeCurrentUserAvatar()
    emit('uploaded', updatedUser)
    ElMessage.success('已恢复默认头像')
    visible.value = false
  } finally {
    removing.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" title="更换头像" width="430px" align-center destroy-on-close>
    <div class="avatar-dialog-body">
      <el-avatar :size="92" :src="avatar">{{ nickname.slice(0, 1) }}</el-avatar>
      <div>
        <h3>上传新的个人头像</h3>
        <p>选择清晰的正方形图片，支持 JPG、PNG、WebP、GIF，最大 5MB。</p>
      </div>
    </div>

    <el-upload
      action="#"
      drag
      :show-file-list="false"
      :before-upload="beforeUpload"
      :http-request="upload"
      accept="image/jpeg,image/png,image/webp,image/gif"
      :disabled="uploading"
    >
      <el-icon class="avatar-upload-icon" :class="{ 'is-loading': uploading }">
        <UploadFilled v-if="!uploading" />
        <CameraFilled v-else />
      </el-icon>
      <div class="el-upload__text">
        {{ uploading ? '正在上传到 OSS…' : '拖动图片到这里，或点击选择' }}
      </div>
    </el-upload>

    <div v-if="avatar" class="avatar-dialog-actions">
      <el-button type="danger" text :loading="removing" @click="removeAvatar">移除当前头像</el-button>
    </div>
  </el-dialog>
</template>

<style scoped>
.avatar-dialog-body {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
  padding: 4px 2px;
}

.avatar-dialog-body h3 {
  margin: 0 0 7px;
  font-size: 17px;
}

.avatar-dialog-body p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.7;
}

.avatar-upload-icon {
  margin-bottom: 10px;
  color: var(--primary);
  font-size: 38px;
}

.avatar-dialog-actions {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

@media (max-width: 520px) {
  .avatar-dialog-body {
    align-items: flex-start;
  }
}
</style>
