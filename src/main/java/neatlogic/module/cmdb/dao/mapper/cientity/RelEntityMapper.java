/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.dao.mapper.cientity;

import neatlogic.framework.cmdb.dto.ci.RelativeRelItemVo;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 *
 */
public interface RelEntityMapper {
    List<Long> getCiEntityIdByRelativeRelPath(@Param("relativeRelItemList") List<RelativeRelItemVo> relativeRelItemList, @Param("relEntityId") Long relEntityId, @Param("position") String position);

    List<RelEntityVo> getRelentityBySourceRelEntityId(Long sourceRelEntityId);

    List<RelEntityVo> getExpiredRelEntity(RelEntityVo relEntityVo);

    RelEntityVo getRelEntityById(Long id);

    List<RelEntityVo> getRelEntityByRelId(RelEntityVo relEntityVo);

    int checkRelEntityIsExists(RelEntityVo relEntityVo);

    List<RelEntityVo> getFromRelEntityByFromCiIdAndRelId(RelEntityVo relEntityVo);

    List<RelEntityVo> getToRelEntityByToCiIdAndRelId(RelEntityVo relEntityVo);

    List<RelEntityVo> getRelEntityByFromCiIdAndRelId(@Param("relId") Long relId, @Param("fromCiId") Long fromCiId);

    List<RelEntityVo> getRelEntityByToCiIdAndRelId(@Param("relId") Long relId, @Param("toCiId") Long toCiId);

    List<RelEntityVo> getRelEntityByCiEntityId(Long ciEntityId);

    List<RelEntityVo> searchRelEntityByCiEntityIdList(@Param("idList") List<Long> idList,
                                                      @Param("relIdList") List<Long> relIdList);

    List<RelEntityVo> getRelEntityByFromCiEntityIdAndRelId(@Param("fromCiEntityId") Long fromCiEntityId,
                                                           @Param("relId") Long relId, @Param("limit") Long limit);

    RelEntityVo getRelEntityByFromCiEntityIdAndToCiEntityIdAndRelId(@Param("fromCiEntityId") Long fromCiEntityId, @Param("toCiEntityId") Long toCiEntityId, @Param("relId") Long relId);

    List<RelEntityVo> getRelEntityByToCiEntityIdAndRelId(@Param("toCiEntityId") Long toCiEntityId,
                                                         @Param("relId") Long relId, @Param("limit") Long limit);

    List<Long> getFromToCiEntityIdByCiEntityIdList(@Param("idList") List<Long> idList);

    List<Long> getFromToCiEntityIdByCiEntityId(Long ciEntityId);

    void clearRelEntityFromIndex(@Param("relId") Long relId, @Param("ciEntityId") Long ciEntityId, @Param("limit") Integer limit);

    void clearRelEntityToIndex(@Param("relId") Long relId, @Param("ciEntityId") Long ciEntityId, @Param("limit") Integer limit);


    void updateRelEntityFromIndex(RelEntityVo relEntityVo);

    void updateRelEntityToIndex(RelEntityVo relEntityVo);

    void updateRelEntityValidDay(RelEntityVo relEntityVo);

    void updateRelEntityRenewTime(Long relEntityId);

    int insertRelEntity(RelEntityVo relEntityVo);


    int deleteRelEntityByFromCiEntityIdAndRelId(
            @Param("fromCiEntityId") Long fromCiEntityId, @Param("relId") Long relId);

    int deleteRelEntityByToCiEntityIdAndRelId(@Param("toCiEntityId") Long toCiEntityId,
                                              @Param("relId") Long relId);

    int deleteRelEntityByRelIdFromCiEntityIdToCiEntityId(@Param("relId") Long relId,
                                                         @Param("fromCiEntityId") Long fromCiEntityId,
                                                         @Param("toCiEntityId") Long toCiEntityId);

    void deleteRelEntityBySourceRelEntityId(Long sourceRelEntityId);

}
