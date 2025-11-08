/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

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
