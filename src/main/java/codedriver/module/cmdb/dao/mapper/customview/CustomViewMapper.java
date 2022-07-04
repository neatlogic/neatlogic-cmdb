/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.customview;

import codedriver.framework.cmdb.dto.customview.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CustomViewMapper {
    CustomViewConstAttrVo getCustomViewConstAttrByUuid(String uuid);

    CustomViewAttrVo getCustomViewAttrByUuid(String uuid);

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

    void deleteCustomViewById(Long customViewId);

    void buildCustomView(String sql);

    void dropCustomView(String viewName);
}
