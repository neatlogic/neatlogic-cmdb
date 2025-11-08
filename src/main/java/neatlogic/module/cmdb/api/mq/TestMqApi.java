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

package neatlogic.module.cmdb.api.mq;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.mq.core.ITopic;
import neatlogic.framework.mq.core.TopicFactory;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.service.customview.CustomViewService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class TestMqApi extends PrivateApiComponentBase {

    @Resource
    private CustomViewService customViewService;

    @Override
    public String getName() {
        return "测试MQ";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "视图id", isRequired = true)})
    @Output({@Param(explode = CustomViewVo.class)})
    @Description(desc = "测试MQ接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        ITopic<CiEntityTransactionVo> topic = TopicFactory.getTopic("cmdb/cientity/insert");
        topic.send(new CiEntityTransactionVo());
        return null;
    }

    @Override
    public String getToken() {
        return "/cmdb/mq/test";
    }
}
