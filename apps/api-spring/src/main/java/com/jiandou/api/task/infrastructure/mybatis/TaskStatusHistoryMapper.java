package com.jiandou.api.task.infrastructure.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务状态HistoryMyBatis Mapper。
 */
@Mapper
public interface TaskStatusHistoryMapper extends BaseMapper<TaskStatusHistoryEntity> {
}
