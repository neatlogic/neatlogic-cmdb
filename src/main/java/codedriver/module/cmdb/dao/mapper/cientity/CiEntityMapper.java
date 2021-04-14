/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.cientity;

import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CiEntityMapper {
    /**
     * 获取配置项基本信息
     *
     * @param ciEntityId 配置项id
     * @return CiEntityVo
     */
    CiEntityVo getCiEntityBaseInfoById(Long ciEntityId);

    /**
     * 根据id列表返回多个配置项基本信息
     *
     * @param ciEntityIdList 配置项id列表
     * @return 多个CiEntityVo
     */
    List<CiEntityVo> getCiEntityBaseInfoByIdList(@Param("ciEntityIdList") List<Long> ciEntityIdList);

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

    int getCiEntityCountByCiId(Long ciId);

    List<Map<String, Object>> searchCiEntity(CiEntityVo ciEntityVo);

    List<Long> searchCiEntityId(CiEntityVo ciEntityVo);

    List<Map<String, Object>> getCiEntityById(CiEntityVo ciEntityVo);

    void deleteAttrEntityByFromCiEntityIdAndAttrId(@Param("fromCiEntityId") Long fromCiEntityId, @Param("attrId") Long attrId);

    void updateCiEntityBaseInfo(CiEntityVo ciEntityVo);

    void updateCiEntity(CiEntityVo ciEntityVo);

    void insertCiEntity(CiEntityVo ciEntityVo);

    void insertCiEntityBaseInfo(CiEntityVo ciEntityVo);

    void insertAttrEntity(AttrEntityVo attrEntityVo);

    void deleteCiEntityBaseInfo(CiEntityVo ciEntityVo);

    void deleteCiEntity(CiEntityVo ciEntityVo);

    //old

    List<Long> getCiEntityIdByCiId(Long ciId);

    List<CiEntityVo> getCiEntityByAttrId(Long attrId);

    List<Long> getCiEntityIdByGroupIdList(@Param("groupIdList") List<Long> groupIdList,
                                          @Param("ciEntityIdList") List<Long> ciEntityIdList, @Param("typeList") List<String> typeList);


    int searchCiEntityIdCount(CiEntityVo ciEntityVo);

    List<CiEntityVo> searchCiEntityByIdList(@Param("idList") List<Long> idList);

    Long getIdByCiIdAndName(@Param("ciId") Long ciId, @Param("name") String name);


    int updateCiEntityLockById(CiEntityVo ciEntityVo);

    int deleteCiEntityByCiId(Long ciId);


}
