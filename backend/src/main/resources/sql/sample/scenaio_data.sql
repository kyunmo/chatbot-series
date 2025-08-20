-- 샘플 데이터: 3편 테스트용

-- 1. 기본 봇 생성
INSERT INTO bots (id, name, description, user_id, is_active, created_at, updated_at) 
VALUES (1, '개인 비서 봇', '일정과 메모를 도와주는 봇', 1, true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 2. 기본 시나리오 생성 (start_step_id는 나중에 설정)
INSERT INTO scenarios (id, bot_id, name, description, is_default, created_at, updated_at) 
VALUES (1, 1, '기본 대화', '일반적인 대화 시나리오', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 3. 시나리오 단계들 생성
INSERT INTO scenario_steps (id, scenario_id, step_type, content, input_type, next_step_id, is_start_step, order_index, created_at, updated_at) VALUES
(1, 1, 'MESSAGE', '안녕하세요! 무엇을 도와드릴까요?', 'CHOICE', 2, true, 1, NOW(), NOW()),
(2, 1, 'QUESTION', '원하시는 서비스를 선택해주세요:\n1. 일정 확인\n2. 메모 작성\n3. 간단한 계산', 'CHOICE', 3, false, 2, NOW(), NOW()),
(3, 1, 'MESSAGE', '일정을 확인해드리겠습니다!', 'TEXT', null, false, 3, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 4. 시나리오에 시작 단계 설정
UPDATE scenarios SET start_step_id = 1 WHERE id = 1;

-- 시퀀스 재설정 (PostgreSQL)
SELECT setval('user_account_id_seq', COALESCE((SELECT MAX(id) FROM user_account), 1));
SELECT setval('bots_id_seq', COALESCE((SELECT MAX(id) FROM bots), 1));
SELECT setval('scenarios_id_seq', COALESCE((SELECT MAX(id) FROM scenarios), 1));
SELECT setval('scenario_steps_id_seq', COALESCE((SELECT MAX(id) FROM scenario_steps), 1));
