import { defineStore } from 'pinia'
import SockJS from 'sockjs-client'
import { Stomp, CompatClient } from '@stomp/stompjs'

interface ChatMessage {
  message: string
  isFromBot: boolean
  timestamp: Date
  stepId?: number
}

export const useChatStore = defineStore('chat', {
  state: () => ({
    stompClient: null as CompatClient | null,
    messages: [] as ChatMessage[],
    sessionId: '',
    isConnected: false,
    currentStepId: null as number | null
  }),

  actions: {
    async connect() {
      this.sessionId = `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`

      const socket = new SockJS('http://localhost:5173/ws/chat')
      this.stompClient = Stomp.over(socket)

      this.stompClient.connect({},
        (frame) => {
          console.log('Connected: ' + frame)
          this.isConnected = true

          // 메시지 구독
          this.stompClient?.subscribe(`/topic/chat/${this.sessionId}`, (response) => {
            const chatResponse = JSON.parse(response.body)
            this.addMessage(chatResponse)
          })
        },
        (error) => {
          console.error('Connection error:', error)
          this.isConnected = false
        }
      )
    },

    disconnect() {
      if (this.stompClient?.connected) {
        this.stompClient.disconnect()
        this.isConnected = false
      }
    },

    sendMessage(message: string) {
      if (!this.isConnected || !this.stompClient) return

      // 사용자 메시지 추가
      this.addMessage({
        message,
        isFromBot: false,
        timestamp: new Date()
      })

      // 서버로 전송
      this.stompClient.send(`/app/chat/${this.sessionId}`, {}, JSON.stringify({
        message,
        sessionId: this.sessionId,
        stepId: this.currentStepId,
        timestamp: new Date().toISOString()
      }))
    },

    startScenario(scenarioId: number) {
      if (!this.isConnected || !this.stompClient) return

      this.stompClient.send(`/app/chat/${this.sessionId}/start`, {}, JSON.stringify({
        scenarioId,
        sessionId: this.sessionId
      }))
    },

    addMessage(message: ChatMessage | any) {
      this.messages.push({
        message: message.message,
        isFromBot: message.isFromBot,
        timestamp: new Date(message.timestamp),
        stepId: message.currentStepId
      })

      // 다음 단계 ID 업데이트
      if (message.nextStepId) {
        this.currentStepId = message.nextStepId
      }
    }
  }
})
