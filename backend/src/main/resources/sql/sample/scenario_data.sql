-- 기존 데이터 삭제 후 재생성
DELETE FROM scenario_steps WHERE scenario_id = 1;
DELETE FROM scenarios WHERE id = 1;

-- 1. 더 완성도 높은 시나리오 생성
INSERT INTO scenarios (id, bot_id, name, description, is_default, created_at, updated_at)
VALUES (1, 1, '개인 비서 서비스', '일정, 메모, 계산 등을 도와주는 종합 서비스', true, NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;

-- 2. 분기가 있는 상세 시나리오 단계들
INSERT INTO scenario_steps (id, scenario_id, step_type, content, input_type, next_step_id, is_start_step, order_index, created_at, updated_at) VALUES
-- 시작 단계
(1, 1, 'MESSAGE', '안녕하세요! 개인 비서 ChatBot입니다. 😊\n무엇을 도와드릴까요?', 'CHOICE', 2, true, 1, NOW(), NOW()),

-- 메인 메뉴
(2, 1, 'QUESTION', '원하시는 서비스를 선택해주세요:\n\n1️⃣ 일정 관리\n2️⃣ 메모 작성\n3️⃣ 간단한 계산\n4️⃣ 도움말 보기\n\n숫자를 입력해주세요.', 'CHOICE', null, false, 2, NOW(), NOW()),

-- 일정 관리 분기 (1번 선택 시)
(3, 1, 'MESSAGE', '📅 일정 관리 서비스입니다.\n어떤 일정을 확인하시겠어요?', 'CHOICE', 4, false, 3, NOW(), NOW()),
(4, 1, 'QUESTION', '일정 옵션을 선택해주세요:\n\n1️⃣ 오늘 일정\n2️⃣ 이번 주 일정\n3️⃣ 특정 날짜 일정\n4️⃣ 메인 메뉴로', 'CHOICE', 5, false, 4, NOW(), NOW()),
(5, 1, 'MESSAGE', '오늘은 2025년 8월 21일입니다.\n📋 오늘의 일정:\n• 10:00 - ChatBot 개발 회의\n• 14:00 - 프로젝트 리뷰\n• 16:00 - 코드 정리\n\n추가로 도움이 필요하시면 언제든 말씀해주세요!', 'TEXT', 2, false, 5, NOW(), NOW()),

-- 메모 작성 분기 (2번 선택 시)
(6, 1, 'MESSAGE', '📝 메모 작성 서비스입니다.\n어떤 메모를 작성하시겠어요?', 'TEXT', 7, false, 6, NOW(), NOW()),
(7, 1, 'QUESTION', '메모 내용을 입력해주세요:\n(취소하려면 "취소"를 입력하세요)', 'TEXT', 8, false, 7, NOW(), NOW()),
(8, 1, 'MESSAGE', '✅ 메모가 저장되었습니다!\n\n언제든 "메모 목록"이라고 말씀하시면 저장된 메모를 확인할 수 있어요.\n\n다른 도움이 필요하시면 메인 메뉴로 돌아가겠습니다.', 'TEXT', 2, false, 8, NOW(), NOW()),

-- 계산 분기 (3번 선택 시)
(9, 1, 'MESSAGE', '🧮 간단한 계산 서비스입니다.\n사칙연산을 도와드릴게요!', 'TEXT', 10, false, 9, NOW(), NOW()),
(10, 1, 'QUESTION', '계산식을 입력해주세요:\n예) 10 + 20, 100 * 5\n(메인 메뉴로 가려면 "메뉴"를 입력하세요)', 'TEXT', 11, false, 10, NOW(), NOW()),
(11, 1, 'MESSAGE', '계산 결과를 알려드렸습니다! 📊\n\n다른 계산이 필요하시면 다시 계산식을 입력해주세요.\n메인 메뉴로 가려면 "메뉴"라고 입력해주세요.', 'TEXT', 10, false, 11, NOW(), NOW()),

-- 도움말 분기 (4번 선택 시)
(12, 1, 'MESSAGE', '❓ 도움말\n\n이 ChatBot은 다음 기능을 제공합니다:\n\n📅 일정 관리 - 오늘/이번주 일정 확인\n📝 메모 작성 - 간단한 메모 저장\n🧮 계산기 - 사칙연산 계산\n\n언제든 "메뉴"라고 입력하시면 메인 메뉴로 돌아갑니다.', 'TEXT', 2, false, 12, NOW(), NOW())

    ON CONFLICT (id) DO NOTHING;

-- 3. 시나리오 단계 연결 설정 (조건부 분기 처리를 위한 업데이트)
-- 메인 메뉴에서 사용자 입력에 따른 분기 설정은 ScenarioService에서 처리

-- 4. 시나리오에 시작 단계 설정
UPDATE scenarios SET start_step_id = 1 WHERE id = 1;

-- 5. 추가 시나리오: 간단한 FAQ
INSERT INTO scenarios (id, bot_id, name, description, is_default, created_at, updated_at)
VALUES (2, 1, '자주 묻는 질문', 'FAQ 기반 빠른 응답', false, NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;

INSERT INTO scenario_steps (id, scenario_id, step_type, content, input_type, next_step_id, is_start_step, order_index, created_at, updated_at) VALUES
                                                                                                                                                   (20, 2, 'MESSAGE', '자주 묻는 질문 모드입니다.\n궁금한 것을 물어보세요!', 'TEXT', 21, true, 1, NOW(), NOW()),
                                                                                                                                                   (21, 2, 'MESSAGE', '답변을 드렸습니다. 추가 질문이 있으시면 언제든 말씀해주세요!', 'TEXT', 21, false, 2, NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;

UPDATE scenarios SET start_step_id = 20 WHERE id = 2;

-- 시퀀스 재설정
SELECT setval('scenarios_id_seq', COALESCE((SELECT MAX(id) FROM scenarios), 2));
SELECT setval('scenario_steps_id_seq', COALESCE((SELECT MAX(id) FROM scenario_steps), 21));