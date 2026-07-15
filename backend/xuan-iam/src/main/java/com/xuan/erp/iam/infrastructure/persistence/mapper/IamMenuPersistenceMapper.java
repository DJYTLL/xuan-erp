package com.xuan.erp.iam.infrastructure.persistence.mapper;

import com.xuan.erp.iam.infrastructure.persistence.entity.IamMenuRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * IAM 菜单持久化 Mapper，负责 iam_menu 表的读取。
 */
@Mapper
public interface IamMenuPersistenceMapper {

    /**
     * 按菜单编码查询有效菜单记录。
     */
    IamMenuRecord findByCode(@Param("code") String code);

    /**
     * 按菜单主键查询有效菜单记录。
     */
    IamMenuRecord findById(@Param("id") Long id);

    /**
     * 查询全部有效菜单记录。
     */
    List<IamMenuRecord> findActiveMenus();

    /**
     * 新增菜单记录。
     */
    int insert(@Param("menu") IamMenuRecord menu);

    /**
     * 更新菜单记录。
     */
    int update(@Param("menu") IamMenuRecord menu);
}
