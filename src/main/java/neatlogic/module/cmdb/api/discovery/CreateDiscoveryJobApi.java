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

package neatlogic.module.cmdb.api.discovery;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.crossover.IAutoexecJobActionCrossoverService;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.framework.cmdb.dto.discovery.DiscoverConfCombopVo;
import neatlogic.framework.cmdb.exception.discover.DiscoverConfNotFoundException;
import neatlogic.framework.cmdb.exception.discover.DiscoverNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.constvalue.JobSource;
import neatlogic.module.cmdb.dao.mapper.discovery.DiscoveryMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class CreateDiscoveryJobApi extends PrivateApiComponentBase {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private DiscoveryMapper discoveryMapper;

    @Override
    public String getName() {
        return "创建自动发现作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "confId", type = ApiParamType.LONG, isRequired = true, desc = "自动发现配置id")
    })
    @Output({
    })
    @Description(desc = "创建自动发现作业")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long confId = paramObj.getLong("confId");
        DiscoverConfCombopVo discoverConfCombopVo = discoveryMapper.getDiscoveryConfCombopByConfId(confId);
        if (discoverConfCombopVo == null) {
            throw new DiscoverNotFoundException(confId);
        }
        new Query();
        List<JSONObject> list = mongoTemplate.find(new Query(Criteria.where("id").is(confId)), JSONObject.class, "_discovery_conf");
        if (CollectionUtils.isEmpty(list)) {
            throw new DiscoverConfNotFoundException(confId);
        }
        JSONObject config = list.get(0);
//        const { nets, ports, snmpport, communities, workercount, timingtmpl } = row;
//        const param = {
//            roundCount: 64,
//            combopId: row.combopId,
//            source: 'discovery',
//            param: { nets, ports, snmpport, communities, workercount, timingtmpl },
//              name: row.name,
//            invokeId: row.id
//        };
        JSONObject param = new JSONObject();
        param.put("nets", config.getString("nets"));
        param.put("ports", config.getString("ports"));
        param.put("snmpport", config.getString("snmpport"));
        param.put("communities", config.getString("communities"));
        param.put("workercount", config.getString("workercount"));
        param.put("timingtmpl", config.getString("timingtmpl"));
        AutoexecJobVo autoexecJobVo = new AutoexecJobVo();
        autoexecJobVo.setInvokeId(confId);
        autoexecJobVo.setSource(JobSource.DISCOVERY.getValue());
        autoexecJobVo.setOperationId(discoverConfCombopVo.getCombopId());
        autoexecJobVo.setOperationType(CombopOperationType.COMBOP.getValue());
        autoexecJobVo.setName(config.getString("name"));
        autoexecJobVo.setRoundCount(64);
        autoexecJobVo.setParam(param);
        IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
        autoexecJobActionCrossoverService.validateCreateJob(autoexecJobVo);
        return autoexecJobVo.getId();
    }

    @Override
    public String getToken() {
        return "/cmdb/discovery/job/create";
    }
}
