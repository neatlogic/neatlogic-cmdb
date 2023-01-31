/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.dao.mapper.tag;

import neatlogic.framework.cmdb.dto.tag.TagVo;

import java.util.List;

public interface CmdbTagMapper {
    List<TagVo> searchTagList(TagVo TagVo);

    int searchTagListCount(TagVo cmdbTagVo);

    void insertCmdbTag(TagVo tagVo);

}
