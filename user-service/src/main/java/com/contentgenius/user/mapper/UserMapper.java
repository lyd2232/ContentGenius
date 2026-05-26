package com.contentgenius.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.contentgenius.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
