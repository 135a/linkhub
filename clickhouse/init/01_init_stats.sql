-- 创建统计数据库
CREATE DATABASE IF NOT EXISTS shortlink_stats;

-- 访问统计表（PV/UV/UIP，按小时+星期聚合）
CREATE TABLE IF NOT EXISTS shortlink_stats.link_access_stats
(
    full_short_url String,
    date           Date,
    pv             UInt32 DEFAULT 0,
    uv             UInt32 DEFAULT 0,
    uip            UInt32 DEFAULT 0,
    hour           UInt8,
    weekday        UInt8,
    create_time    DateTime DEFAULT now()
) ENGINE = MergeTree()
PARTITION BY toYYYYMMDD(date)
ORDER BY (full_short_url, date)
SETTINGS index_granularity = 8192;

-- 地理位置统计表
CREATE TABLE IF NOT EXISTS shortlink_stats.link_locale_stats
(
    full_short_url String,
    date           Date,
    cnt            UInt32 DEFAULT 0,
    province       String DEFAULT '',
    city           String DEFAULT '',
    adcode         String DEFAULT '',
    country        String DEFAULT '',
    create_time    DateTime DEFAULT now()
) ENGINE = MergeTree()
PARTITION BY toYYYYMMDD(date)
ORDER BY (full_short_url, date)
SETTINGS index_granularity = 8192;

-- 操作系统统计表
CREATE TABLE IF NOT EXISTS shortlink_stats.link_os_stats
(
    full_short_url String,
    date           Date,
    cnt            UInt32 DEFAULT 0,
    os             String DEFAULT '',
    create_time    DateTime DEFAULT now()
) ENGINE = MergeTree()
PARTITION BY toYYYYMMDD(date)
ORDER BY (full_short_url, date)
SETTINGS index_granularity = 8192;

-- 浏览器统计表
CREATE TABLE IF NOT EXISTS shortlink_stats.link_browser_stats
(
    full_short_url String,
    date           Date,
    cnt            UInt32 DEFAULT 0,
    browser        String DEFAULT '',
    create_time    DateTime DEFAULT now()
) ENGINE = MergeTree()
PARTITION BY toYYYYMMDD(date)
ORDER BY (full_short_url, date)
SETTINGS index_granularity = 8192;

-- 设备统计表
CREATE TABLE IF NOT EXISTS shortlink_stats.link_device_stats
(
    full_short_url String,
    date           Date,
    cnt            UInt32 DEFAULT 0,
    device         String DEFAULT '',
    create_time    DateTime DEFAULT now()
) ENGINE = MergeTree()
PARTITION BY toYYYYMMDD(date)
ORDER BY (full_short_url, date)
SETTINGS index_granularity = 8192;

-- 网络类型统计表
CREATE TABLE IF NOT EXISTS shortlink_stats.link_network_stats
(
    full_short_url String,
    date           Date,
    cnt            UInt32 DEFAULT 0,
    network        String DEFAULT '',
    create_time    DateTime DEFAULT now()
) ENGINE = MergeTree()
PARTITION BY toYYYYMMDD(date)
ORDER BY (full_short_url, date)
SETTINGS index_granularity = 8192;

-- 访问日志明细表
CREATE TABLE IF NOT EXISTS shortlink_stats.link_access_logs
(
    full_short_url String,
    user           String DEFAULT '',
    ip             String DEFAULT '',
    browser        String DEFAULT '',
    os             String DEFAULT '',
    network        String DEFAULT '',
    device         String DEFAULT '',
    locale         String DEFAULT '',
    create_time    DateTime DEFAULT now()
) ENGINE = MergeTree()
PARTITION BY toYYYYMMDD(toDate(create_time))
ORDER BY (full_short_url, create_time)
SETTINGS index_granularity = 8192;

-- 今日实时统计表
CREATE TABLE IF NOT EXISTS shortlink_stats.link_stats_today
(
    full_short_url String,
    date           Date,
    today_pv       UInt32 DEFAULT 0,
    today_uv       UInt32 DEFAULT 0,
    today_uip      UInt32 DEFAULT 0,
    create_time    DateTime DEFAULT now()
) ENGINE = MergeTree()
PARTITION BY toYYYYMMDD(date)
ORDER BY (full_short_url, date)
SETTINGS index_granularity = 8192;
