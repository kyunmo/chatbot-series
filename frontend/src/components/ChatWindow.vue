<!-- components/ChatWindow.vue -->
<template>
  <div class="chat-container">
    <!-- 채팅 헤더 -->
    <div class="chat-header">
      <div class="bot-info">
        <div class="bot-avatar">🤖</div>
        <div class="bot-details">
          <h3>AI 개인비서</h3>
          <span class="status" :class="{ connected: chatStore.isConnected }">
            {{ chatStore.isConnected ? '온라인' : '연결 중...' }}
          </span>
        </div>
      </div>
      <div class="chat-actions">
        <button @click="resetChat" class="reset-btn">
          대화 초기화
        </button>
      </div>
    </div>

    <!-- 메시지 영역 -->
    <div class="messages-container" ref="messagesContainer">
      <div
        v-for="message in chatStore.messages"
        :key="message.id"
        class="message-wrapper"
        :class="{ 'bot-message': message.isFromBot, 'user-message': !message.isFromBot }"
      >
        <!-- 봇 메시지 -->
        <div v-if="message.isFromBot" class="bot-message-container">
          <div class="bot-avatar">🤖</div>
          <div class="message-content">
            <div class="message-text" v-html="formatMessage(message.message)"></div>

            <!-- 선택 버튼들 -->
            <div v-if="message.choices && message.choices.length > 0" class="choice-buttons">
              <button
                v-for="choice in message.choices"
                :key="choice.value"
                @click="handleChoiceClick(choice)"
                class="choice-button"
                :disabled="!chatStore.isConnected"
              >
                <span v-if="choice.emoji" class="choice-emoji">{{ choice.emoji }}</span>
                <span class="choice-label">{{ choice.label }}</span>
              </button>
            </div>

            <div class="message-time">{{ formatTime(message.timestamp) }}</div>
          </div>
        </div>

        <!-- 사용자 메시지 -->
        <div v-else class="user-message-container">
          <div class="message-content">
            <div class="message-text">{{ message.message }}</div>
            <div class="message-time">{{ formatTime(message.timestamp) }}</div>
          </div>
          <div class="user-avatar">👤</div>
        </div>
      </div>

      <!-- 타이핑 인디케이터 -->
      <div v-if="chatStore.isTyping" class="typing-indicator">
        <div class="bot-avatar">🤖</div>
        <div class="typing-dots">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </div>

    <!-- 입력 영역 -->
    <div class="input-container">
      <!-- 빠른 액션 버튼들 -->
      <div class="quick-actions">
        <button @click="startDemo" class="quick-action-btn primary">
          시나리오 시작
        </button>
        <button @click="showHelp" class="quick-action-btn">
          도움말
        </button>
        <button @click="showSettings" class="quick-action-btn">
          설정
        </button>
      </div>

      <!-- 메시지 입력 -->
      <div class="message-input">
        <input
          v-model="currentMessage"
          @keyup.enter="sendMessage"
          @input="handleTyping"
          placeholder="메시지를 입력하세요... (또는 위 버튼을 클릭)"
          :disabled="!chatStore.isConnected"
          class="message-field"
        />
        <button
          @click="sendMessage"
          :disabled="!chatStore.isConnected || !currentMessage.trim()"
          class="send-button"
        >
          <span v-if="!chatStore.isConnected">⏳</span>
          <span v-else>📤</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useChatStore } from '@/stores/chatStore'
import type { ChoiceOption, ChatMessage } from '@/types/chat'

const chatStore = useChatStore()
const currentMessage = ref('')
const messagesContainer = ref<HTMLElement>()
const typingTimeout = ref<NodeJS.Timeout>()

