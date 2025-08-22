
-- 기존 데이터 정리
TRUNCATE TABLE scenario_steps RESTART IDENTITY CASCADE;
TRUNCATE TABLE scenarios RESTART IDENTITY CASCADE;

-- 시나리오 생성
INSERT INTO scenarios (id, bot_id, name, description, is_default, created_at, updated_at)
VALUES (1, 1, '개인비서 서비스', '일정, 메모, 계산 등을 제공하는 개인비서', true, NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;

-- 시나리오 단계들
INSERT INTO scenario_steps (id, scenario_id, step_type, content, input_type, conditions, next_step_id, is_start_step, order_index, created_at, updated_at) VALUES

-- 1. 시작 단계 (사용자 이름 수집)
(1, 1, 'MESSAGE', '안녕하세요! 개인비서입니다.

먼저 성함을 알려주시겠어요?', 'TEXT',
 '{
   "variable_mapping": {
     "target": "userName",
     "validation": "required|min:2|max:10",
     "message": "성함은 2자 이상 10자 이하로 입력해주세요."
   }
 }', 2, true, 1, NOW(), NOW()),

-- 2. 메인 메뉴 (동적 사용자 이름 + 선택 버튼)
(2, 1, 'QUESTION', '${userName}님, 반갑습니다!

어떤 서비스를 이용하시겠어요?', 'CHOICE',
 '{
   "type": "user_choice",
   "choices": [
     {
       "value": "schedule",
       "label": "일정 관리",
       "next_step": 101
     },
     {
       "value": "memo",
       "label": "메모 작성",
       "next_step": 201
     },
     {
       "value": "calculator",
       "label": "계산기",
       "next_step": 301
     },
     {
       "value": "settings",
       "label": "설정",
       "next_step": 401
     }
   ]
 }', null, false, 2, NOW(), NOW()),

-- 101-104: 일정 관리 서브 시나리오
(101, 1, 'MESSAGE', '${userName}님의 일정 관리 서비스입니다.

어떤 일정을 확인하시겠어요?', 'CHOICE',
 '{
   "type": "user_choice",
   "choices": [
     {
       "value": "today",
       "label": "오늘 일정",
       "next_step": 102
     },
     {
       "value": "tomorrow",
       "label": "내일 일정",
       "next_step": 102
     },
     {
       "value": "week",
       "label": "이번 주 일정",
       "next_step": 102
     },
     {
       "value": "add_schedule",
       "label": "일정 추가",
       "next_step": 103
     },
     {
       "value": "back_to_menu",
       "label": "메인 메뉴로",
       "next_step": 2
     }
   ]
 }', null, false, 101, NOW(), NOW()),

(102, 1, 'MESSAGE',
 '${userName}님의 ${today} 일정

 • 09:00 - 팀 미팅
 • 14:00 - 프로젝트 리뷰
 • 16:00 - ChatBot 개발
 • 18:00 - 저녁 약속

 다른 도움이 필요하시면 아래 버튼을 클릭해주세요!', 'CHOICE',
 '{
   "type": "user_choice",
   "choices": [
     {
       "value": "schedule_detail",
       "label": "일정 상세보기",
       "next_step": 102
     },
     {
       "value": "add_schedule",
       "label": "일정 추가",
       "next_step": 103
     },
     {
       "value": "back_to_menu",
       "label": "메인 메뉴로",
       "next_step": 2
     }
   ]
 }', null, false, 102, NOW(), NOW()),

(103, 1, 'MESSAGE', '새로운 일정을 추가하겠습니다.

일정 제목을 입력해주세요:', 'TEXT',
 '{
   "variable_mapping": {
     "target": "scheduleTitle",
     "validation": "required|min:1|max:50"
   }
 }', 104, false, 103, NOW(), NOW()),

(104, 1, 'MESSAGE', '"${scheduleTitle}" 일정이 추가되었습니다!

${userName}님, 추가로 도움이 필요하시면 언제든 말씀해주세요.', 'CHOICE',
 '{
   "type": "user_choice",
   "choices": [
     {
       "value": "add_more",
       "label": "일정 더 추가",
       "next_step": 103
     },
     {
       "value": "view_schedule",
       "label": "일정 확인",
       "next_step": 101
     },
     {
       "value": "back_to_menu",
       "label": "메인 메뉴로",
       "next_step": 2
     }
   ]
 }', null, false, 104, NOW(), NOW()),

-- 201-203: 메모 작성 서브 시나리오
(201, 1, 'MESSAGE', '${userName}님의 메모 작성 서비스입니다.

어떤 작업을 하시겠어요?', 'CHOICE',
 '{
   "type": "user_choice",
   "choices": [
     {
       "value": "create_memo",
       "label": "새 메모 작성",
       "next_step": 202
     },
     {
       "value": "view_memos",
       "label": "저장된 메모 보기",
       "next_step": 203
     },
     {
       "value": "back_to_menu",
       "label": "메인 메뉴로",
       "next_step": 2
     }
   ]
 }', null, false, 201, NOW(), NOW()),

(202, 1, 'MESSAGE', '새로운 메모를 작성해주세요:

(취소하려면 "취소"라고 입력하세요)', 'TEXT',
 '{
   "variable_mapping": {
     "target": "memoContent",
     "validation": "required|min:1|max:200"
   }
 }', 203, false, 202, NOW(), NOW()),

(203, 1, 'MESSAGE', '메모가 저장되었습니다!

${userName}님의 메모: "${memoContent}"

저장 시간: ${now}', 'CHOICE',
 '{
   "type": "user_choice",
   "choices": [
     {
       "value": "create_memo",
       "label": "새 메모 작성",
       "next_step": 202
     },
     {
       "value": "back_to_memo_menu",
       "label": "메모 메뉴로",
       "next_step": 201
     },
     {
       "value": "back_to_menu",
       "label": "메인 메뉴로",
       "next_step": 2
     }
   ]
 }', null, false, 203, NOW(), NOW()),

-- 301-303: 계산기 서브 시나리오
(301, 1, 'MESSAGE', '${userName}님의 계산기 서비스입니다.

계산할 식을 입력해주세요!

예시: 10 + 20, 100 * 5, 50 / 2', 'TEXT',
 '{
   "variable_mapping": {
     "target": "calculation",
     "validation": "required"
   }
 }', 302, false, 301, NOW(), NOW()),

(302, 1, 'MESSAGE', '계산 결과

${calculation} = 42

다른 계산이 필요하시면 아래 버튼을 클릭해주세요!', 'CHOICE',
 '{
   "type": "user_choice",
   "choices": [
     {
       "value": "calculate_more",
       "label": "다른 계산",
       "next_step": 301
     },
     {
       "value": "calc_history",
       "label": "계산 기록",
       "next_step": 303
     },
     {
       "value": "back_to_menu",
       "label": "메인 메뉴로",
       "next_step": 2
     }
   ]
 }', null, false, 302, NOW(), NOW()),

(303, 1, 'MESSAGE', '${userName}님의 계산 기록

최근 계산:
• ${calculation} = 42

(기록 기능은 향후 확장 예정입니다)', 'CHOICE',
 '{
   "type": "user_choice",
   "choices": [
     {
       "value": "calculate_more",
       "label": "새로운 계산",
       "next_step": 301
     },
     {
       "value": "back_to_menu",
       "label": "메인 메뉴로",
       "next_step": 2
     }
   ]
 }', null, false, 303, NOW(), NOW()),

-- 401: 설정 메뉴
(401, 1, 'MESSAGE', '${userName}님의 설정

현재 설정:
• 이름: ${userName}
• 시간: ${now}

설정 기능은 향후 확장될 예정입니다!', 'CHOICE',
 '{
   "type": "user_choice",
   "choices": [
     {
       "value": "change_name",
       "label": "이름 변경",
       "next_step": 1
     },
     {
       "value": "reset_chat",
       "label": "대화 초기화",
       "next_step": 1
     },
     {
       "value": "back_to_menu",
       "label": "메인 메뉴로",
       "next_step": 2
     }
   ]
 }', null, false, 401, NOW(), NOW())

    ON CONFLICT (id) DO NOTHING;

-- 시나리오 시작 단계 설정
UPDATE scenarios SET start_step_id = 1 WHERE id = 1;

-- Step 1이 시작 단계인지 확인 및 재설정
UPDATE scenario_steps SET is_start_step = false WHERE scenario_id = 1;
UPDATE scenario_steps SET is_start_step = true WHERE id = 1 AND scenario_id = 1;

-- 시퀀스 재설정
SELECT setval('scenarios_id_seq', COALESCE((SELECT MAX(id) FROM scenarios), 1));
SELECT setval('scenario_steps_id_seq', COALESCE((SELECT MAX(id) FROM scenario_steps), 401));

-- 기본 데이터 추가 (없는 경우만)
INSERT INTO bots (id, user_id, name, description, created_at, updated_at)
VALUES (1, 1, '개인비서', '일정, 메모, 계산을 도와주는 개인비서입니다.', NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, username, email, created_at, updated_at)
VALUES (1, 'testuser', 'test@example.com', NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;