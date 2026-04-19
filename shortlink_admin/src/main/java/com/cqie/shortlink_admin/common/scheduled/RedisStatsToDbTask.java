package com.cqie.shortlink_admin.common.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cqie.shortlink_admin.entity.LinkAccessStatsDO;
import com.cqie.shortlink_admin.entity.ScheduledDO;
import com.cqie.shortlink_admin.mapper.LinkAccessStatsMapper;
import com.cqie.shortlink_admin.service.ScheduledService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class RedisStatsToDbTask implements SchedulingConfigurer {

    private final RedisTemplate<String, String> redisTemplate;
    private final ScheduledService scheduledService;
    private final LinkAccessStatsMapper linkAccessStatsMapper;


    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        scheduledTaskRegistrar.addTriggerTask(
                // 执行任务
                this::syncRedisStatsToDatabase,
                // 动态读取 cron
                triggerContext -> {
                    ScheduledDO scheduledDO = scheduledService.getOne(
                            new LambdaQueryWrapper<ScheduledDO>()
                                    .eq(ScheduledDO::getCronId, 1)
                    );
                    return new CronTrigger(scheduledDO.getCron()).nextExecutionTime(triggerContext).toInstant();
                }
        );
    }

    /**
     * 同步 Redis 中的 UV、PV 数据到数据库
     */
    private void syncRedisStatsToDatabase() {
        System.out.println("【定时任务】开始同步 Redis UV/PV 数据到数据库 =====");

        //获取当天的数据
        LocalDateTime now = LocalDateTime.now();
        processAllKeysForDate(now);
        // 获取昨天的数据，防止跨天数据遗漏
        processAllKeysForDate(now.minusDays(1));

        log.info("【定时任务】完成同步 Redis UV/PV 数据到数据库 =====");

    }


    private void processAllKeysForDate(LocalDateTime date) {
        String nowDay = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        // 1. 扫描 PV 和 UV 的 Key，统一分批处理
        ScanOptions pvOpts = ScanOptions.scanOptions()
                .match("short-link:pv:" + nowDay + ":*")
                .count(1000)
                .build();
        ScanOptions uvOpts = ScanOptions.scanOptions()
                .match("short-link:uv:" + nowDay + ":*")
                .count(1000)
                .build();

        // 2. 使用 Cursor 分批扫描 Redis Key，避免一次性加载过多数据到内存
        try (Cursor<String> pvCursor = redisTemplate.scan(pvOpts);
             Cursor<String> uvCursor = redisTemplate.scan(uvOpts)) {

            List<String> pvBatch = new ArrayList<>(1000);
            List<String> uvBatch = new ArrayList<>(1000);

            while (pvCursor.hasNext() || uvCursor.hasNext()) {
                // 攒 PV 批次
                while (pvCursor.hasNext() && pvBatch.size() < 500) {
                    pvBatch.add(pvCursor.next());
                }
                // 攒 UV 批次
                while (uvCursor.hasNext() && uvBatch.size() < 500) {
                    uvBatch.add(uvCursor.next());
                }

                // 处理 PV 批次
                if (!pvBatch.isEmpty()) {
                    processPVBatchWithPipeline(pvBatch);
                    pvBatch.clear();
                }
                // 处理 UV 批次
                if (!uvBatch.isEmpty()) {
                    processUVBatchWithPipeline(uvBatch);
                    uvBatch.clear();
                }
            }
        }
    }

    /**
     * 使用 Redis Pipeline 批量PV获取数据，减少网络开销
     * @param keys 要获取的 Redis Key 列表
     */
    private void processPVBatchWithPipeline(List<String> keys) {
        // Pipeline 批量获取 PV 值
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            keys.forEach(key -> connection.stringCommands().get(key.getBytes()));
            return null;
        });

        List<LinkAccessStatsDO> statsList = new ArrayList<>(results.size());

        for (int i = 0; i < results.size(); i++) {
            String key = keys.get(i);
            String value = (String) results.get(i);
            if (value == null) {
                continue;
            }

            //解析key格式
            //short-link : pv : 2026-04-16 : 16 : PemwYn
            String[] parts = key.split(":");
            if (parts.length < 4) {
                continue;
            }

            LinkAccessStatsDO stats = new LinkAccessStatsDO();
            stats.setDate(LocalDate.parse(parts[2]).atTime(0, 0));

            if (parts.length == 5) {
                stats.setHour(Integer.parseInt(parts[3]));
                stats.setFullShortUrl(parts[4]);
            } else {
                stats.setHour(-1);   // -1 代表全天汇总
                stats.setFullShortUrl(parts[3]);
            }

            stats.setPv(Integer.parseInt(value));
            stats.setUv(-1);   // UV 不更新
            stats.setWeekday(stats.getDate().getDayOfWeek().getValue());
            statsList.add(stats);
        }

        linkAccessStatsMapper.batchUpsert(statsList);
    }

    /**
     * 使用 Redis Pipeline 批量UV获取数据，减少网络开销
     * @param keys 要获取的 Redis Key 列表
     */
    private void processUVBatchWithPipeline(List<String> keys) {
        // Pipeline 批量获取 PV 值
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            keys.forEach(key -> connection.hyperLogLogCommands().pfCount(key.getBytes()));
            return null;
        });

        List<LinkAccessStatsDO> statsList = new ArrayList<>(results.size());

        for (int i = 0; i < results.size(); i++) {
            String key = keys.get(i);
            Long value = (Long) results.get(i);
            if (value == null) {
                continue;
            }

            //解析key格式
            //short-link : uv : 2026-04-16 : 16 : PemwYn
            String[] parts = key.split(":");
            if (parts.length < 4) {
                continue;
            }

            LinkAccessStatsDO stats = new LinkAccessStatsDO();
            stats.setDate(LocalDate.parse(parts[2]).atTime(0, 0));

            if (parts.length == 5) {
                stats.setHour(Integer.parseInt(parts[3]));
                stats.setFullShortUrl(parts[4]);
            } else {
                stats.setHour(-1);   // -1 代表全天汇总
                stats.setFullShortUrl(parts[3]);
            }

            stats.setUv(value.intValue());
            stats.setPv(-1);   // PV 不更新
            stats.setWeekday(stats.getDate().getDayOfWeek().getValue());
            statsList.add(stats);
        }

        linkAccessStatsMapper.batchUpsert(statsList);
    }
}
