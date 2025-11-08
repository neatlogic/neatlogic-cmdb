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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional
public class GetCiEntityBaseInfoListApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityMapper ciEntityMapper;


    @Override
    public String getToken() {
        return "/cmdb/cientity/baseinfo/list";
    }

    @Override
    public String getName() {
        return "nmcac.getcientitybaseinfolistapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "idList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "nmcac.getcientitybaseinfolistapi.input.param.desc.idlist")})
    @Output({@Param(name = "Return", explode = CiEntityVo[].class)})
    @Description(desc = "nmcac.getcientitybaseinfolistapi.description.desc")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray ids = jsonObj.getJSONArray("idList");
        List<Long> idList = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            idList.add(ids.getLong(i));
        }
        if (CollectionUtils.isNotEmpty(idList)) {
            return ciEntityMapper.getCiEntityBaseInfoByIdList(idList);
        }
        return null;
    }

}
