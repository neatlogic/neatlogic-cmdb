/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.sync;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.sync.CollectionVo;
import codedriver.framework.cmdb.dto.sync.SyncCiCollectionVo;
import codedriver.framework.cmdb.dto.sync.SyncFieldVo;
import codedriver.framework.cmdb.exception.sync.SyncCiCollectionNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.sync.SyncMapper;
import com.alibaba.fastjson.JSONObject;
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
        return "获取集合搜索条件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "集合映射id")})
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        SyncCiCollectionVo ciCollectionVo = syncMapper.getSyncCiCollectionById(id);
        if (ciCollectionVo == null) {
            throw new SyncCiCollectionNotFoundException(id);
        }
        CollectionVo collectionVo = mongoTemplate.findOne(new Query(Criteria.where("type").is(ciCollectionVo.getCollectionName())), CollectionVo.class, "_dictionary");
        List<SyncFieldVo> fieldList = new ArrayList<>();
        if (collectionVo != null) {
            if (CollectionUtils.isNotEmpty(collectionVo.getField())) {
                for (int i = 0; i < collectionVo.getField().size(); i++) {
                    JSONObject fieldObj = collectionVo.getField().getJSONObject(i);
                    SyncFieldVo syncFieldVo = JSONObject.toJavaObject(fieldObj, SyncFieldVo.class);
                    //有条件表达式的字段才能作为搜索条件
                    if (CollectionUtils.isNotEmpty(syncFieldVo.getExpressionList())) {
                        fieldList.add(syncFieldVo);
                    }
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
