package win.scolia.sso.service.Impl;

import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import win.scolia.sso.bean.entity.Permission;
import win.scolia.sso.bean.entity.Role;
import win.scolia.sso.bean.entity.RolePermission;
import win.scolia.sso.dao.PermissionMapper;
import win.scolia.sso.dao.RolePermissionMapper;
import win.scolia.sso.exception.DuplicatePermissionException;
import win.scolia.sso.exception.MissPermissionException;
import win.scolia.sso.exception.MissRoleException;
import win.scolia.sso.service.PermissionService;
import win.scolia.sso.service.RoleService;
import win.scolia.sso.util.CacheUtils;
import win.scolia.sso.util.PageUtils;

import java.util.List;
import java.util.Set;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private RoleService roleService;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private CacheUtils cacheUtils;

    @Autowired
    private PageUtils pageUtils;

    @Override
    public void createPermission(String permission) {
        Permission cachePermission = this.getPermission(permission);
        if (cachePermission != null) {
            throw new DuplicatePermissionException(String.format("%s already exist", permission));
        }
        Permission record = new Permission(permission);
        record.forCreate();
        try {
            permissionMapper.insert(record);
        } catch (DuplicateKeyException e) {
            throw new DuplicatePermissionException(String.format("%s already exist", permission), e);
        }
    }

    @Override
    public void addPermissionToRole(String roleName, String permission) {
        Role role = roleService.getRoleByRoleName(roleName);
        if (role == null) {
            throw new MissRoleException(String.format("%s not exist", roleName));
        }
        Permission p = this.getPermission(permission);
        if (p == null) {
            throw new MissPermissionException(String.format("%s not exist", permission));
        }
        RolePermission record = new RolePermission(role.getRoleId(), p.getPermissionId());
        record.forCreate();
        try {
            rolePermissionMapper.insert(record);
            cacheUtils.clearRolePermissions(roleName);
        } catch (DuplicateKeyException e) {
            throw new DuplicatePermissionException(e);
        }

    }

    @Transactional
    @Override
    public void removePermission(String permission) {
        Permission record = this.getPermission(permission);
        if (record == null) {
            throw new MissPermissionException(String.format("%s not exist", permission));
        }
        // 删除权限的同时, 也删除其映射表中的相关记录
        permissionMapper.delete(record);
        RolePermission target = new RolePermission();
        target.setPermissionId(record.getPermissionId());
        rolePermissionMapper.delete(target);
        cacheUtils.clearPermission(permission);
        cacheUtils.clearAllRolePermissions();
    }

    @Override
    public void removeRolePermission(String roleName, String permission) {
        Role role = roleService.getRoleByRoleName(roleName);
        if (role == null) {
            throw new MissRoleException(String.format("%s not exist", roleName));
        }
        Permission p = this.getPermission(permission);
        if (p == null) {
            throw new MissPermissionException(String.format("%s not exist", permission));
        }
        RolePermission target = new RolePermission(role.getRoleId(), p.getPermissionId());
        rolePermissionMapper.delete(target);
        cacheUtils.clearRolePermissions(roleName);
    }

    @Override
    public void changePermission(String oldPermission, String newPermission) {
        Permission op = this.getPermission(oldPermission);
        if (op == null) {
            throw new MissPermissionException(String.format("%s not exist", oldPermission));
        }
        Permission np = this.getPermission(newPermission);
        if (np != null) {
            throw new DuplicatePermissionException(String.format("%s already exist", newPermission));
        }
        Permission record = new Permission(op.getPermissionId(), newPermission);
        record.forUpdate();
        permissionMapper.updateByPrimaryKeySelective(record);
        cacheUtils.clearPermission(oldPermission);
        cacheUtils.clearAllRolePermissions();
    }

    @Override
    public Set<String> getPermissionsByRoleName(String roleName) {
        Set<String> permissions = cacheUtils.getRolePermissions(roleName);
        if (permissions == null) {
            permissions = permissionMapper.selectPermissionsByRoleName(roleName);
            cacheUtils.cacheRolePermissions(roleName, permissions);
        }
        return permissions;
    }

    @Override
    public Permission getPermission(String permission) {
        Permission p = cacheUtils.getPermission(permission);
        if (p == null) {
            Permission query = new Permission(permission);
            p = permissionMapper.selectOne(query);
            cacheUtils.cachePermission(p);
        }
        return p;
    }

    @Override
    public PageInfo<Permission> listAllPermission(Integer pageNum) {
        pageUtils.startPage(pageNum);
        List<Permission> permissions = permissionMapper.selectAll();
        return pageUtils.getPageInfo(permissions);
    }
}
