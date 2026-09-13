-- 每份面试报告的分项能力只允许写回一次，避免重复请求再次叠加权重。
ALTER TABLE `ability_score`
    ADD UNIQUE KEY `uk_ability_score_interview_source` (`source_type`, `source_id`, `ability_name`);

-- source_interview_id 为 NULL 的首版规划不受影响；同一面试不能生成多个新版本。
ALTER TABLE `career_plan`
    ADD UNIQUE KEY `uk_career_plan_user_source_interview` (`user_id`, `source_interview_id`),
    ADD UNIQUE KEY `uk_career_plan_user_version` (`user_id`, `version`);
