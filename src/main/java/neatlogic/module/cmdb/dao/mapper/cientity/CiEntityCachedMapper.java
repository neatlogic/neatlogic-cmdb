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
