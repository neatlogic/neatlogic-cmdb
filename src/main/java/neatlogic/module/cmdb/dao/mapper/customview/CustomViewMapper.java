/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

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

    CustomViewGlobalAttrVo getCustomViewGlobalAttrByUuid(@Param("customViewId") Long customViewId, @Param("uuid") String uuid);

    CustomViewVo getCustomViewById(Long customViewId);

    CustomViewVo getCustomViewByName(String customViewName);

    List<CustomViewVo> searchCustomView(CustomViewVo customViewVo);

    List<CustomViewCiVo> getCustomViewCiByCustomViewId(Long customViewId);

    List<CustomViewCiVo> getCustomViewCiBaseInfoByCustomViewId(Long customViewId);

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
