package com.jiandou.api.task.infrastructure.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务队列事件MyBatis Mapper。
 */
@Mapper
public interface TaskQueueEventMapper extends BaseMapper<TaskQueueEventEntity> {
}
