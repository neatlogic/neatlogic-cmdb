/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.ci;

import codedriver.framework.cmdb.dto.ci.AttrVo;

import java.util.List;

public interface AttrMapper {

    List<String> getAttrGroupByCiId(Long ciId);

    AttrVo getAttrById(Long attrId);

    List<AttrVo> getAttrByCiId(Long ciId);


    int checkAttrNameIsRepeat(AttrVo attrVo);

    int updateAttr(AttrVo attrVo);

    int insertAttr(AttrVo attrVo);

    int deleteAttrById(Long attrId);

    int deleteAttrByCiId(Long ciId);
}
