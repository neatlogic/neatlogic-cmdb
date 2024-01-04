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

package neatlogic.module.cmdb.dao.mapper.ci;

import neatlogic.framework.cmdb.dto.ci.CiAuthVo;

import java.util.List;

/**
 * 
 * @Author:chenqiwei
 * @Time:Aug 15, 2020
 * @ClassName: CiMapper
 * @Description: TODO
 */
public interface CiAuthMapper {
    public List<CiAuthVo> getCiAuthByCiId(Long ciId);

    List<CiAuthVo> getCiAuthByCiIdList(List<Long> ciIdList);

    public int insertCiAuth(CiAuthVo ciAuthVo);

    public int deleteCiAuthByCiId(Long ciId);

}
