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

package neatlogic.module.cmdb.dao.mapper.globalattr;

import neatlogic.framework.cmdb.crossover.IGlobalAttrCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.GlobalAttrEntityVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrItemVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GlobalAttrMapper extends IGlobalAttrCrossoverMapper {

    int checkGlobalAttrNameIsUsed(GlobalAttrVo globalAttrVo);

    int checkGlobalAttrIsUsed(Long attrId);

    int checkGlobalAttrItemIsUsed(Long itemId);

    List<GlobalAttrVo> getGlobalAttrByCiId(Long ciId);

    List<GlobalAttrVo> getGlobalAttrByIdList(GlobalAttrVo globalAttrVo);

    GlobalAttrVo getGlobalAttrById(Long id);

    GlobalAttrVo getGlobalAttrByName(String name);

    List<GlobalAttrItemVo> getAllGlobalAttrItemByAttrId(Long attrId);

    List<GlobalAttrItemVo> searchGlobalAttrItem(GlobalAttrItemVo globalAttrItemVo);

    GlobalAttrItemVo getGlobalAttrItemById(Long id);

    GlobalAttrItemVo getGlobalAttrItemByAttrIdAndValue(@Param("attrId") Long attrId, @Param("value") String value);

    List<GlobalAttrVo> searchGlobalAttr(GlobalAttrVo globalAttrVo);

    List<GlobalAttrEntityVo> getGlobalAttrByCiEntityId(Long ciEntityId);

    List<GlobalAttrEntityVo> getGlobalAttrByCiEntityIdList(@Param("ciEntityIdList") List<Long> ciEntityIdList);

    void updateGlobalAttr(GlobalAttrVo globalAttrVo);

    void updateGlobalAttrItem(GlobalAttrItemVo globalAttrItemVo);

    void insertGlobalAttr(GlobalAttrVo globalAttrVo);

    void insertGlobalAttrItem(GlobalAttrItemVo globalAttrItemVo);

    void insertGlobalAttrEntityItem(GlobalAttrEntityVo globalAttrEntityVo);

    void deleteGlobalAttrEntityByCiEntityId(Long ciEntityId);

    void deleteGlobalAttrById(Long id);

    void deleteGlobalAttrItemById(Long id);

    void deleteGlobalAttrEntityByCiEntityIdAndAttrId(@Param("ciEntityId") Long ciEntityId, @Param("attrId") Long attrId);
}
