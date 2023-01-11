/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.cmdb.api.discovery;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.auth.label.SYNC_MODIFY;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
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

