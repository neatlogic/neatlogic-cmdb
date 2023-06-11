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

package neatlogic.module.cmdb.api.cientity;

import neatlogic.framework.auth.core.AuthAction;
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
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.legalvalid.IllegalCiEntityMapper;
import com.alibaba.fastjson.JSONObject;
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
