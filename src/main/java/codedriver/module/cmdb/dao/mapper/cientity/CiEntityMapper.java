/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.cientity;

import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.fulltextindex.dto.fulltextindex.FullTextIndexTypeVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CiEntityMapper {
    /**
     * 查询没有创建索引的配置项id
     *
     * @param fullTextIndexTypeVo 索引类型
     * @return 配置项id列表
     */
    List<Long> getNotIndexCiEntityIdList(FullTextIndexTypeVo fullTextIndexTypeVo);

    /**
     * 检查引用属性是否有值
     *
     * @param fromCiId 模型id
     * @param attrId   属性id
     * @return 数量
     */
    int getAttrEntityCountByFromCiIdAndAttrId(@Param("fromCiId") Long fromCiId, @Param("attrId") Long attrId);

    /**
     * 搜索配置项基本信息数量，只支持条件ci_id
     *
     * @param ciEntityVo 配置项信息
     * @return 条目数量
     */
    int searchCiEntityBaseInfoCount(CiEntityVo ciEntityVo);

    /**
     * 搜索配置项基本信息数量，只支持条件ci_id
     *
     * @param ciEntityVo 配置项信息
     * @return 配置项基本信息列表
     */
    List<CiEntityVo> searchCiEntityBaseInfo(CiEntityVo ciEntityVo);

    /**
     * 获取当前模型以及所有子模型的配置项数量
     *
     * @param lft 模型左编码
     * @param rht 模型右编码
     * @return 配置项数量
     */
    int getDownwardCiEntityCountByLR(@Param("lft") Integer lft, @Param("rht") Integer rht);

    /**
     * 获取当前模型以及所有子模型的配置项
     *
     * @param lft  模型左编码
     * @param rht  模型右编码
     * @param size 限制的返回数量
     * @return 配置项
     */
    List<CiEntityVo> getDownwardCiEntityByLRLimitSize(@Param("lft") Integer lft, @Param("rht") Integer rht, @Param("size") Integer size);

    /**
     * 获取配置项基本信息
     *
     * @param ciEntityId 配置项id
     * @return CiEntityVo
     */
    CiEntityVo getCiEntityBaseInfoById(Long ciEntityId);

    /**
     * 根据配置项名称获取配置项
     *
     * @param ciEntityVo 配置项名称
     * @return 配置项列表
     */
    List<CiEntityVo> getCiEntityBaseInfoByName(CiEntityVo ciEntityVo);

    /**
     * 根据配置项名称获取虚拟模型配置项
     *
     * @param ciEntityVo
     * @return 配置项列表
     */
    List<CiEntityVo> getVirtualCiEntityBaseInfoByName(CiEntityVo ciEntityVo);

    /**
     * 根据id列表返回多个配置项基本信息
     *
     * @param ciEntityIdList 配置项id列表
     * @return 多个CiEntityVo
     */
    List<CiEntityVo> getCiEntityBaseInfoByIdList(@Param("ciEntityIdList") List<Long> ciEntityIdList);

    /**
     * 根据id列表返回多个虚拟配置项基本信息
     *
     * @param ciEntityVo 条件
     * @return 多个配置项
     */
    List<CiEntityVo> getVirtualCiEntityBaseInfoByIdList(CiEntityVo ciEntityVo);

    /**
     * 返回除ciEntityId外所有引用了目标配置项的配置项数量
     *
     * @param ciEntityId       配置项id
     * @param attrId           属性id
     * @param toCiEntityIdList 目标配置项id列表
     * @return 数量
     */
    int getAttrEntityCountByAttrIdAndValue(@Param("ciEntityId") Long ciEntityId, @Param("attrId") Long attrId, @Param("toCiEntityIdList") List<Long> toCiEntityIdList);

    /**
     * 返回除ciEntityId外所有使用了属性值的配置项
     *
     * @param ciEntityId 配置项id
     * @param attrVo     属性定义
     * @param value      属性值（只能是单值）
     * @return 数量
     */
    int getCiEntityCountByAttrIdAndValue(@Param("ciEntityId") Long ciEntityId, @Param("attrVo") AttrVo attrVo, @Param("value") String value);

    /**
     * 返回来源配置项引用的所有目标属性
     *
     * @param attrId         属性id
     * @param fromCiEntityId 来源配置项id
     * @return 属性列表
     */
    List<AttrEntityVo> getAttrEntityByAttrIdAndFromCiEntityId(@Param("fromCiEntityId") Long fromCiEntityId, @Param("attrId") Long attrId);

    /**
     * 返回来源配置项引用的所有目标配置项基本信息
     *
     * @param fromCiEntityId 来源配置项id
     * @param attrId         属性id
     * @return 配置项列表
     */
    List<CiEntityVo> getCiEntityBaseInfoByAttrIdAndFromCiEntityId(@Param("fromCiEntityId") Long fromCiEntityId, @Param("attrId") Long attrId);

    /**
     * 查询使用了当前配置项的属性列表（删除配置项时用）
     *
     * @param toCiEntityId 配置项id
     * @return 属性列表
     */
    List<AttrVo> getAttrListByToCiEntityId(Long toCiEntityId);

    List<Map<String, Object>> searchCiEntity(CiEntityVo ciEntityVo);

    List<Long> searchCiEntityId(CiEntityVo ciEntityVo);

    List<Map<String, Object>> getCiEntityById(CiEntityVo ciEntityVo);

    List<CiEntityVo> getCiEntityListByCiIdListAndName(CiEntityVo ciEntityVo);

    void deleteAttrEntityByFromCiEntityIdAndAttrId(@Param("fromCiEntityId") Long fromCiEntityId, @Param("attrId") Long attrId);

    void updateCiEntityBaseInfo(CiEntityVo ciEntityVo);

    void updateCiEntityName(CiEntityVo ciEntityVo);

    void updateCiEntity(CiEntityVo ciEntityVo);

    void insertCiEntity(CiEntityVo ciEntityVo);

    void insertCiEntityBaseInfo(CiEntityVo ciEntityVo);

    void insertAttrEntity(AttrEntityVo attrEntityVo);

    void deleteCiEntityBaseInfo(CiEntityVo ciEntityVo);

    void deleteCiEntity(CiEntityVo ciEntityVo);

    void deleteAttrEntityByAttrId(Long attrId);
    //old

    List<Long> getCiEntityIdByCiId(CiEntityVo ciEntityVo);

    List<CiEntityVo> getCiEntityByAttrId(Long attrId);


    int searchCiEntityIdCount(CiEntityVo ciEntityVo);

    List<CiEntityVo> searchCiEntityByIdList(@Param("idList") List<Long> idList);

    Long getIdByCiIdAndName(@Param("ciId") Long ciId, @Param("name") String name);


    int updateCiEntityLockById(CiEntityVo ciEntityVo);

    int deleteCiEntityByCiId(Long ciId);

    int deleteParentCiEntity(@Param("currentCi") CiVo currentCiVo, @Param("parentCiList") List<CiVo> parentCiList);

}
