/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.cmdb.api.ci;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.cmdb.exception.rel.CiNotMatchException;
import neatlogic.framework.cmdb.exception.rel.RelIrregularException;
import neatlogic.framework.cmdb.exception.rel.RelNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class ValidateRelApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;


    @Resource
    private RelMapper relMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Override
    public String getToken() {
        return "/cmdb/rel/validate";
    }

    @Override
    public String getName() {
        return "校验配置项关系是否允许创建";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "relId", type = ApiParamType.LONG, isRequired = true, desc = "关系id"),
            @Param(name = "relDirection", rule = "from,to", type = ApiParamType.STRING, isRequired = true, desc = "关系方向"),
            @Param(name = "fromCiEntityId", type = ApiParamType.LONG, isRequired = true, desc = "来源配置项id"),
            @Param(name = "fromCiId", type = ApiParamType.LONG, isRequired = true, desc = "来源模型id"),
            @Param(name = "toCiId", type = ApiParamType.LONG, isRequired = true, desc = "目标模型id"),
    })
    @Output({@Param(name = "isValidate", type = ApiParamType.BOOLEAN, desc = "是否允许创建")})
    @Description(desc = "校验配置项关系是否允许创建")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long relId = jsonObj.getLong("relId");
        Long fromCiEntityId = jsonObj.getLong("fromCiEntityId");
        String relDirection = jsonObj.getString("relDirection");
        Long fromCiId = jsonObj.getLong("fromCiId");
        Long toCiId = jsonObj.getLong("toCiId");
        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(fromCiEntityId);
        if (ciEntityVo == null) {
            throw new CiEntityNotFoundException(fromCiEntityId);
        }
        if (!ciEntityVo.getCiId().equals(fromCiId)) {
            throw new CiNotMatchException();
        }
        RelVo relVo = relMapper.getRelById(relId);

        if (relVo == null) {
            throw new RelNotFoundException(relId);
        }
        //根据rel的方向调整配置项id的上下游顺序
        if (relDirection.equals(RelDirectionType.TO.getValue())) {
            Long tmpFromCiId = fromCiId;
            fromCiId = toCiId;
            toCiId = tmpFromCiId;
        }

        Long relFromCiId = relVo.getFromCiId();
        Long relToCiId = relVo.getToCiId();
        CiVo fromCiVo = ciMapper.getCiBaseInfoById(relFromCiId);
        CiVo toCiVo = ciMapper.getCiBaseInfoById(relToCiId);
        if (fromCiVo == null) {
            throw new CiNotFoundException(fromCiId);
        }
        if (toCiVo == null) {
            throw new CiNotFoundException(toCiId);
        }
        List<Long> fromCiIdList = ciMapper.getDownwardCiIdListByLR(fromCiVo.getLft(), fromCiVo.getRht());
        List<Long> toCiIdList = ciMapper.getDownwardCiIdListByLR(toCiVo.getLft(), toCiVo.getRht());
        if (fromCiIdList.contains(fromCiId) && toCiIdList.contains(toCiId)) {
            return null;
        } else {
            throw new RelIrregularException(fromCiVo, toCiVo);
        }
    }
}
