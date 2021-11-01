/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.cientity;

import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 *
 */
public interface AttrEntityMapper {
    List<AttrEntityVo> getAttrEntityByFromCiIdAndAttrId(AttrEntityVo attrEntityVo);

    List<AttrEntityVo> getAttrEntityByFromCiEntityIdAndAttrId(@Param("fromCiEntityId") Long fromCiEntityId, @Param("attrId") Long attrId, @Param("limit") Integer limit);

    void updateAttrEntityFromIndex(AttrEntityVo attrEntityVo);

    void clearAttrEntityFromIndex(@Param("attrId") Long attrId, @Param("ciEntityId") Long ciEntityId);
}
