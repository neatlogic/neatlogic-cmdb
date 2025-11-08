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

package neatlogic.module.cmdb.api.sync;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.framework.cmdb.dto.sync.CollectionVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteUnknownCollectionDataApi extends PrivateApiComponentBase {
    @Resource
    private MongoTemplate mongoTemplate;


    @Override
    public String getName() {
        return "nmcas.deleteunknowncollectiondataapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "_id", type = ApiParamType.STRING, isRequired = true, desc = "nfrd.issuewebhookvo.entityfield.name")})
    @Description(desc = "nmcas.deleteunknowncollectiondataapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        paramObj.put("_id", new ObjectId(paramObj.getString("_id")));
        CollectionVo collectionVo = mongoTemplate.findOne(new Query(Criteria.where("name").is("unknown")), CollectionVo.class, "_dictionary");
        if (collectionVo != null) {
            mongoTemplate.remove(paramObj, collectionVo.getCollection());
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/cmdb/sync/collection/unknown/delete";
    }
}
