-- V10__add_telegram_chat_id_to_posts.sql

ALTER TABLE posts
    ADD COLUMN telegram_chat_id VARCHAR(255);
