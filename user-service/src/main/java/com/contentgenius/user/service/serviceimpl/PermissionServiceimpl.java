package com.contentgenius.user.service.serviceimpl;

import com.contentgenius.user.entity.Permission;
import com.contentgenius.user.mapper.PermissionMapper;
import com.contentgenius.user.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionServiceimpl implements PermissionService {

    @Autowired
    private PermissionMapper permissionMapper;

    @Override
    public List<Permission> listByMemberLevel(Integer memberLevel) {
        return permissionMapper.selectByMemberLevel(memberLevel);
    }

    @Override
    public List<Permission> listByUserId(Long userId) {
        return permissionMapper.selectByUserId(userId);
    }

    @Override
    public List<Permission> listForUser(Long userId, Integer memberLevel) {
        return permissionMapper.selectMergedByUser(userId, memberLevel);
    }

    @Override
    public List<String> listCodesForUser(Long userId, Integer memberLevel) {
        return listForUser(userId, memberLevel).stream()
                .map(Permission::getCode)
                .toList();
    }
}
