/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.mongodb;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.sync.SyncCiCollectionVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.sync.SyncMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class TestMongodbApi extends PrivateApiComponentBase {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource
    private SyncMapper syncMapper;

    @Override
    public String getToken() {
        return "/cmdb/mongodb/test";
    }

    @Override
    public String getName() {
        return "mongo测试";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "json", type = ApiParamType.JSONARRAY)
    })
    @Output({@Param(explode = AttrVo.class)})
    @Description(desc = "mongo测试接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Query query = new Query();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
        query.addCriteria(Criteria.where("_id").is("618e3ebba4891d05a813e014"));
        JSONObject json = mongoTemplate.findOne(new Query(), JSONObject.class, "COLLECT_OS");
        SyncCiCollectionVo collectionVo = syncMapper.getSyncCiCollectionById(482988709961728L);
        JSONObject returnObj = new JSONObject();
        returnObj.put("db", sdf.format(collectionVo.getFcd()));
        returnObj.put("mo", sdf.format(json.getDate("_updatetime")));
        return returnObj;
    }
}
