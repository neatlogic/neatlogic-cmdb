/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.ci;

import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.dto.ci.CiTypeVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.customview.CustomViewVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CiMapper extends ICiCrossoverMapper {

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

    /**
     * 查找所有子模型包括自己
     *
     * @param lft 父模型左编码
     * @param rht 父模型右编码
     * @return 模型列表
     */
    List<CiVo> getDownwardCiListByLR(@Param("lft") Integer lft, @Param("rht") Integer rht);

    //List<Long> getCiNameExpressionCiIdByAttrId(Long attrId);

    List<CiVo> getAllCi(@Param("idList") List<Long> idList);

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

    void updateCi(CiVo ciVo);

    void updateCiNameAttrId(CiVo ciVo);
    // int updateCiNameExpression(@Param("ciId") Long ciId, @Param("nameExpression") String nameExpression);

    void insertCi(CiVo ciVo);

    void insertCiUnique(@Param("ciId") Long ciId, @Param("attrId") Long attrId);

    //int insertCiNameExpression(@Param("ciId") Long ciId, @Param("attrId") Long attrId);

    void deleteCiById(Long ciId);

    void deleteCiUniqueByCiId(Long ciId);

    //int deleteCiNameExpressionByCiId(Long ciId);
}
