package com.contentgenius.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.contentgenius.user.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}
