/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.group;

import codedriver.framework.cmdb.dto.group.CiEntityGroupVo;
import codedriver.framework.cmdb.dto.group.CiGroupVo;
import codedriver.framework.cmdb.dto.group.GroupVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GroupMapper {
    List<GroupVo> searchGroup(GroupVo groupVo);

    int searchGroupCount(GroupVo groupVo);

    GroupVo getGroupById(Long id);

    List<CiGroupVo> getCiGroupByCiId(Long ciId);

    List<Long> getGroupIdByUserUuid(@Param("userUuid") String userUuid,
                                    @Param("teamUuidList") List<String> teamUuidList, @Param("roleUuidList") List<String> roleUuidList);

    void insertCiEntityGroup(CiEntityGroupVo ciEntityGroupVo);

    void updateGroup(GroupVo groupVo);

    void insertGroup(GroupVo groupVo);

    void insertCiGroup(CiGroupVo ciGroupVo);

    void deleteCiGroupByGroupId(Long groupId);

    void deleteCiEntityGroupByCiEntityIdAndCiGroupId(@Param("ciEntityId") Long ciEntityId, @Param("ciGroupId") Long ciGroupId);

}
