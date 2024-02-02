/*
 * Copyright(c) 2024 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
