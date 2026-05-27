package com.contentgenius.user.service;

import com.contentgenius.user.entity.Permission;

import java.util.List;

public interface PermissionService {

    /** 某档会员自带的权限 */
    List<Permission> listByMemberLevel(Integer memberLevel);

    /** 某用户单独绑定的权限 */
    List<Permission> listByUserId(Long userId);

    /** 登录用：档位权限 + 用户单独权限 */
    List<Permission> listForUser(Long userId, Integer memberLevel);

    /** 登录用：上述权限的 code 列表，便于塞进 authorities */
    List<String> listCodesForUser(Long userId, Integer memberLevel);
}
