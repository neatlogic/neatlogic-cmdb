/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.customview;

import codedriver.framework.cmdb.dto.customview.CustomViewAttrVo;
import codedriver.framework.cmdb.dto.customview.CustomViewCiVo;
import codedriver.framework.cmdb.dto.customview.CustomViewLinkVo;
import codedriver.framework.cmdb.dto.customview.CustomViewVo;

import java.util.List;

public interface CustomViewMapper {
    CustomViewVo getCustomViewById(Long customViewId);

    List<CustomViewVo> searchCustomView(CustomViewVo customViewVo);

    int searchCustomViewCount(CustomViewVo customViewVo);

    void updateCustomView(CustomViewVo customViewVo);

    void updateCustomViewActive(CustomViewVo customViewVo);

    void insertCustomView(CustomViewVo customViewVo);

    void insertCustomViewCi(CustomViewCiVo customViewCiVo);

    void insertCustomViewAttr(CustomViewAttrVo customViewAttrVo);

    void insertCustomViewLink(CustomViewLinkVo customViewLinkVo);

    void deleteCustomViewCiByCustomViewId(Long customViewId);

    void deleteCustomViewLinkByCustomViewId(Long customViewId);

    void buildCustomView(String sql);
}
