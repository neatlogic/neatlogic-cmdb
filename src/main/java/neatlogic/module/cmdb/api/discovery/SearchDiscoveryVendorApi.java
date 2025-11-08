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
package neatlogic.module.cmdb.api.discovery;

import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * @author longrf
 * @date 2023/1/4 16:54
 */

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDiscoveryVendorApi extends PrivateApiComponentBase {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "获取自动发现厂商列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", desc = "关键词", type = ApiParamType.STRING)
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.JSONARRAY, desc = "厂商列表")
    })
    @Description(desc = "获取自动发现厂商列表接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String keyword = paramObj.getString("keyword");
        MongoCollection<Document> collection = mongoTemplate.getCollection("_discovery_vendor");
        Document searchDoc = new Document();
        if (StringUtils.isNotBlank(keyword)) {
            Pattern pattern = Pattern.compile("^.*" + keyword + ".*$", Pattern.CASE_INSENSITIVE);
            Document nameDoc = new Document();
            nameDoc.put("VENDOR", pattern);
            searchDoc.put("$and", Collections.singletonList(nameDoc));
        }
        FindIterable<Document> collectionList = collection.find(searchDoc);
        return collectionList.into(new ArrayList<>());
    }

    @Override
    public String getToken() {
        return "/cmdb/discovery/vendor/search";
    }
}

