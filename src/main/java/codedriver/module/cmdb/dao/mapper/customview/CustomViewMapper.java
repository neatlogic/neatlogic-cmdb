/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.customview;

import codedriver.framework.cmdb.dto.customview.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CustomViewMapper {
    CustomViewAttrVo getCustomViewAttrByUuid(String uuid);

    CustomViewVo getCustomViewById(Long customViewId);

    List<CustomViewVo> searchCustomView(CustomViewVo customViewVo);

    List<CustomViewCiVo> getCustomViewCiByCustomViewId(Long customViewId);

    List<CustomViewAttrVo> getCustomViewAttrByCustomViewId(CustomViewAttrVo customViewAttrVo);

    List<CustomViewLinkVo> getCustomViewLinkByCustomViewId(Long customViewId);

    int searchCustomViewCount(CustomViewVo customViewVo);

    void updateCustomView(CustomViewVo customViewVo);

    void updateCustomViewActive(CustomViewVo customViewVo);

    void insertCustomViewRel(CustomViewRelVo customViewRelVo);

    void insertCustomView(CustomViewVo customViewVo);

    void insertCustomViewCi(CustomViewCiVo customViewCiVo);

    void insertCustomViewAttr(CustomViewAttrVo customViewAttrVo);

    void insertCustomViewLink(CustomViewLinkVo customViewLinkVo);

    void insertCustomViewTag(@Param("customViewId") Long customViewId, @Param("tagId") Long tagId);

    void deleteCustomViewCiByCustomViewId(Long customViewId);

    void deleteCustomViewAttrByCustomViewId(Long customViewId);

    void deleteCustomViewLinkByCustomViewId(Long customViewId);

    void deleteCustomViewTagByCustomViewId(Long customViewId);

    void deleteCustomViewRelByCustomViewId(Long customViewId);

    void buildCustomView(String sql);
}
