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

package neatlogic.module.cmdb.dao.mapper.sync;

import neatlogic.framework.cmdb.dto.sync.ObjectVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ObjectMapper {
    ObjectVo getObjectByCategoryAndType(@Param("category") String category, @Param("type") String type);

    int checkObjectIsExists(ObjectVo objectVo);

    int checkObjectCiIdIsExists(ObjectVo objectVo);

    ObjectVo getObjectById(Long id);

    List<ObjectVo> searchObject(ObjectVo objectVo);

    int searchObjectCount(ObjectVo objectVo);

    void insertObject(ObjectVo objectVo);

    void updateObject(ObjectVo objectVo);

    void deleteObject(Long id);
}
