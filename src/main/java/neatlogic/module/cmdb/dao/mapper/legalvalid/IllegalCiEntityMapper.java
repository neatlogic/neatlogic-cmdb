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

package neatlogic.module.cmdb.dao.mapper.legalvalid;

import neatlogic.framework.cmdb.dto.legalvalid.IllegalCiEntityVo;
import neatlogic.framework.cmdb.dto.legalvalid.LegalValidVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface IllegalCiEntityMapper {
    /**
     * 获取不合规配置项数量
     *
     * @param ciId 模型id
     * @return 数量
     */
    List<LegalValidVo> getIllegalCiEntityCountByCiId(Long ciId);

    List<IllegalCiEntityVo> searchIllegalCiEntity(IllegalCiEntityVo illegalCiEntityVo);

    List<Long> searchIllegalCiEntityId(IllegalCiEntityVo illegalCiEntityVo);

    int searchIllegalCiEntityCount(IllegalCiEntityVo illegalCiEntityVo);

    void insertCiEntityIllegal(IllegalCiEntityVo illegalCiEntityVo);

    void deleteCiEntityIllegal(@Param("ciEntityId") Long ciEntityId, @Param("legalValidId") Long legalValidId);


}
