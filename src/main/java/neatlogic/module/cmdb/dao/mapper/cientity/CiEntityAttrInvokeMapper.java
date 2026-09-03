/*
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package neatlogic.module.cmdb.dao.mapper.cientity;

import neatlogic.framework.cmdb.dto.cientity.AttrInvokeVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CiEntityAttrInvokeMapper {

    List<AttrInvokeVo> getAttrInvokeListByAttrId(Long attrId);

    List<AttrInvokeVo> getAttrInvokeListByCiEntityIdAndAttrId(@Param("ciEntityId") Long ciEntityId, @Param("attrId") Long attrId);

    List<AttrInvokeVo> getAttrInvokeListByAttrTypeAndTypeAndInvokeIdList(@Param("attrType") String attrType, @Param("type") String type, @Param("invokeIdList") List<Long> invokeIdList);

    List<AttrInvokeVo> getAttrInvokeListByCiEntityIdListAndAttrIdList(@Param("ciEntityIdList") List<Long> ciEntityIdList, @Param("attrIdList") List<Long> attrIdList);

    int insertAttrInvokeList(@Param("attrInvokeList") List<AttrInvokeVo> attrInvokeList);

    int updateAttrInvokeList(@Param("attrInvokeList") List<AttrInvokeVo> attrInvokeList);

    int deleteAttrInvokeByCiEntityIdAndAttrId(@Param("ciEntityId") Long ciEntityId, @Param("attrId") Long attrId);

    int deleteAttrInvokeByCiEntityId(Long ciEntityId);

    int deleteAttrInvokeByAttrId(Long attrId);
}
