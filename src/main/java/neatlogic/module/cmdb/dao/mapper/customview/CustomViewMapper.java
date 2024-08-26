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

package neatlogic.module.cmdb.dao.mapper.customview;

import neatlogic.framework.cmdb.dto.customview.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CustomViewMapper {
    Long getCiIdByCustomViewId(Long customViewId);

    CustomViewTemplateVo getCustomViewTemplateById(Long customViewId);

    int checkCustomViewGlobalAttrIsExists(CustomViewGlobalAttrVo customViewGlobalAttr);

    int checkCustomViewAttrIsExists(CustomViewAttrVo customViewAttrVo);

    CustomViewConstAttrVo getCustomViewConstAttrByUuid(@Param("customViewId") Long customViewId, @Param("uuid") String uuid);

    CustomViewAttrVo getCustomViewAttrByUuid(@Param("customViewId") Long customViewId, @Param("uuid") String uuid);

    CustomViewVo getCustomViewById(Long customViewId);

    CustomViewVo getCustomViewByName(String customViewName);

    List<CustomViewVo> searchCustomView(CustomViewVo customViewVo);

    List<CustomViewCiVo> getCustomViewCiByCustomViewId(Long customViewId);

    List<CustomViewAttrVo> getCustomViewAttrByCustomViewId(CustomViewAttrVo customViewAttrVo);

    List<CustomViewAttrVo> getCustomViewAttrByUuidList(List<String> uuidList);

    List<CustomViewConstAttrVo> getCustomViewConstAttrByCustomViewId(CustomViewConstAttrVo customViewConstAttrVo);

    List<CustomViewLinkVo> getCustomViewLinkByCustomViewId(Long customViewId);

    int checkCustomViewNameIsExists(CustomViewVo customViewVo);

    List<CustomViewGlobalAttrVo> getCustomViewGlobalAttrByCustomViewId(CustomViewGlobalAttrVo customViewGlobalAttrVo);

    List<Long> getAllIdList();

    int searchCustomViewCount(CustomViewVo customViewVo);

    void updateCustomView(CustomViewVo customViewVo);

    void updateCustomViewActive(CustomViewVo customViewVo);

    void insertCustomViewRel(CustomViewRelVo customViewRelVo);

    void insertCiCustomView(@Param("ciId") Long ciId, @Param("customViewId") Long customViewId);

    void insertCustomViewTemplate(CustomViewTemplateVo customViewTemplateVo);

    void insertCustomView(CustomViewVo customViewVo);

    void insertCustomViewCi(CustomViewCiVo customViewCiVo);

    void insertCustomViewConstAttr(CustomViewConstAttrVo customViewConstAttrVo);

    void insertCustomViewGlobalAttr(CustomViewGlobalAttrVo customViewGlobalAttrVo);

    void insertCustomViewAttr(CustomViewAttrVo customViewAttrVo);

    void insertCustomViewLink(CustomViewLinkVo customViewLinkVo);

    void insertCustomViewTag(@Param("customViewId") Long customViewId, @Param("tagId") Long tagId);

    void insertCustomViewAuth(CustomViewAuthVo customViewAuthVo);

    void deleteCustomViewCiByCustomViewId(Long customViewId);

    void deleteCustomViewGlobalAttrByCustomViewId(Long customViewId);

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

    void deleteCustomViewAuthByCustomViewId(Long customViewId);
}
