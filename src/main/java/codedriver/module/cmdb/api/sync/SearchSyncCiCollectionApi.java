/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.sync;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.sync.CollectionVo;
import codedriver.framework.cmdb.dto.sync.SyncCiCollectionVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.service.sync.SyncService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchSyncCiCollectionApi extends PrivateApiComponentBase {

    @Resource
    private SyncService syncService;
    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String getToken() {
        return "/cmdb/sync/cicollection/search";
    }

    @Override
    public String getName() {
        return "搜索模型集合映射";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, desc = "模型id"), @Param(name = "collectionType", type = ApiParamType.STRING, desc = "集合类型"), @Param(name = "collectionName", type = ApiParamType.STRING, desc = "集合名称")})
    @Description(desc = "搜索模型集合映射接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        SyncCiCollectionVo syncCiCollectionVo = JSONObject.toJavaObject(jsonObj, SyncCiCollectionVo.class);
        if (StringUtils.isNotBlank(jsonObj.getString("collectionType")) && StringUtils.isBlank(jsonObj.getString("collectionName"))) {
            Query query = new Query();
            Criteria criteria = Criteria.where("collection").is(jsonObj.getString("collectionType"));
            query.addCriteria(criteria);
            List<CollectionVo> collectionList = mongoTemplate.find(query, CollectionVo.class, "_dictionary");
            List<String> collectionNameList = new ArrayList<>();
            for (CollectionVo collectionVo : collectionList) {
                collectionNameList.add(collectionVo.getName());
            }
            syncCiCollectionVo.setCollectionNameList(collectionNameList);
        }
        List<SyncCiCollectionVo> syncCiCollectionList = syncService.searchSyncCiCollection(syncCiCollectionVo);
        return TableResultUtil.getResult(syncCiCollectionList, syncCiCollectionVo);
    }

}
