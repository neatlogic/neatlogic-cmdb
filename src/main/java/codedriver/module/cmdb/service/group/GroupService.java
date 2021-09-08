/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.group;

import codedriver.framework.cmdb.dto.group.GroupVo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface GroupService {
    List<Long> getCurrentUserGroupIdList();

    @Transactional
    void insertGroup(GroupVo groupVo);

    @Transactional
    void updateGroup(GroupVo groupVo);
}
