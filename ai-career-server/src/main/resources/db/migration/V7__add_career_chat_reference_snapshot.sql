ALTER TABLE `career_chat_message`
    ADD COLUMN `references_json` JSON DEFAULT NULL COMMENT '服务端本轮实际提供给模型的知识来源快照；NULL 表示旧消息未验证';
