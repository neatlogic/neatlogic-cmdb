/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.cmdb.api.resourcecenter.resource;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountAccessTestVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceAccountAccessTestException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.util.IpUtil;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.dto.runner.GroupNetworkVo;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerGroupRunnerNotFoundException;
import neatlogic.framework.exception.runner.RunnerNotMatchException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ResourceAccountAccessTestApi extends PrivateApiComponentBase {

    @Resource
    private ResourceMapper resourceMapper;

    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Resource
    RunnerMapper runnerMapper;

    @Override
    public String getToken() {
        return "resourcecenter/account/accesstest";
    }

    @Override
    public String getName() {
        return "测试帐号可用性";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资源id"),
            @Param(name = "accountIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "帐号ID列表"),
    })
    @Output({
            @Param(explode = AccountVo.class),
    })
    @Description(desc = "测试帐号可用性")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long resourceId = paramObj.getLong("resourceId");
        List<Long> accountIdList = paramObj.getJSONArray("accountIdList").toJavaList(Long.class);
        ResourceVo resource = resourceMapper.getResourceById(resourceId);
        if (resource == null) {
            throw new ResourceNotFoundException(resourceId);
        }
        // 根据网段查找runner
        List<RunnerMapVo> runnerMapList = null;
        List<GroupNetworkVo> networkVoList = runnerMapper.getAllNetworkMask();
        for (GroupNetworkVo networkVo : networkVoList) {
            if (IpUtil.isBelongSegment(resource.getIp(), networkVo.getNetworkIp(), networkVo.getMask())) {
                RunnerGroupVo groupVo = runnerMapper.getRunnerMapGroupById(networkVo.getGroupId());
                if (CollectionUtils.isEmpty(groupVo.getRunnerMapList())) {
                    throw new RunnerGroupRunnerNotFoundException(groupVo.getName() + "(" + networkVo.getGroupId() + ") ");
                }
                runnerMapList = groupVo.getRunnerMapList();
            }
        }
        if (CollectionUtils.isEmpty(runnerMapList)) {
            throw new RunnerNotMatchException();
        }
        // 随机分配runner
        int runnerMapIndex = (int) (Math.random() * runnerMapList.size());
        RunnerMapVo runnerMapVo = runnerMapList.get(runnerMapIndex);
        List<AccountAccessTestVo> accessTestVoList = new ArrayList<>();
        List<AccountVo> accountList = resourceAccountMapper.getAccountListByIdList(accountIdList);
        if (accountList.size() > 0) {
            accountList.forEach(vo -> accessTestVoList.add(new AccountAccessTestVo(resource.getIp()
                    , resource.getPort()
                    , vo.getProtocolPort()
                    , vo.getProtocol()
                    , resource.getName()
                    , resource.getTypeName()
                    , vo.getAccount()
                    , vo.getPasswordCipher()
                    , vo.getName()))
            );
        }
        String url = runnerMapVo.getUrl() + "api/rest/account/accesstest";
        JSONObject paramJson = new JSONObject();
        paramJson.put("accountList", accessTestVoList);
        HttpRequestUtil request = HttpRequestUtil.post(url).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        JSONObject resultJson = request.getResultJson();
        String error = request.getError();
        if (StringUtils.isNotBlank(error)) {
            throw new ResourceAccountAccessTestException(error);
        }
        return resultJson.getJSONArray("Return");
    }

}
