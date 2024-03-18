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
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.crossover.IAutoexecJobActionCrossoverService;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
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
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional
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
        List<JSONObject> list = mongoTemplate.find(new Query(Criteria.where("id").is(confId)), JSONObject.class, "_discovery_conf");
        if (CollectionUtils.isEmpty(list)) {
            throw new DiscoverConfNotFoundException(confId);
        }
        JSONObject config = list.get(0);
        JSONObject param = new JSONObject();
        param.put("nets", config.getString("nets"));
        param.put("ports", config.getString("ports"));
        param.put("snmpport", config.getString("snmpport"));
        param.put("communities", config.getString("communities"));
        param.put("workercount", config.getString("workercount"));
        param.put("timingtmpl", config.getString("timingtmpl"));
        AutoexecJobVo autoexecJobVo = new AutoexecJobVo();
        autoexecJobVo.setInvokeId(confId);
        autoexecJobVo.setRouteId(confId.toString());
        autoexecJobVo.setSource(JobSource.DISCOVERY.getValue());
        autoexecJobVo.setOperationId(discoverConfCombopVo.getCombopId());
        autoexecJobVo.setOperationType(CombopOperationType.COMBOP.getValue());
        autoexecJobVo.setName(config.getString("name"));
        autoexecJobVo.setRoundCount(64);
        autoexecJobVo.setParam(param);
        IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
        autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(autoexecJobVo);
        IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
        autoexecJobVo.setAction(JobAction.FIRE.getValue());
        autoexecJobVo.setIsFirstFire(1);
        fireAction.doService(autoexecJobVo);
        JSONObject resultObj = new JSONObject();
        resultObj.put("jobId", autoexecJobVo.getId());
        return resultObj;
    }

    @Override
    public String getToken() {
        return "/cmdb/discovery/job/create";
    }
}
