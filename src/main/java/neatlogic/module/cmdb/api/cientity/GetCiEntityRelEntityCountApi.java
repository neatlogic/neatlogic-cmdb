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

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional
public class GetCiEntityRelEntityCountApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private RelMapper relMapper;

    @Resource
    private RelEntityMapper relEntityMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/relentity/get";
    }

    @Override
    public String getName() {
        return "获取配置项关系概要信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.cientityid"),
    })
    @Output({@Param(explode = RelVo[].class)})
    @Description(desc = "获取配置项关系概要信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciEntityId = jsonObj.getLong("ciEntityId");
        CiEntityVo ciEntityVo = ciEntityService.getCiEntityBaseInfoById(ciEntityId);
        if (ciEntityVo == null) {
            throw new CiEntityNotFoundException(ciEntityId);
        }
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciEntityVo.getCiId()));
        for (RelVo relVo : relList) {
            //from和to是需要颠倒的
            if (relVo.getDirection().equalsIgnoreCase(RelDirectionType.FROM.getValue())) {
                relVo.setToRelEntityCount(relEntityMapper.getRelEntityCountByFromCiEntityIdAndRelId(ciEntityId, relVo.getId()));
            } else {
                relVo.setFromRelEntityCount(relEntityMapper.getRelEntityCountByToCiEntityIdAndRelId(ciEntityId, relVo.getId()));
            }
        }
        return relList;
    }


}
