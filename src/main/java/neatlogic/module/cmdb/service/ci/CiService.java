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

package neatlogic.module.cmdb.service.ci;

import neatlogic.framework.cmdb.dto.ci.CiVo;
import org.springframework.transaction.annotation.Transactional;

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
    void updateCi(CiVo ciVo);

    @Transactional
    int deleteCi(Long ciId);


    CiVo getCiByName(String ciName);

    /**
     * 创建ci动态表和视图
     */
    void initCiTableView();
}
