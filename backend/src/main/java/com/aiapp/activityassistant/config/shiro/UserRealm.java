package com.aiapp.activityassistant.config.shiro;

import com.aiapp.activityassistant.entity.User;
import com.aiapp.activityassistant.service.RbacService;
import com.aiapp.activityassistant.service.TokenService;
import com.aiapp.activityassistant.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Shiro Realm：
 * <ul>
 *   <li>认证：校验 Redis 中的登录 token（复用 {@link TokenService}），不引入额外会话存储</li>
 *   <li>授权：加载 RBAC 角色与权限；角色权限本身由 RbacService 做 Redis 缓存</li>
 * </ul>
 */
@RequiredArgsConstructor
public class UserRealm extends AuthorizingRealm {

    private final TokenService tokenService;
    private final UserService userService;
    private final RbacService rbacService;

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof BearerAuthenticationToken;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken)
            throws AuthenticationException {
        String token = String.valueOf(authenticationToken.getPrincipal());
        Long userId = tokenService.resolveUserId(token);
        if (userId == null) {
            throw new IncorrectCredentialsException("登录状态已失效，请重新登录");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new UnknownAccountException("账号不存在");
        }
        // 被禁用账号即使持有未过期 token 也直接拒绝
        if (Objects.equals(0, user.getStatus())) {
            throw new LockedAccountException("账号已被禁用");
        }

        List<String> roles = rbacService.listRoleCodes(userId);
        HashSet<String> permissions = new HashSet<>(rbacService.listPermissionCodes(userId));
        LoginUser loginUser = new LoginUser(userId, user.getUsername(), roles, permissions);
        return new SimpleAuthenticationInfo(loginUser, token, getName());
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        // principal 在认证阶段已带好角色与权限，这里不再重复查库
        LoginUser loginUser = (LoginUser) principals.getPrimaryPrincipal();
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(new HashSet<>(loginUser.getRoles()));
        info.setStringPermissions(loginUser.getPermissions());
        return info;
    }
}
