ALTER TABLE `interview_message`
    ADD COLUMN `score` INT NULL COMMENT '本轮回答内部评分，不向候选人展示',
    ADD COLUMN `evaluation` TEXT NULL COMMENT '本轮回答内部评价，不向候选人展示';
