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

package neatlogic.module.cmdb.dao.mapper.group;

import neatlogic.framework.cmdb.dto.group.CiEntityGroupVo;
import neatlogic.framework.cmdb.dto.group.CiGroupVo;
import neatlogic.framework.cmdb.dto.group.GroupAuthVo;
import neatlogic.framework.cmdb.dto.group.GroupVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GroupMapper {
    List<GroupVo> getDoingGroupByServerId(GroupVo groupVo);

    int getCiEntityCountByGroupId(Long groupId);

    List<GroupVo> searchGroup(GroupVo groupVo);

    int searchGroupCount(GroupVo groupVo);

    GroupVo getGroupById(Long id);

    List<CiGroupVo> getCiGroupByCiId(Long ciId);

    List<GroupVo> getGroupByUserUuid(@Param("userUuid") String userUuid,
                                     @Param("teamUuidList") List<String> teamUuidList, @Param("roleUuidList") List<String> roleUuidList);

    List<Long> getGroupIdByUserUuid(@Param("userUuid") String userUuid,
                                    @Param("teamUuidList") List<String> teamUuidList, @Param("roleUuidList") List<String> roleUuidList);

    List<GroupVo> getActiveGroupByCiId(Long ciId);

    List<Long> getCiIdByGroupIdList(@Param("groupIdList") List<Long> groupIdList, @Param("ciIdList") List<Long> ciIdList, @Param("typeList") List<String> typeList);

    List<Long> getCiEntityIdByGroupIdList(@Param("groupIdList") List<Long> groupIdList,
                                          @Param("ciEntityIdList") List<Long> ciEntityIdList, @Param("typeList") List<String> typeList);

    void insertCiEntityGroup(CiEntityGroupVo ciEntityGroupVo);

    void updateGroupCiEntityCount(GroupVo groupVo);

    void updateGroupStatus(GroupVo groupVo);

    void updateGroup(GroupVo groupVo);

    void insertGroup(GroupVo groupVo);

    void insertCiGroup(CiGroupVo ciGroupVo);

    void insertGroupAuth(GroupAuthVo groupAuthVo);

    void deleteGroupById(Long groupId);

    void deleteGroupAuthByGroupId(Long groupId);

    void deleteCiGroupByGroupId(Long groupId);

    void deleteCiEntityGroupByCiEntityIdAndCiGroupId(@Param("ciEntityId") Long ciEntityId, @Param("ciGroupId") Long ciGroupId);

    void deleteCiEntityGroupByGroupId(Long groupId);
}
