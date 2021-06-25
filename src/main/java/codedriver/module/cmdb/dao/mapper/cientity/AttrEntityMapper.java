/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.cientity;

import codedriver.framework.elasticsearch.annotation.ESParam;
import codedriver.framework.elasticsearch.annotation.ESSearch;
import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AttrEntityMapper {

    AttrEntityVo getAttrEntityByCiEntityIdAndAttrId(@Param("ciEntityId") Long ciEntityId,
                                                    @Param("attrId") Long attrId);

    List<AttrEntityVo> getAttrEntityByAttrIdAndValue(AttrEntityVo attrEntityVo);


    List<AttrEntityVo> getAttrEntityByCiEntityId(Long ciEntityId);

    List<AttrEntityVo> searchAttrEntityByCiEntityIdList(@Param("idList") List<Long> idList,
                                                        @Param("attrIdList") List<Long> attrIdList);

    @ESSearch
    int insertAttrEntity(@ESParam("cientity") AttrEntityVo attrEntityVo);

    @ESSearch
    int deleteAttrEntity(@ESParam("cientity") AttrEntityVo attrEntityVo);

}
