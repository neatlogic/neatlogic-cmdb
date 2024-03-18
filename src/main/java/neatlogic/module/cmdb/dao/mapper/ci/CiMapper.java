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

package neatlogic.module.cmdb.dao.mapper.ci;

import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiTypeVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.module.cmdb.annotation.CiId;
import neatlogic.module.cmdb.annotation.DeleteCiView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CiMapper extends ICiCrossoverMapper {
    List<Long> getDownwardCiIdListByLR(@Param("lft") Integer lft, @Param("rht") Integer rht);

    CiVo getCiBaseInfoById(Long ciId);

    List<CiVo> getCiTree();

    Long getCiLock(Long ciId);

    String getCiViewXmlById(Long ciId);

    List<Long> getCiUniqueByCiId(Long ciId);

    int getAttrCountByCiId(Long ciId);

    int getRelCountByCiId(Long ciId);

    List<CustomViewVo> getCustomViewByCiId(Long ciId);

    List<CiVo> getCiBaseInfoByCiEntityIdList(@Param("ciEntityIdList") List<Long> ciEntityIdList);

    /**
     * 查找所有父模型包括自己
     *
     * @param lft 子模型左编码
     * @param rht 子模型右编码
     * @return 模型列表
     */
    List<CiVo> getUpwardCiListByLR(@Param("lft") Integer lft, @Param("rht") Integer rht);

    List<Long> getUpwardCiIdListByLR(@Param("lft") Integer lft, @Param("rht") Integer rht);

    /**
     * 查找所有子模型包括自己
     *
     * @param lft 父模型左编码
     * @param rht 父模型右编码
     * @return 模型列表
     */
    List<CiVo> getDownwardCiListByLR(@Param("lft") Integer lft, @Param("rht") Integer rht);

    List<CiVo> getBatchDownwardCiListByCiList(@Param("ciList") List<CiVo> ciList);

    List<CiVo> getDownwardCiEntityQueryCiListByLR(@Param("lft") Integer lft, @Param("rht") Integer rht, @Param("authenticationInfo") AuthenticationInfoVo authenticationInfo, @Param("isHasAuth") boolean isHasAuth);

    //List<Long> getCiNameExpressionCiIdByAttrId(Long attrId);

    List<CiVo> getAllCi(@Param("idList") List<Long> idList);

    List<CiVo> getAllAuthCi(AuthenticationInfoVo authenticationInfoVo);

    List<CiVo> getCiListByNameList(List<String> nameList);

    List<CiVo> getCiByTargetCiId(Long ciId);

    List<CiVo> getCiByFromCiId(Long ciId);

    List<CiVo> getCiByToCiId(Long ciId);

    List<CiVo> getCiByIdList(@Param("ciIdList") List<Long> ciIds);

    int checkCiNameIsExists(CiVo ciVo);

    int checkCiLabelIsExists(CiVo ciVo);

    List<CiTypeVo> searchCiTypeCi(CiVo ciVo);

    CiVo getCiById(Long ciId);

    CiVo getCiByName(String ciName);

    CiVo getCiByLabel(String ciLabel);

    List<CiVo> getCiListByLabelList(List<String> ciLabelList);

    int getVirtualCiCount();

    List<CiVo> getVirtualCiList(BasePageVo searchVo);

    void updateCi(CiVo ciVo);

    void updateCiNameAttrId(CiVo ciVo);
    // int updateCiNameExpression(@Param("ciId") Long ciId, @Param("nameExpression") String nameExpression);

    void insertCi(CiVo ciVo);

    void insertCiUnique(@Param("ciId") Long ciId, @Param("attrId") Long attrId);

    void saveCiTreeItem(CiVo ciVo);
    //int insertCiNameExpression(@Param("ciId") Long ciId, @Param("attrId") Long attrId);

    @DeleteCiView
    void deleteCiById(@CiId Long ciId);

    void deleteCiUniqueByCiId(Long ciId);

    //int deleteCiNameExpressionByCiId(Long ciId);
}
