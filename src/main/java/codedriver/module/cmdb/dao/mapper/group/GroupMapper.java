/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.group;

import codedriver.framework.cmdb.dto.group.CiEntityGroupVo;
import codedriver.framework.cmdb.dto.group.CiGroupVo;
import codedriver.framework.cmdb.dto.group.GroupAuthVo;
import codedriver.framework.cmdb.dto.group.GroupVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GroupMapper {
    List<GroupVo> getDoingGroupByServerId(GroupVo groupVo);

    int getCiEntityCountByGroupId(Long groupId);

    List<GroupVo> searchGroup(GroupVo groupVo);

    int searchGroupCount(GroupVo groupVo);

    GroupVo getGroupById(Long id);

    List<CiGroupVo> getCiGroupByCiId(Long ciId);

    List<Long> getGroupIdByUserUuid(@Param("userUuid") String userUuid,
                                    @Param("teamUuidList") List<String> teamUuidList, @Param("roleUuidList") List<String> roleUuidList);

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
