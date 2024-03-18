/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.api.discovery;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.framework.cmdb.dto.discovery.DiscoverConfCombopVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.SnowflakeUtil;
import neatlogic.module.cmdb.dao.mapper.discovery.DiscoveryMapper;
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
