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

package neatlogic.module.cmdb.dao.mapper.resourcecenter;

import neatlogic.framework.cmdb.dto.resourcecenter.ResourceTagVo;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ResourceTagMapper {

    int getTagCount(TagVo searchVo);

    List<TagVo> getTagListForSelect(TagVo searchVo);

    List<String> getTagNameListForSelect(TagVo searchVo);

    List<TagVo> searchTag(TagVo vo);

    int searchTagCount(TagVo vo);

    int checkTagNameIsRepeats(TagVo vo);

    int checkTagIsExistsById(Long id);

    TagVo getTagById(Long id);

    TagVo getTagByName(String name);

    int checkTagHasBeenReferredById(Long id);

    List<TagVo> getTagListByTagNameList(List<String> tagNameList);

    List<TagVo> getTagListByIdList(List<Long> idList);

    List<ResourceTagVo> getResourceTagListByResourceIdList(List<Long> resourceIdList);

    List<TagVo> searchTagListByIdList(List<Long> tagIdList);

    int updateTag(TagVo vo);

    int insertTag(TagVo vo);

    int insertIgnoreResourceTag(List<ResourceTagVo> resourceTagVoList);

    int deleteTagById(Long id);

    int deleteResourceTagByResourceId(Long resourceId);

    int deleteResourceTagByResourceIdAndTagIdList(@Param("resourceIdList") List<Long> resourceIdList, @Param("tagIdList") List<Long> tagIdList);
}
