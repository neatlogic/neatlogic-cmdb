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

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class RefreshCiEntityNameApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiEntityService ciEntityService;

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, desc = "模型id，不提供代表刷新所有")})
    @Description(desc = "刷新配置项名称，调用成功后刷新作业会在后台进行")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long ciId = paramObj.getLong("ciId");
        List<CiVo> ciList = new ArrayList<>();
        if (ciId != null) {
            CiVo ciVo = ciMapper.getCiById(ciId);
            if (ciVo != null) {
                ciList.add(ciVo);
            }
        } else {
            ciList = ciMapper.getAllCi(null);
        }
        if (CollectionUtils.isNotEmpty(ciList)) {
            List<CiVo> finalCiList = ciList;
            CachedThreadPool.execute(new NeatLogicThread("CIENTITY_NAME_UPDATE") {
                @Override
                protected void execute() {
                    for (CiVo ciVo : finalCiList) {
                        ciEntityService.updateCiEntityNameForCi(ciVo);
                    }
                }
            });
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/cmdb/cientity/name/update";
    }

    @Override
    public String getName() {
        return "刷新配置项名称";
    }
}
