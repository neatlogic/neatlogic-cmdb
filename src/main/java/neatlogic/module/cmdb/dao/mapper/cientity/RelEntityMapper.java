/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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

    List<RelEntityVo> getRelEntityByRelIdAndLikeToCiEntityName(RelEntityVo relEntityVo);

    List<RelEntityVo> getRelEntityByRelIdAndToCiEntityName(RelEntityVo relEntityVo);

    List<RelEntityVo> getRelEntityByRelIdAndLikeFromCiEntityName(RelEntityVo relEntityVo);

    List<RelEntityVo> getRelEntityByRelIdAndFromCiEntityName(RelEntityVo relEntityVo);

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
