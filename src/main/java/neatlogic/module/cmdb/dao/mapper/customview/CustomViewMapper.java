/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.dao.mapper.customview;

import neatlogic.framework.cmdb.dto.customview.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CustomViewMapper {
    Long getCiIdByCustomViewId(Long customViewId);

    CustomViewTemplateVo getCustomViewTemplateById(Long customViewId);

    int checkCustomViewAttrIsExists(CustomViewAttrVo customViewAttrVo);

    CustomViewConstAttrVo getCustomViewConstAttrByUuid(@Param("customViewId") Long customViewId, @Param("uuid") String uuid);

    CustomViewAttrVo getCustomViewAttrByUuid(@Param("customViewId") Long customViewId, @Param("uuid") String uuid);

    CustomViewVo getCustomViewById(Long customViewId);

    CustomViewVo getCustomViewByName(String customViewName);

    List<CustomViewVo> searchCustomView(CustomViewVo customViewVo);

    List<CustomViewCiVo> getCustomViewCiByCustomViewId(Long customViewId);

    List<CustomViewAttrVo> getCustomViewAttrByCustomViewId(CustomViewAttrVo customViewAttrVo);

    List<CustomViewConstAttrVo> getCustomViewConstAttrByCustomViewId(CustomViewConstAttrVo customViewConstAttrVo);

    List<CustomViewLinkVo> getCustomViewLinkByCustomViewId(Long customViewId);

    int checkCustomViewNameIsExists(CustomViewVo customViewVo);

    int searchCustomViewCount(CustomViewVo customViewVo);

    void updateCustomView(CustomViewVo customViewVo);

    void updateCustomViewActive(CustomViewVo customViewVo);

    void insertCustomViewRel(CustomViewRelVo customViewRelVo);

    void insertCiCustomView(@Param("ciId") Long ciId, @Param("customViewId") Long customViewId);

    void insertCustomViewTemplate(CustomViewTemplateVo customViewTemplateVo);

    void insertCustomView(CustomViewVo customViewVo);

    void insertCustomViewCi(CustomViewCiVo customViewCiVo);

    void insertCustomViewConstAttr(CustomViewConstAttrVo customViewConstAttrVo);

    void insertCustomViewAttr(CustomViewAttrVo customViewAttrVo);

    void insertCustomViewLink(CustomViewLinkVo customViewLinkVo);

    void insertCustomViewTag(@Param("customViewId") Long customViewId, @Param("tagId") Long tagId);

    void deleteCustomViewCiByCustomViewId(Long customViewId);

    void deleteCustomViewAttrByCustomViewId(Long customViewId);

    void deleteCustomViewConstAttrByCustomViewId(Long customViewId);

    void deleteCustomViewLinkByCustomViewId(Long customViewId);

    void deleteCustomViewTagByCustomViewId(Long customViewId);

    void deleteCustomViewRelByCustomViewId(Long customViewId);

    void deleteCiCustomViewByCustomViewId(Long customViewId);

    void deleteCustomViewById(Long customViewId);

    void buildCustomView(String sql);

    void dropCustomView(String viewName);

    void deleteCustomViewTemplateById(Long customViewId);
}
