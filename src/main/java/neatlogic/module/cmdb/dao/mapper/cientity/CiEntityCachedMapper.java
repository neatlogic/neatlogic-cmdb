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

package neatlogic.module.cmdb.dao.mapper.cientity;

import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CiEntityCachedMapper  {
    /**
     * 获取配置项基本信息
     *
     * @param ciEntityId 配置项id
     * @return CiEntityVo
     */
    CiEntityVo getCiEntityBaseInfoById(Long ciEntityId);
    /**
     * 根据id列表返回多个配置项基本信息
     *
     * @param ciEntityIdList 配置项id列表
     * @return 多个CiEntityVo
     */
    List<CiEntityVo> getCiEntityBaseInfoByIdList(@Param("ciEntityIdList") List<Long> ciEntityIdList);

    /**
     * 根据id列表返回多个虚拟配置项基本信息
     *
     * @param ciEntityVo 条件
     * @return 多个配置项
     */
    List<CiEntityVo> getVirtualCiEntityBaseInfoByIdList(CiEntityVo ciEntityVo);
}
