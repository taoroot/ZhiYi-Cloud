package com.github.taoroot.cloud.mall.v1.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "admin_dept")
public class AdminDept extends Model<AdminDept> {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private Integer parentId;

    private Integer weight;

    private String email;

    private String phone;

    private String leader;

    private Boolean enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
