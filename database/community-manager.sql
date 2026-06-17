CREATE TABLE community_follow (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  user_id BIGINT NOT NULL,
                                  follow_user_id BIGINT NOT NULL,
                                  create_time DATETIME,
                                  UNIQUE KEY uk_user_follow (user_id, follow_user_id)
);