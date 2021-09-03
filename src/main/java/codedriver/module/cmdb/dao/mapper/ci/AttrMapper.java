/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.ci;

import codedriver.framework.cmdb.dto.attrexpression.AttrExpressionRelVo;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AttrMapper {
    List<AttrVo> searchAttr(AttrVo attrVo);

    int searchAttrCount(AttrVo attrVo);

    List<AttrVo> getAttrByValidatorId(Long validatorId);

    List<AttrExpressionRelVo> getExpressionAttrRelByValueCiIdAndAttrIdList(@Param("valueCiId") Long valueCiId, @Param("valueAttrIdList") List<Long> valueAttrIdList);

    List<AttrVo> getExpressionAttrByValueCiIdAndAttrIdList(@Param("valueCiId") Long valueCiId, @Param("valueAttrIdList") List<Long> valueAttrIdList);

    List<Long> getExpressionCiIdByValueCiId(Long valueCiId);

    List<AttrVo> getExpressionAttrByValueAttrId(Long valueAttrId);

    List<String> getAttrGroupByCiId(Long ciId);

    AttrVo getAttrById(Long attrId);

    List<AttrVo> getAttrByIdList(@Param("attrIdList") List<Long> attrIdList);

    List<AttrVo> getAttrByCiId(Long ciId);


    int checkAttrNameIsRepeat(AttrVo attrVo);

    int updateAttr(AttrVo attrVo);

    int insertAttr(AttrVo attrVo);

    int insertAttrExpressionRel(@Param("expressionCiId") Long expressionCiId, @Param("expressionAttrId") Long expressionAttrId, @Param("valueCiId") Long valueCiId, @Param("valueAttrId") Long valueAttrId);

    int deleteAttrById(Long attrId);

    int deleteAttrExpressionRelByValueAttrId(Long valueAttrId);

    int deleteAttrExpressionRelByExpressionAttrId(Long expressionAttrId);

}
