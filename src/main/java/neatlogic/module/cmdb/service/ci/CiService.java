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

package neatlogic.module.cmdb.service.ci;

import org.springframework.transaction.annotation.Transactional;

import neatlogic.framework.cmdb.dto.ci.CiVo;

import java.util.List;

public interface CiService {
    CiVo getCiById(Long id);

    @Transactional
    void insertCi(CiVo ciVo);

    void buildCiView(CiVo ciVo);

    @Transactional
    void updateCiUnique(Long ciId, List<Long> attrIdList);

    @Transactional
    void updateCiNameAttrId(CiVo ciVo);

    @Transactional
    void updateCiNameExpression(Long ciId, String nameExpression);

    @Transactional
    void updateCi(CiVo ciVo);

    @Transactional
    int deleteCi(Long ciId);


    CiVo getCiByName(String ciName);

    /**
     * 创建ci动态表和视图
     */
    void initCiTableView();
}