onMounted(async () => {
  await chatStore.connect()
  // 환영 메시지 표시
  chatStore.addWelcomeMessage()
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

const handleChoiceClick = (choice: ChoiceOption) => {
  // 사용자 선택을 메시지로 추가 (버튼 라벨 표시)
  chatStore.addMessage({
    id: Date.now().toString(),
    message: choice.label,
    isFromBot: false,
    timestamp: new Date(),
    type: 'choice'
  })

  // 선택 값을 서버로 전송 (선택지는 사용자 메시지로 표시하지 않음)
  if (!chatStore.isConnected || !chatStore.stompClient) return

  // 타이핑 인디케이터 시작
  chatStore.isTyping = true

  // 서버로 직접 전송 (sendMessage를 거치지 않음)
  chatStore.stompClient.send(`/app/chat/${chatStore.sessionId}`, {}, JSON.stringify({
    message: choice.value,
    sessionId: chatStore.sessionId,
    stepId: chatStore.currentStepId,
    scenarioId: chatStore.currentScenarioId,
    timestamp: new Date().toISOString()
  }))
}

const handleTyping = () => {
  // 타이핑 인디케이터 (간단 구현)
  clearTimeout(typingTimeout.value)
  typingTimeout.value = setTimeout(() => {
    // 타이핑 종료 처리
  }, 1000)
}

const startDemo = () => {
  chatStore.startScenario(1)
}

const resetChat = () => {
  chatStore.resetChat()
}

const showHelp = () => {
  chatStore.addMessage({
    id: Date.now().toString(),
    message: `
      도움말

      기본 사용법:
      • 버튼을 클릭하거나 메시지를 입력하세요
      • "메뉴", "처음", "도움말" 언제든 입력 가능

      주요 기능:
      - 일정 관리: 오늘/내일/이번주 일정 확인
      - 메모 작성: 간단한 메모 저장
      - 계산기: 사칙연산 계산
      - 설정: 봇 설정 변경
    `,
    isFromBot: true,
    timestamp: new Date(),
    type: 'info'
  })
}

const showSettings = () => {
  // 설정 메뉴 표시 (추후 구현)
  chatStore.addMessage({
    id: Date.now().toString(),
    message: "설정 기능은 곧 추가될 예정입니다!",
    isFromBot: true,
    timestamp: new Date(),
    type: 'info'
  })
}

const formatMessage = (message: string) => {
  // 마크다운 스타일 간단 처리
  return message
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/\n/g, '<br>')
}

const formatTime = (date: Date) => {
  return date.toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 700px;
  max-width: 500px;
  margin: 0 auto;
  border: 1px solid #e0e0e0;
  border-radius: 16px;
  background: white;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.bot-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.bot-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
}

.bot-details h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
}

.status {
  font-size: 0.8rem;
  opacity: 0.8;
}

.status.connected {
  color: #4ade80;
}

.reset-btn {
  padding: 0.5rem 1rem;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background 0.2s;
}

.reset-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  background: #f8fafc;
}

.message-wrapper {
  display: flex;
  flex-direction: column;
}

.bot-message-container {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  margin-right: 20%;
}

.user-message-container {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  margin-left: 20%;
  flex-direction: row-reverse;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-text {
  padding: 1rem;
  border-radius: 12px;
  word-break: break-word;
  line-height: 1.5;
}

.bot-message-container .message-text {
  background: white;
  color: #374151;
  border: 1px solid #e5e7eb;
}

.user-message-container .message-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.choice-buttons {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 0.75rem;
}

.choice-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: #f1f5f9;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.95rem;
}

.choice-button:hover:not(:disabled) {
  background: #e2e8f0;
  border-color: #cbd5e1;
  transform: translateY(-1px);
}

.choice-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.choice-emoji {
  font-size: 1.1rem;
}

.choice-label {
  font-weight: 500;
}

.message-time {
  font-size: 0.75rem;
  color: #6b7280;
  margin-top: 0.5rem;
  text-align: right;
}

.user-message-container .message-time {
  color: rgba(255, 255, 255, 0.8);
  text-align: left;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-right: 50%;
}

.typing-dots {
  display: flex;
  gap: 4px;
  padding: 1rem;
  background: white;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.typing-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #9ca3af;
  animation: typing 1.4s infinite ease-in-out both;
}

.typing-dots span:nth-child(1) { animation-delay: -0.32s; }
.typing-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.input-container {
  border-top: 1px solid #e5e7eb;
  padding: 1rem;
  background: white;
}

.quick-actions {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
  flex-wrap: wrap;
}

.quick-action-btn {
  padding: 0.5rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  font-size: 0.85rem;
  transition: all 0.2s;
}

.quick-action-btn.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: transparent;
}

.quick-action-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.message-input {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.message-field {
  flex: 1;
  padding: 0.875rem 1rem;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  font-size: 0.95rem;
  transition: border-color 0.2s;
}

.message-field:focus {
  outline: none;
  border-color: #667eea;
}

.message-field:disabled {
  background: #f9fafb;
  opacity: 0.6;
}

.send-button {
  padding: 0.875rem 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 1.1rem;
  transition: all 0.2s;
  min-width: 48px;
}

.send-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.send-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 스크롤바 스타일링 */
.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.messages-container::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>
