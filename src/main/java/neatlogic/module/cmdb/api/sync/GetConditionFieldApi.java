/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
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

package neatlogic.module.cmdb.api.sync;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.sync.CollectionVo;
import neatlogic.framework.cmdb.dto.sync.SyncCiCollectionVo;
import neatlogic.framework.cmdb.dto.sync.SyncFieldVo;
import neatlogic.framework.cmdb.exception.sync.CollectionNotFoundException;
import neatlogic.framework.cmdb.exception.sync.SyncCiCollectionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.sync.SyncMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetConditionFieldApi extends PrivateApiComponentBase {
    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private SyncMapper syncMapper;

    @Override
    public String getName() {
        return "nmcas.getconditionfieldapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "id")})
    @Description(desc = "nmcas.getconditionfieldapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        SyncCiCollectionVo ciCollectionVo = syncMapper.getSyncCiCollectionById(id);
        if (ciCollectionVo == null) {
            throw new SyncCiCollectionNotFoundException(id);
        }
        CollectionVo collectionVo = mongoTemplate.findOne(new Query(Criteria.where("name").is(ciCollectionVo.getCollectionName())), CollectionVo.class, "_dictionary");
        if (collectionVo == null) {
            throw new CollectionNotFoundException(ciCollectionVo.getCollectionName());
        }
        List<SyncFieldVo> fieldList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(collectionVo.getFields())) {
            for (int i = 0; i < collectionVo.getFields().size(); i++) {
                JSONObject fieldObj = collectionVo.getFields().getJSONObject(i);
                SyncFieldVo syncFieldVo = JSONObject.toJavaObject(fieldObj, SyncFieldVo.class);
                //有条件表达式的字段才能作为搜索条件
                if (CollectionUtils.isNotEmpty(syncFieldVo.getExpressionList())) {
                    fieldList.add(syncFieldVo);
                }
            }
        }
        fieldList.sort(Comparator.comparing(SyncFieldVo::getName));
        return fieldList;
    }

    @Override
    public String getToken() {
        return "/cmdb/sync/condition/field/get";
    }
}
