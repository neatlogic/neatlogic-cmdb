/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.dao.mapper.ci;

import neatlogic.framework.cmdb.crossover.IAttrCrossoverMapper;
import neatlogic.framework.cmdb.dto.attrexpression.AttrExpressionRelVo;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AttrMapper extends IAttrCrossoverMapper {
    AttrVo getAttrByCiIdAndName(@Param("ciId") Long ciId, @Param("attrName") String attrName);

    AttrVo getDeclaredAttrByCiIdAndName(@Param("ciId") Long ciId, @Param("name") String attrName);

    List<AttrVo> getAllNeedTargetCiAttrList();

    List<AttrVo> searchAttr(AttrVo attrVo);

    int searchAttrCount(AttrVo attrVo);

    List<AttrVo> getAttrByValidatorId(Long validatorId);

    List<AttrExpressionRelVo> getExpressionAttrRelByValueCiIdAndAttrIdList(/*@Param("valueCiId") Long valueCiId,*/ @Param("valueAttrIdList") List<Long> valueAttrIdList);

    List<AttrVo> getExpressionAttrByValueCiIdAndAttrIdList(@Param("valueCiId") Long valueCiId, @Param("valueAttrIdList") List<Long> valueAttrIdList);

    List<Long> getExpressionCiIdByValueCiId(Long valueCiId);

    List<AttrVo> getExpressionAttrByValueAttrId(Long valueAttrId);

    List<String> getAttrGroupByCiId(Long ciId);

    AttrVo getAttrById(Long attrId);

    List<AttrVo> getAttrByIdList(@Param("attrIdList") List<Long> attrIdList);

    List<AttrVo> getAttrByCiId(Long ciId);

    List<AttrVo> getDeclaredAttrListByCiId(Long ciId);

    int checkAttrNameIsRepeat(AttrVo attrVo);

    int updateAttr(AttrVo attrVo);

    int insertAttr(AttrVo attrVo);

    int insertAttrExpressionRel(@Param("expressionCiId") Long expressionCiId, @Param("expressionAttrId") Long expressionAttrId, @Param("valueCiId") Long valueCiId, @Param("valueAttrId") Long valueAttrId);

    int deleteAttrById(Long attrId);

    int deleteAttrExpressionRelByValueAttrId(Long valueAttrId);

    int deleteAttrExpressionRelByExpressionAttrId(Long expressionAttrId);

}
