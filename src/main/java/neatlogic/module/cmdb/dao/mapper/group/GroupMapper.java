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
