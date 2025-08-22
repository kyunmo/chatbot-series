<template>
  <div class="chat-container">
    <!-- 채팅 헤더 -->
    <div class="chat-header">
      <h3>ChatBot Demo</h3>
      <div class="connection-status" :class="{ connected: chatStore.isConnected }">
        {{ chatStore.isConnected ? '연결됨' : '연결 중...' }}
      </div>
    </div>

    <!-- 메시지 영역 -->
    <div class="messages-container" ref="messagesContainer">
      <div
        v-for="(message, index) in chatStore.messages"
        :key="index"
        class="message"
        :class="{ 'bot-message': message.isFromBot, 'user-message': !message.isFromBot }"
      >
        <div class="message-content">
          {{ message.message }}
        </div>
        <div class="message-time">
          {{ formatTime(message.timestamp) }}
        </div>
      </div>
    </div>

    <!-- 입력 영역 -->
    <div class="input-container">
      <div class="quick-actions">
        <button @click="startDemo" class="action-btn">
          시나리오 데모 시작
        </button>
      </div>

      <div class="message-input">
        <input
          v-model="currentMessage"
          @keyup.enter="sendMessage"
          placeholder="메시지를 입력하세요..."
          :disabled="!chatStore.isConnected"
        />
        <button @click="sendMessage" :disabled="!chatStore.isConnected">
          전송
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useChatStore } from '@/stores/chatStore'

const chatStore = useChatStore()
const currentMessage = ref('')
const messagesContainer = ref<HTMLElement>()

onMounted(async () => {
  await chatStore.connect()
})

onUnmounted(() => {
  chatStore.disconnect()
})

// 새 메시지 시 스크롤
watch(() => chatStore.messages.length, async () => {
  await nextTick()
  scrollToBottom()
})

const sendMessage = () => {
  if (!currentMessage.value.trim()) return

  chatStore.sendMessage(currentMessage.value)
  currentMessage.value = ''
}

const startDemo = () => {
  chatStore.startScenario(1) // 기본 시나리오 ID
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const formatTime = (date: Date) => {
  return date.toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 600px;
  max-width: 400px;
  margin: 0 auto;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border-bottom: 1px solid #eee;
  background: #f8f9fa;
}

.connection-status {
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.8rem;
  background: #ff6b6b;
  color: white;
}

.connection-status.connected {
  background: #51cf66;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.message {
  display: flex;
  flex-direction: column;
  max-width: 80%;
}

.bot-message {
  align-self: flex-start;
}

.user-message {
  align-self: flex-end;
}

.message-content {
  padding: 0.75rem;
  border-radius: 8px;
  word-break: break-word;
}

.bot-message .message-content {
  background: #e9ecef;
  color: #495057;
}

.user-message .message-content {
  background: #007bff;
  color: white;
}

.message-time {
  font-size: 0.7rem;
  color: #6c757d;
  margin-top: 0.25rem;
  text-align: right;
}

.input-container {
  border-top: 1px solid #eee;
  padding: 1rem;
}

.quick-actions {
  margin-bottom: 0.5rem;
}

.action-btn {
  padding: 0.5rem 1rem;
  background: #28a745;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
}

.action-btn:hover {
  background: #218838;
}

.message-input {
  display: flex;
  gap: 0.5rem;
}

.message-input input {
  flex: 1;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.message-input button {
  padding: 0.75rem 1rem;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.message-input button:disabled {
  background: #6c757d;
  cursor: not-allowed;
}
</style>
