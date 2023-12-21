/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    GlobalAttrVo getGlobalAttrById(Long id);

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
