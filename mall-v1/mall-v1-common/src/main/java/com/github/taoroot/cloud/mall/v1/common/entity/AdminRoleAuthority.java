package com.github.taoroot.cloud.mall.v1.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("admin_role_authority")
@EqualsAndHashCode(callSuper = true)
public class AdminRoleAuthority extends Model<AdminRoleAuthority> {

	private static final long serialVersionUID = 1L;

	private Integer roleId;

	private Integer authorityId;
}