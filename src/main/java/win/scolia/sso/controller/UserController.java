package win.scolia.sso.controller;


import com.github.pagehelper.PageInfo;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import win.scolia.sso.bean.entity.UserSafely;
import win.scolia.sso.bean.vo.entry.UserEntry;
import win.scolia.sso.bean.vo.export.MessageExport;
import win.scolia.sso.bean.vo.export.UserExport;
import win.scolia.sso.exception.DuplicateRoleException;
import win.scolia.sso.exception.DuplicateUserException;
import win.scolia.sso.exception.MissRoleException;
import win.scolia.sso.exception.MissUserException;
import win.scolia.sso.service.PermissionService;
import win.scolia.sso.service.RoleService;
import win.scolia.sso.service.UserService;
import win.scolia.sso.util.MessageUtils;
import win.scolia.sso.util.ShiroUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 主要负责用户管理的相关信息
 */
@Controller
@RequestMapping(value = "account/users")
@RequiresAuthentication
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    /**
     * 新增用户
     *
     * @param userEntry   用户信息
     * @param bindingResult 验证信息
     * @return 201 成功, 400 参数错误 409 该用户已存在
     */
    @PostMapping
    @RequiresPermissions("system:user:add")
    public ResponseEntity<MessageExport> addUser(@Validated(UserEntry.Register.class) UserEntry userEntry,
                                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            MessageExport messageExport = MessageUtils.makeValidMessage(bindingResult);
            return ResponseEntity.badRequest().body(messageExport);
        }
        try {
            userService.createUser(userEntry);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("{} add user: {}", ShiroUtils.getCurrentUserName(), userEntry.getUserName());
            }
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (DuplicateUserException e) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("{} add duplicate user: {}", ShiroUtils.getCurrentUserName(), userEntry.getUserName());
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * 删除某个用户
     *
     * @param userName 用户名
     * @return 200 成功, 403 权限不足, 404 要删除的用户不存在
     */
    @DeleteMapping("{userName}")
    @RequiresPermissions("system:user:delete")
    public ResponseEntity<Void> deleteUser(@PathVariable("userName") String userName) {
        try {
            userService.removeUserByUserName(userName);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("{} delete user: {}", ShiroUtils.getCurrentUserName(), userName);
            }
            return ResponseEntity.ok().build();
        } catch (MissUserException e) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("{} delete miss user: {}", ShiroUtils.getCurrentUserName(), userName);
            }
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 修改用户密码
     *
     * @param userName 用户名
     * @param password 密码
     * @return 200 成功, 404 要修改密码的用户不存在
     */
    @PutMapping("{userName}/password")
    @RequiresPermissions("system:user:update")
    public ResponseEntity<Void> changePassword(@PathVariable("userName") String userName, @RequestParam String password) {
        try {
            userService.changePasswordDirectly(userName, password);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("{} change user password: {}", ShiroUtils.getCurrentUserName(), userName);
            }
            return ResponseEntity.ok().build();
        } catch (MissUserException e) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("{} change miss user password: {}", ShiroUtils.getCurrentUserName(), userName);
            }
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取某个用户的信息
     *
     * @param userName 用户名
     * @return 200 成功, 404 没有此用户
     */
    @GetMapping("{userName}")
    @RequiresPermissions("system:user:get")
    public ResponseEntity<UserExport> getUser(@PathVariable("userName") String userName) {
        UserSafely userSafely = userService.getUserSafelyByUserName(userName);
        if (userSafely == null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("{} get miss user info: {}", ShiroUtils.getCurrentUserName(), userName);
            }
            return ResponseEntity.notFound().build();
        }
        Set<String> roles = roleService.getUserRolesByUserName(userSafely.getUserName());
        Set<String> permissions = new HashSet<>();
        for (String roleName : roles) {
            permissions.addAll(permissionService.getPermissionsByRoleName(roleName));
        }
        UserExport vo = new UserExport(userSafely, roles, permissions);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("{} get user info: {}", ShiroUtils.getCurrentUserName(), userName);
        }
        return ResponseEntity.ok(vo);
    }

    /**
     * 列出所有的用户
     *
     * @param pageNum 页码
     * @return 200 成功
     */
    @GetMapping("list/{pageNum}")
    @RequiresPermissions("system:user:list")
    public ResponseEntity<PageInfo> listUsers(@PathVariable("pageNum") Integer pageNum) {
        PageInfo pageInfo = userService.listUsersSafely(pageNum);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("{} list users in page: {}", ShiroUtils.getCurrentUserName(), pageNum);
        }
        return ResponseEntity.ok(pageInfo);
    }

    /**
     * 为用户添加一个角色
     *
     * @param userName 用户名
     * @param roleName 角色名
     * @return 200 成功 404 用户不存在 400 角色不存在 409 角色已添加
     */
    @PostMapping("{userName}/roles")
    @RequiresPermissions("system:user:edit")
    public ResponseEntity<Void> addRoleToUser(@PathVariable("userName") String userName,
                                              @RequestParam String roleName) {
        try {
            roleService.addRoleToUser(userName, roleName);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("{} add user's role: {}:{}", ShiroUtils.getCurrentUserName(), userName, roleName);
            }
            return ResponseEntity.ok().build();
        } catch (MissUserException e) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("{} add user's role: {}:{}, but miss user", ShiroUtils.getCurrentUserName(), userName, roleName);
            }
            return ResponseEntity.notFound().build();
        } catch (MissRoleException e) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("{} add user's role: {}:{}, but miss role", ShiroUtils.getCurrentUserName(), userName, roleName);
            }
            return ResponseEntity.badRequest().build();
        } catch (DuplicateRoleException e) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("{} add duplicate user's role: {}:{}", ShiroUtils.getCurrentUserName(), userName, roleName);
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * 为用户删除一个角色
     *
     * @param userName 用户名
     * @param roleName 角色名
     * @return 200 成功 400 角色不存在 404 用户不存在
     */
    @DeleteMapping("{userName}/roles/{roleName}")
    @RequiresPermissions("system:user:edit")
    public ResponseEntity<Void> deleteUserRole(@PathVariable("userName") String userName,
                                               @PathVariable("roleName") String roleName) {
        try {
            roleService.romeUserRole(userName, roleName);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("{} delete user's role: {}:{}", ShiroUtils.getCurrentUserName(), userName, roleName);
            }
            return ResponseEntity.ok().build();
        } catch (MissUserException e) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("{} delete user's role: {}:{}, but miss user", ShiroUtils.getCurrentUserName(), userName, roleName);
            }
            return ResponseEntity.notFound().build();
        } catch (MissRoleException e) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("{} delete user's role: {}:{}, but miss role", ShiroUtils.getCurrentUserName(), userName, roleName);
            }
            return ResponseEntity.badRequest().build();
        }
    }
}
