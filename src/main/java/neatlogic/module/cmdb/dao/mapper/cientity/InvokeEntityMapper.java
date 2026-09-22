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

import neatlogic.framework.cmdb.crossover.IInvokeEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.InvokeEntityVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InvokeEntityMapper extends IInvokeEntityCrossoverMapper {

    List<Long> getCiEntityIdListByAttrId(Long attrId);

    List<InvokeEntityVo> getInvokeEntityListByCiEntityIdAndAttrId(@Param("ciEntityId") Long ciEntityId, @Param("attrId") Long attrId);

    List<InvokeEntityVo> getInvokeEntityListByAttrId(Long attrId);

    // 视图值为聚合的invoke_id，读取匹配索引后按配置项、属性分别还原。
    List<InvokeEntityVo> getInvokeEntityListByInvokeIdList(@Param("invokeIdList") List<Long> invokeIdList, @Param("attrIdList") List<Long> attrIdList);

    int insertInvokeEntityList(@Param("invokeEntityList") List<InvokeEntityVo> invokeEntityList);

    int deleteInvokeEntityByCiEntityIdAndAttrId(@Param("ciEntityId") Long ciEntityId, @Param("attrId") Long attrId);

    int deleteInvokeEntityByAttrId(Long attrId);
}
