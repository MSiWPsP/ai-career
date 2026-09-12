ALTER TABLE `interview`
    ADD COLUMN `max_questions` INT NOT NULL DEFAULT 10 AFTER `question_count`;
