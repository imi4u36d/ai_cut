package com.jiandou.api.task.infrastructure.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 工作节点InstanceMyBatis Mapper。
 */
@Mapper
public interface WorkerInstanceMapper extends BaseMapper<WorkerInstanceEntity> {

    @Select("""
        SELECT worker_instance_id
        FROM biz_worker_instances
        WHERE is_deleted = 0
          AND status = 'RUNNING'
          AND last_heartbeat_at < #{staleBefore}
        ORDER BY last_heartbeat_at ASC
        LIMIT #{limit}
        """)
    /**
     * 处理selectStale工作节点Instance标识列表。
     * @param staleBefore staleBefore值
     * @param limit 返回的最大条目数
     * @return 处理结果
     */
    List<String> selectStaleWorkerInstanceIds(
        @Param("staleBefore") OffsetDateTime staleBefore,
        @Param("limit") int limit
    );
}
