package com.jiandou.api.task.infrastructure.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务阶段运行MyBatis Mapper。
 */
@Mapper
public interface TaskStageRunMapper extends BaseMapper<TaskStageRunEntity> {
}
