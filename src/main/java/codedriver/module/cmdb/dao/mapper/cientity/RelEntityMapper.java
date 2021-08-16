/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.cientity;

import codedriver.framework.cmdb.dto.cientity.RelEntityVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 *
 */
public interface RelEntityMapper {
    List<RelEntityVo> getRelEntityByRelId(RelEntityVo relEntityVo);

    int checkRelEntityIsExists(RelEntityVo relEntityVo);

    List<RelEntityVo> getRelEntityByFromCiIdAndRelId(@Param("relId") Long relId, @Param("fromCiId") Long fromCiId);

    List<RelEntityVo> getRelEntityByToCiIdAndRelId(@Param("relId") Long relId, @Param("toCiId") Long toCiId);

    List<RelEntityVo> getRelEntityByCiEntityId(Long ciEntityId);

    List<RelEntityVo> searchRelEntityByCiEntityIdList(@Param("idList") List<Long> idList,
                                                      @Param("relIdList") List<Long> relIdList);

    List<RelEntityVo> getRelEntityByFromCiEntityIdAndRelId(@Param("fromCiEntityId") Long fromCiEntityId,
                                                           @Param("relId") Long relId);

    List<RelEntityVo> getRelEntityByToCiEntityIdAndRelId(@Param("toCiEntityId") Long toCiEntityId,
                                                         @Param("relId") Long relId);

    List<Long> getFromToCiEntityIdByCiEntityIdList(@Param("idList") List<Long> idList);

    List<Long> getFromToCiEntityIdByCiEntityId(Long ciEntityId);

    int insertRelEntity(RelEntityVo relEntityVo);

    void updateRelEntityIsDeleteByRelId(Long relId);

    int deleteRelEntityByFromCiEntityIdAndRelId(
            @Param("fromCiEntityId") Long fromCiEntityId, @Param("relId") Long relId);

    int deleteRelEntityByToCiEntityIdAndRelId(@Param("toCiEntityId") Long toCiEntityId,
                                              @Param("relId") Long relId);

    int deleteRelEntityByRelIdFromCiEntityIdToCiEntityId(@Param("relId") Long relId,
                                                         @Param("fromCiEntityId") Long fromCiEntityId,
                                                         @Param("toCiEntityId") Long toCiEntityId);
}
