RENAME TABLE `sys_user` TO `user`;

ALTER TABLE `user`
    RENAME INDEX `uk_sys_user_username` TO `uk_user_username`;
