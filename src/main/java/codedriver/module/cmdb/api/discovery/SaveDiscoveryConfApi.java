/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.discovery;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.discovery.DiscoverConfCombopVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.SnowflakeUtil;
import codedriver.module.cmdb.auth.label.SYNC_MODIFY;
import codedriver.module.cmdb.dao.mapper.discovery.DiscoveryMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDiscoveryConfApi extends PrivateApiComponentBase {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private DiscoveryMapper discoveryMapper;


    @Override
    public String getToken() {
        return "/cmdb/discovery/conf/save";
    }

    @Override
    public String getName() {
        return "保存自动发现设置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "_id", type = ApiParamType.STRING, desc = "id，存在代表修改，不存在代表新增"),
            @Param(name = "id", type = ApiParamType.LONG, desc = "记录引用关系时的id，如果不提供则代表需要新创建"),
            @Param(name = "name", type = ApiParamType.STRING, xss = true, isRequired = true, desc = "名称"),
            @Param(name = "nets", type = ApiParamType.STRING, isRequired = true, desc = "网段"),
            @Param(name = "ports", type = ApiParamType.STRING, isRequired = true, desc = "端口"),
            @Param(name = "snmpport", type = ApiParamType.STRING, isRequired = true, desc = "SNMP端口"),
            @Param(name = "communities", type = ApiParamType.STRING, desc = "团体字"),
            @Param(name = "combopId", type = ApiParamType.LONG, desc = "组合工具id"),
            @Param(name = "timingtmpl", type = ApiParamType.INTEGER, isRequired = true, desc = "速度级别,1最慢，5最快"),
            @Param(name = "workercount", type = ApiParamType.INTEGER, isRequired = true, desc = "工作线程")
    })
    @Description(desc = "保存自动发现设置接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String[] fieldList = new String[]{"_id", "id", "name", "nets", "ports", "snmpport", "timingtmpl", "communities", "workercount"};
        Long combopId = jsonObj.getLong("combopId");
        String _id = jsonObj.getString("_id");
        if (!jsonObj.containsKey("id")) {
            jsonObj.put("id", SnowflakeUtil.uniqueLong());
        }
        //由于前端可能会送一些没用的字段进来，而mongodb是直接保存json，所以需要先删除没用的字段
        jsonObj.keySet().removeIf(key -> Arrays.stream(fieldList).noneMatch(d -> d.equalsIgnoreCase(key)));
        if (StringUtils.isNotBlank(jsonObj.getString("_id"))) {
            jsonObj.put("_id", new ObjectId(jsonObj.getString("_id")));
        }
        mongoTemplate.save(jsonObj, "_discovery_conf");
        discoveryMapper.deleteDiscoveryConfCombopByConfId(jsonObj.getLong("id"));
        if (combopId != null) {
            discoveryMapper.insertDiscoveryConfCombop(new DiscoverConfCombopVo(jsonObj.getLong("id"), combopId));
        }
        return null;
    }
}
