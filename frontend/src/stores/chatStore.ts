// stores/chatStore.ts
import { defineStore } from 'pinia'
import SockJS from 'sockjs-client'
import { Stomp, CompatClient } from '@stomp/stompjs'
import type { ChatMessage, ChoiceOption, MessageType } from '@/types/chat'

export const useChatStore = defineStore('chat', {
  state: () => ({
    stompClient: null as CompatClient | null,
    messages: [] as ChatMessage[],
    sessionId: '',
    isConnected: false,
    isTyping: false,
    currentStepId: null as number | null,
    currentScenarioId: null as number | null,
    userVariables: {} as Record<string, any>
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
            this.handleServerMessage(chatResponse)
          })
        },
        (error) => {
          console.error('Connection error:', error)
          this.isConnected = false
        }
      )
    },

    handleServerMessage(response: any) {
      // 타이핑 인디케이터 종료
      this.isTyping = false

      // 서버 응답을 ChatMessage로 변환
      const message: ChatMessage = {
        id: Date.now().toString(),
        message: response.message,
        isFromBot: true,
        timestamp: new Date(response.timestamp),
        type: this.determineMessageType(response),
        choices: response.choices || [],
        metadata: {
          stepId: response.currentStepId,
          scenarioId: response.scenarioId,
          variables: response.variables
        }
      }

      this.addMessage(message)

      // 상태 업데이트
      if (response.nextStepId) {
        this.currentStepId = response.nextStepId
      }
      if (response.scenarioId) {
        this.currentScenarioId = response.scenarioId
      }
    },

    determineMessageType(response: any): MessageType {
      if (response.choices && response.choices.length > 0) {
        return 'choice'
      }
      if (response.type === 'error') {
        return 'error'
      }
      if (response.type === 'info') {
        return 'info'
      }
      return 'text'
    },

    sendMessage(message: string) {
      if (!this.isConnected || !this.stompClient) return

      // 일반 텍스트 메시지만 사용자 메시지로 표시
      // (버튼 클릭은 Vue 컴포넌트에서 직접 처리)
      if (!this.isChoiceValue(message)) {
        this.addMessage({
          id: Date.now().toString(),
          message,
          isFromBot: false,
          timestamp: new Date(),
          type: 'text'
        })
      }

      // 타이핑 인디케이터 시작
      this.isTyping = true

      // 서버로 전송
      this.stompClient.send(`/app/chat/${this.sessionId}`, {}, JSON.stringify({
        message,
        sessionId: this.sessionId,
        stepId: this.currentStepId,
        scenarioId: this.currentScenarioId,
        timestamp: new Date().toISOString()
      }))
    },

    startScenario(scenarioId: number) {
      if (!this.isConnected || !this.stompClient) return

      this.isTyping = true
      this.currentScenarioId = scenarioId

      this.stompClient.send(`/app/chat/${this.sessionId}/start`, {}, JSON.stringify({
        scenarioId,
        sessionId: this.sessionId
      }))
    },

    addMessage(message: ChatMessage) {
      this.messages.push(message)

      // 메시지 개수 제한 (성능 고려)
      if (this.messages.length > 100) {
        this.messages = this.messages.slice(-50)
      }
    },

    addWelcomeMessage() {
      this.addMessage({
        id: 'welcome',
        message: `
          안녕하세요! AI 개인비서입니다.

          무엇을 도와드릴까요?

          아래 버튼을 클릭하거나 직접 메시지를 입력해주세요.
          언제든 "메뉴", "도움말", "처음"이라고 말씀하시면 처음으로 돌아갑니다.
        `,
        isFromBot: true,
        timestamp: new Date(),
        type: 'info',
        choices: [
          { value: 'start_demo', label: '시나리오 시작하기' },
          { value: 'help', label: '도움말 보기' },
          { value: 'about', label: '봇 소개' }
        ]
      })
    },

    resetChat() {
      this.messages = []
      this.currentStepId = null
      this.currentScenarioId = null
      this.userVariables = {}
      this.isTyping = false

      // 환영 메시지 다시 표시
      setTimeout(() => {
        this.addWelcomeMessage()
      }, 500)
    },

    isChoiceValue(message: string): boolean {
      // 선택 버튼의 value인지 확인
      const choiceValues = [
        // 기본 선택지
        'start_demo', 'help', 'about',
        // 메인 메뉴
        'schedule', 'memo', 'calculator', 'settings',
        // 일정 관리
        'today', 'tomorrow', 'week', 'add_schedule', 'schedule_detail',
        // 메모 관리
        'create_memo', 'view_memos', 'back_to_memo_menu',
        // 계산기
        'calculate_more', 'calc_history',
        // 설정
        'change_name', 'reset_chat',
        // 공통 네비게이션
        'back_to_menu', 'add_more', 'view_schedule'
      ]

      // 정확한 일치 또는 패턴 기반 확인
      return choiceValues.includes(message) ||
             message.startsWith('back_to_') ||
             message.startsWith('view_') ||
             message.startsWith('add_') ||
             message.endsWith('_menu') ||
             message.endsWith('_more')
    },

    disconnect() {
      if (this.stompClient?.connected) {
        this.stompClient.disconnect()
        this.isConnected = false
      }
    }
  }
})
