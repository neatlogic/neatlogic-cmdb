package codedriver.module.cmdb.dao.mapper.group;

import java.util.List;

import org.apache.ibatis.annotations.Param;

/**
 * @Author:chenqiwei
 * @Time:Sep 8, 2020
 * @ClassName: GroupMapper
 */
public interface GroupMapper {

    public List<Long> getGroupIdByUserUuid(@Param("userUuid") String userUuid,
        @Param("teamUuidList") List<String> teamUuidList, @Param("roleUuidList") List<String> roleUuidList);

}
