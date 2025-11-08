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

package neatlogic.module.cmdb.api.resourcecenter.resource;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountAccessTestVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceAccountAccessTestException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceAccountAccessTestHostException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.util.IpUtil;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.dto.runner.GroupNetworkVo;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.exception.runner.RunnerGroupRunnerNotFoundException;
import neatlogic.framework.exception.runner.RunnerNotMatchException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ResourceAccountAccessTestApi extends PrivateApiComponentBase {
    private static final Random random = new Random();

    @Resource
    private ResourceMapper resourceMapper;

    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Resource
    RunnerMapper runnerMapper;

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Override
    public String getToken() {
        return "resourcecenter/account/accesstest";
    }

    @Override
    public String getName() {
        return "nmcarr.resourceaccountaccesstestapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.resourceid"),
            @Param(name = "runnerId", type = ApiParamType.LONG, desc = "term.deploy.runnerid"),
            @Param(name = "accountIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "term.cmdb.accountidlist"),
    })
    @Output({
            @Param(explode = AccountVo.class),
    })
    @Description(desc = "nmcarr.resourceaccountaccesstestapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long resourceId = paramObj.getLong("resourceId");
        Long runnerId = paramObj.getLong("runnerId");
        List<Long> accountIdList = paramObj.getJSONArray("accountIdList").toJavaList(Long.class);
        ResourceVo resource = resourceCenterResourceService.getResourceById(resourceId);
        if (resource == null) {
            throw new ResourceNotFoundException(resourceId);
        }
        RunnerMapVo runnerMapVo = null;
        if (runnerId == null) {
            //如果没有指定runner则根据网段查找runner
            List<RunnerMapVo> runnerMapList = null;
            List<GroupNetworkVo> networkVoList = runnerMapper.getAllNetworkMask(null);
            for (GroupNetworkVo networkVo : networkVoList) {
                //如果不是ip，则尝试域名解析
                if (StringUtils.isNotBlank(resource.getIp()) && !RegexUtils.isMatch(resource.getIp(), RegexUtils.IP)) {
                    try {
                        InetAddress addresses = InetAddress.getByName(resource.getIp());
                        resource.setIp(addresses.getHostAddress());
                    } catch (Exception ex) {
                        throw new ResourceAccountAccessTestHostException(resource.getIp());
                    }
                }
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
            int runnerMapIndex = random.nextInt(runnerMapList.size());
            runnerMapVo = runnerMapList.get(runnerMapIndex);
        }else{
            runnerMapVo = runnerMapper.getRunnerMapByRunnerMapId(runnerId);
        }

        List<AccountAccessTestVo> accessTestVoList = new ArrayList<>();
        List<AccountVo> accountList = resourceAccountMapper.getAccountListByIdList(accountIdList);
        if (!accountList.isEmpty()) {
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

        if (request.getResponseCode() != 200) {
            String errMsg = String.format("test account failed, ResponseCode:%d, ErrorMsg: %s, Exception: %s, Result: %s", request.getResponseCode(), request.getErrorMsg(), request.getError(), request.getResult());
            throw new ApiRuntimeException(errMsg);
        }
        JSONObject resultJson = request.getResultJson();
        String error = request.getError();
        if (StringUtils.isNotBlank(error)) {
            throw new ResourceAccountAccessTestException(error);
        }
        JSONObject result = new JSONObject();
        result.put("result", resultJson.getJSONArray("Return"));
        result.put("runner", runnerMapVo.getName() + "(" + runnerMapVo.getId() + ")");
        return result;
    }
}
