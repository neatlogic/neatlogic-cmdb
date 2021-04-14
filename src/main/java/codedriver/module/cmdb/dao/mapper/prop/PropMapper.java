/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.prop;

import java.util.List;

import codedriver.framework.cmdb.dto.prop.PropVo;

public interface PropMapper {
    public int checkPropIsInUsed(Long propId);

    public int checkPropNameIsExists(PropVo propVo);

    public int checkPropLabelIsExists(PropVo propVo);

    public PropVo getPropByAttrId(Long attrId);

    public PropVo getPropById(Long propId);

    public List<PropVo> searchProp(PropVo propVo);

    public int searchPropCount(PropVo propVo);

    public int insertProp(PropVo propVo);

    public int updateProp(PropVo propVo);

    public int deletePropById(Long propId);
}
