DO
$$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_user
      WHERE usename = 'chatbot') THEN
      CREATE USER chatbot WITH PASSWORD 'chatbot2025@';
END IF;
END
$$;

-- 데이터베이스 생성
CREATE DATABASE chatbot_dev OWNER chatbot;
CREATE DATABASE chatbot_test OWNER chatbot;

-- chatbot_dev 데이터베이스에 권한 부여
GRANT ALL PRIVILEGES ON DATABASE chatbot_dev TO chatbot;
GRANT ALL PRIVILEGES ON DATABASE chatbot_test TO chatbot;

-- chatbot_dev 데이터베이스로 전환
\c chatbot_dev;

-- 스키마 생성
CREATE SCHEMA IF NOT EXISTS chatbot AUTHORIZATION chatbot;

-- 기본 테이블 생성 (옵션)
CREATE TABLE IF NOT EXISTS chatbot.health_check (
                                                    id SERIAL PRIMARY KEY,
                                                    status VARCHAR(10),
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 테스트 데이터 삽입
INSERT INTO chatbot.health_check (status) VALUES ('OK');