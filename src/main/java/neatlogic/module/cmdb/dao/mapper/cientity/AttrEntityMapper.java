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

import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.cientity.AttrEntityVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 *
 */
public interface AttrEntityMapper {
    List<String> getAttrValueByCiId(AttrVo attrVo);

    List<AttrEntityVo> getAttrEntityByFromCiIdAndAttrId(AttrEntityVo attrEntityVo);

    List<AttrEntityVo> getAttrEntityByToCiEntityId(Long ciEntityId);

    List<AttrEntityVo> getAttrEntityByFromCiEntityIdAndAttrId(@Param("fromCiEntityId") Long fromCiEntityId, @Param("attrId") Long attrId, @Param("limit") Long limit);

    void updateAttrEntityFromIndex(AttrEntityVo attrEntityVo);

    void clearAttrEntityFromIndex(@Param("attrId") Long attrId, @Param("ciEntityId") Long ciEntityId);
}
