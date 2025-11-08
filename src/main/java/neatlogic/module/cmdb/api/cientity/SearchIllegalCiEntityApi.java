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
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.legalvalid.IllegalCiEntityVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.legalvalid.IllegalCiEntityMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchIllegalCiEntityApi extends PrivateApiComponentBase {

    @Resource
    private IllegalCiEntityMapper illegalCiEntityMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/illegalcientity/search";
    }

    @Override
    public String getName() {
        return "nmcac.searchillegalcientityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "legalValidId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ruleid"),
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "common.keyword")})
    @Output({@Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = CiEntityVo[].class),
            @Param(name = "theadList", type = ApiParamType.JSONARRAY, desc = "nmcac.searchattrtargetcientityapi.output.param.desc")})
    @Description(desc = "nmcac.searchillegalcientityapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        IllegalCiEntityVo illegalCiEntityVo = JSONObject.toJavaObject(jsonObj, IllegalCiEntityVo.class);
        CiVo ciVo = ciMapper.getCiById(illegalCiEntityVo.getCiId());
        if (ciVo == null) {
            throw new CiNotFoundException(illegalCiEntityVo.getCiId());
        }
        List<CiVo> downwardCiList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        illegalCiEntityVo.setCiIdList(downwardCiList.stream().map(CiVo::getId).collect(Collectors.toList()));
        List<IllegalCiEntityVo> illegalCiEntityList = illegalCiEntityMapper.searchIllegalCiEntity(illegalCiEntityVo);
        if (CollectionUtils.isNotEmpty(illegalCiEntityList)) {
            illegalCiEntityVo.setRowNum(illegalCiEntityMapper.searchIllegalCiEntityCount(illegalCiEntityVo));
        }
        return TableResultUtil.getResult(illegalCiEntityList, illegalCiEntityVo);
    }

}
