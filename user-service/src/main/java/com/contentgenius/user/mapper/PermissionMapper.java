package com.contentgenius.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.contentgenius.user.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

  /** 按会员档位查询默认权限 */
  @Select("""
      SELECT p.id, p.code, p.name, p.description, p.created_at
      FROM permission p
      INNER JOIN member_level_permission mlp ON p.id = mlp.permission_id
      WHERE mlp.member_level = #{memberLevel}
      """)
  List<Permission> selectByMemberLevel(@Param("memberLevel") Integer memberLevel);

  /** 按用户 id 查询单独绑定的权限 */
  @Select("""
      SELECT p.id, p.code, p.name, p.description, p.created_at
      FROM permission p
      INNER JOIN user_permission up ON p.id = up.permission_id
      WHERE up.user_id = #{userId}
      """)
  List<Permission> selectByUserId(@Param("userId") Long userId);

  /** 档位默认权限 ∪ 用户单独权限（去重） */
  @Select("""
      SELECT DISTINCT p.id, p.code, p.name, p.description, p.created_at
      FROM permission p
      WHERE p.id IN (
          SELECT permission_id FROM member_level_permission WHERE member_level = #{memberLevel}
          UNION
          SELECT permission_id FROM user_permission WHERE user_id = #{userId}
      )
      """)
  List<Permission> selectMergedByUser(
      @Param("userId") Long userId, @Param("memberLevel") Integer memberLevel);
}
