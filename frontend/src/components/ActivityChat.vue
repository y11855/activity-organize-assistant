<!-- 活动群聊组件：拉取历史消息 + WebSocket 实时收发 -->
<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { listChatMessages, sendChatMessage } from '@/api/chat'
import { useUserStore } from '@/stores'
import type { ChatMessageVO } from '@/types'

const props = defineProps<{ activityId: string }>()

const userStore = useUserStore()
const messages = ref<ChatMessageVO[]>([])
const input = ref('')
const sending = ref(false)
const connected = ref(false)
const listRef = ref<HTMLDivElement>()

let socket: WebSocket | null = null
let retryTimer: number | undefined
let closed = false

function scrollToBottom() {
  nextTick(() => {
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  })
}

/** 按消息 ID 去重：本人发送的消息会同时来自接口返回和 WebSocket 广播 */
function append(message: ChatMessageVO) {
  if (messages.value.some((item) => item.id === message.id)) {
    return
  }
  messages.value.push(message)
  scrollToBottom()
}

async function loadHistory() {
  try {
    messages.value = await listChatMessages(props.activityId)
    scrollToBottom()
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

function connect() {
  if (closed) {
    return
  }
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  socket = new WebSocket(
    `${protocol}://${location.host}/api/ws/chat?token=${userStore.token}&activityId=${props.activityId}`
  )
  socket.onopen = () => {
    connected.value = true
  }
  socket.onmessage = (event) => {
    try {
      append(JSON.parse(event.data) as ChatMessageVO)
    } catch {
      // 忽略非 JSON 心跳帧
    }
  }
  socket.onclose = () => {
    connected.value = false
    if (!closed) {
      retryTimer = window.setTimeout(connect, 5000)
    }
  }
}

async function onSend() {
  const content = input.value.trim()
  if (!content || sending.value) {
    return
  }
  sending.value = true
  try {
    const message = await sendChatMessage(props.activityId, content)
    input.value = ''
    append(message)
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    sending.value = false
  }
}

function onKeydown(event: Event) {
  const keyboard = event as KeyboardEvent
  if (keyboard.key === 'Enter' && !keyboard.shiftKey) {
    keyboard.preventDefault()
    onSend()
  }
}

function isMine(message: ChatMessageVO) {
  return String(message.senderId) === String(userStore.userInfo?.id)
}

function formatTime(time: string) {
  return time ? time.replace('T', ' ').slice(5, 16) : ''
}

onMounted(() => {
  loadHistory()
  connect()
})

onUnmounted(() => {
  closed = true
  window.clearTimeout(retryTimer)
  socket?.close()
})
</script>

<template>
  <div class="chat">
    <div class="chat__status">
      <el-tag :type="connected ? 'success' : 'info'" size="small">
        {{ connected ? '实时连接中' : '连接中…' }}
      </el-tag>
      <span class="chat__hint">仅活动参与成员可见</span>
    </div>

    <div ref="listRef" class="chat__list">
      <div
        v-for="message in messages"
        :key="message.id"
        class="chat__item"
        :class="{ 'chat__item--mine': isMine(message) }"
      >
        <div class="chat__meta">
          <span>{{ message.senderName }}</span>
          <span>{{ formatTime(message.createTime) }}</span>
        </div>
        <div class="chat__bubble">{{ message.content }}</div>
      </div>
      <el-empty v-if="!messages.length" description="还没有讨论，说点什么吧" :image-size="60" />
    </div>

    <div class="chat__editor">
      <el-input
        v-model="input"
        type="textarea"
        :rows="2"
        maxlength="500"
        show-word-limit
        placeholder="输入消息，Enter 发送，Shift + Enter 换行"
        @keydown="onKeydown"
      />
      <el-button type="primary" :loading="sending" :disabled="!input.trim()" @click="onSend">
        发送
      </el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 10px;

  &__status {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__hint {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__list {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 12px;
    background: var(--el-fill-color-lighter);
    border-radius: 8px;
  }

  &__item {
    display: flex;
    flex-direction: column;
    margin-bottom: 12px;

    &--mine {
      align-items: flex-end;

      .chat__bubble {
        background: var(--el-color-primary);
        color: #fff;
      }
    }
  }

  &__meta {
    display: flex;
    gap: 8px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-bottom: 4px;
  }

  &__bubble {
    max-width: 80%;
    padding: 8px 12px;
    border-radius: 8px;
    background: #fff;
    color: var(--el-text-color-primary);
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-word;
    border: 1px solid var(--el-border-color-lighter);
  }

  &__editor {
    display: flex;
    gap: 8px;
    align-items: flex-end;
  }
}
</style>
