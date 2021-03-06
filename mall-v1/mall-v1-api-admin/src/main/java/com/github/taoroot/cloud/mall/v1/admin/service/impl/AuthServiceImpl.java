package com.github.taoroot.cloud.mall.v1.admin.service.impl;

import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.taoroot.cloud.common.core.constant.SecurityConstants;
import com.github.taoroot.cloud.common.core.utils.R;
import com.github.taoroot.cloud.common.core.utils.TreeUtils;
import com.github.taoroot.cloud.common.core.vo.AuthSocialInfo;
import com.github.taoroot.cloud.common.core.vo.AuthUserInfo;
import com.github.taoroot.cloud.common.security.SecurityUtils;
import com.github.taoroot.cloud.common.security.tenant.TenantContextHolder;
import com.github.taoroot.cloud.mall.v1.admin.mapper.*;
import com.github.taoroot.cloud.mall.v1.admin.service.AuthService;
import com.github.taoroot.cloud.mall.v1.admin.service.UserRoleService;
import com.github.taoroot.cloud.mall.v1.admin.service.UserService;
import com.github.taoroot.cloud.mall.v1.admin.service.UserSocialService;
import com.github.taoroot.cloud.mall.v1.common.entity.AdminMenu;
import com.github.taoroot.cloud.mall.v1.common.entity.AdminSocialDetails;
import com.github.taoroot.cloud.mall.v1.common.entity.AdminUser;
import com.github.taoroot.cloud.mall.v1.common.entity.AdminUserSocial;
import com.github.taoroot.cloud.mall.v1.common.mapper.SocialDetailsMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Data
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserService userService;
    private final UserRoleService userRoleService;
    private final UserRoleMapper userRoleMapper;
    private final DeptMapper deptMapper;
    private final SocialDetailsMapper socialDetailsMapper;
    private final UserSocialService userSocialService;
    private final UserSocialMapper userSocialMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public R<Object> userInfo() {
        Integer userId = SecurityUtils.userId();
        HashMap<String, Object> result = new HashMap<>();
        AdminUser adminUser = userMapper.selectById(SecurityUtils.userId());
        // 查询用户个人信息
        result.put("info", adminUser);
        // 查询用户角色信息
        result.put("roles", userMapper.roles(userId));
        // 所属部门
        result.put("dept", deptMapper.selectById(adminUser.getDeptId()).getName());
        // 前端功能
        List<String> functions = userMapper.menus(userId, AdminMenu.FUNCTION).stream()
                .map(AdminMenu::getAuthority).collect(Collectors.toList());
        result.put("functions", functions);
        // 前端菜单
        List<AdminMenu> menus = userMapper.menus(userId, AdminMenu.MENU);
        result.put("menus", TreeUtil.build(menus, TreeUtils.ROOT_PARENT_ID, (treeNode, tree) -> {
            tree.setId(treeNode.getId());
            tree.setParentId(treeNode.getParentId());
            tree.setWeight(treeNode.getWeight());
            tree.setName(treeNode.getName());
            tree.putExtra("path", treeNode.getPath());
            tree.putExtra("hidden", treeNode.getHidden());
            tree.putExtra("alwaysShow", treeNode.getAlwaysShow());
            tree.putExtra("redirect", treeNode.getRedirect());
            tree.putExtra("type", treeNode.getType());
            tree.put("component", treeNode.getComponent());
            HashMap<String, Object> meta = new HashMap<>();
            meta.put("title", treeNode.getTitle());
            meta.put("icon", treeNode.getIcon());
            meta.put("breadcrumb", treeNode.getBreadcrumb());
            tree.putExtra("meta", meta);
        }));
        // 社交账号
        result.put("socials", userMapper.socials(userId));
        return R.ok(result);
    }

    @Override
    public AuthUserInfo authByUsername(String username) {
        AdminUser adminUser = userMapper.selectOne(Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getUsername, username));

        if (adminUser == null) {
            return null;
        }

        Integer userId = adminUser.getId();
        AuthUserInfo userInfo = new AuthUserInfo();
        userInfo.setUsername(String.valueOf(adminUser.getId()));
        userInfo.setPassword(adminUser.getPassword());
        userInfo.setNickname(adminUser.getNickname());
        userInfo.setDeptId(adminUser.getDeptId());
        userInfo.setRoleIds(userMapper.roleIds(userId));
        return userInfo;
    }

    @Override
    public List<AuthSocialInfo> socials(String redirectUrl, Boolean isProxy, String type) {
        List<AdminSocialDetails> adminSocialDetails = socialDetailsMapper.selectList(Wrappers.<AdminSocialDetails>lambdaQuery()
                .eq(!StringUtils.isEmpty(type), AdminSocialDetails::getType, type));
        return adminSocialDetails.stream().map(social -> {
            String authorizeUri;
            String redirectUri = String.format(social.getAuthorizeUri(), social.getAppId(), redirectUrl, IdUtil.fastSimpleUUID());
            URI uri = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam(SecurityConstants.TENANT_ID, TenantContextHolder.get()) // 加租户ID
                    .build().toUri();
            authorizeUri = uri.toString();

            // 代理模式
            if (!StringUtils.isEmpty(social.getProxyUri()) && isProxy && social.getIsProxy()) {
                String origin = buildFullRequestUrl(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), null);
                authorizeUri = UriComponentsBuilder
                        .fromUriString(social.getProxyUri())
                        .query(uri.getQuery())
                        .fragment(uri.getFragment())
                        .queryParam(SecurityConstants.OAUTH2_PROXY_ORIGIN_PARAM, origin)
                        .build()
                        .toString();
            }

            AuthSocialInfo socialInfo = new AuthSocialInfo();
            socialInfo.setAuthorizeUri(authorizeUri);
            socialInfo.setType(social.getType());
            socialInfo.setTitle(social.getTitle());
            socialInfo.setIcon(social.getIcon());
            return socialInfo;
        }).collect(Collectors.toList());
    }

    @Override
    public R<String> unbind(Integer id) {
        boolean remove = userSocialService.remove(Wrappers.<AdminUserSocial>lambdaQuery()
                .eq(AdminUserSocial::getId, id)
                .eq(AdminUserSocial::getAdminUserId, SecurityUtils.userId())
        );
        Assert.isTrue(remove, "删除失败");
        return R.okMsg("删除成功");
    }

    @Override
    public R<String> updatePass(String oldPass, String newPass) {
        AdminUser user = userService.getById(SecurityUtils.userId());
        String password = user.getPassword();
        boolean matches = passwordEncoder.matches(oldPass, password);
        Assert.isTrue(matches, "密码错误");
        user.setPassword(passwordEncoder.encode(newPass));
        Assert.isTrue(userService.updateById(user), "未知原因,更新失败");
        return R.okMsg("更新成功");
    }

    public static String buildFullRequestUrl(String scheme, String serverName,
                                             int serverPort, String requestURI, String queryString) {

        scheme = scheme.toLowerCase();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        if ("http".equals(scheme)) {
            if (serverPort != 80 && serverPort > 0) {
                url.append(":").append(serverPort);
            }
        } else if ("https".equals(scheme)) {
            if (serverPort != 443 && serverPort > 0) {
                url.append(":").append(serverPort);
            }
        }
        url.append(requestURI);

        if (queryString != null) {
            url.append("?").append(queryString);
        }

        return url.toString();
    }
}
