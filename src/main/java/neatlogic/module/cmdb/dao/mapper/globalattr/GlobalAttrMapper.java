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
