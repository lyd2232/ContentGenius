package com.contentgenius.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 会员档位与默认权限的映射（联合主键 member_level + permission_id）。
 */
@Data
@TableName("member_level_permission")
public class MemberLevelPermission {

    /** 与 user.member_level 一致：0 免费 1 VIP 2 管理员 */
    private Integer memberLevel;

    private Long permissionId;
}
