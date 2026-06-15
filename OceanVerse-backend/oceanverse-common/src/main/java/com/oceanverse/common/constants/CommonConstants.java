package com.oceanverse.common.constants;

/**
 * 通用常量
 */
public final class CommonConstants {

    private CommonConstants() {}

    // ==================== Redis Key 前缀 ====================
    public static final String REDIS_USER_TOKEN = "user:token:";
    public static final String REDIS_USER_INFO = "user:info:";
    public static final String REDIS_SPECIES_CACHE = "species:cache:";
    public static final String REDIS_SPECIES_HOT = "species:hot";
    public static final String REDIS_LEADERBOARD = "leaderboard:points";
    public static final String REDIS_AI_RECOGNITION = "ai:recognition:";
    public static final String REDIS_POST_HOT = "post:hot";
    public static final String REDIS_CAPTCHA = "captcha:";
    public static final String REDIS_RATE_LIMIT = "rate:limit:";

    // ==================== Redis Stream ====================
    public static final String STREAM_NOTIFICATION = "stream:notification";
    public static final String STREAM_COMMUNITY = "stream:community";
    public static final String STREAM_AI = "stream:ai";
    public static final String STREAM_SYSTEM = "stream:system";
    public static final String STREAM_CONSUMER_GROUP = "oceanverse-group";
    public static final String STREAM_CONSUMER_NAME = "oceanverse-consumer";

    // ==================== 用户状态 ====================
    public static final int USER_STATUS_NORMAL = 1;
    public static final int USER_STATUS_DISABLED = 0;
    public static final int USER_STATUS_LOCKED = 2;

    // ==================== 删除标记 ====================
    public static final int NOT_DELETED = 0;
    public static final int DELETED = 1;

    // ==================== 分页默认值 ====================
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    // ==================== 通用响应码 ====================
    public static final int CODE_SUCCESS = 200;
    public static final int CODE_BAD_REQUEST = 400;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_NOT_FOUND = 404;
    public static final int CODE_ERROR = 500;
}
