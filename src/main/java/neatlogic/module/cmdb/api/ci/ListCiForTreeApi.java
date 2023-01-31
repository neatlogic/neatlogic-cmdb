/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.ci;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCiForTreeApi extends PrivateApiComponentBase {
    @Autowired
    private CiMapper ciMapper;

    @Override
    public String getName() {
        return "获取模型树型列表";
    }

    @Override
    public String getToken() {
        return "cmdb/ci/listtree";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, desc = "模型id")})
    @Output({@Param(explode = ValueTextVo[].class)})
    @Description(desc = "返回模型树型列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        List<CiVo> ciList = ciMapper.getAllCi(null);
        Map<Long, CiVo> ciMap = new HashMap<>();
        for (CiVo ciVo : ciList) {
            ciMap.put(ciVo.getId(), ciVo);
        }
        //将模型挂到父模型上
        for (CiVo ciVo : ciList) {
            if (ciVo.getParentCiId() != null) {
                CiVo parentCiVo = ciMap.get(ciVo.getParentCiId());
                if (parentCiVo != null) {
                    parentCiVo.addChild(ciVo);
                }
            }
        }
        //清除所有非父节点模型
        ciList.removeIf(ciVo -> ciVo.getParentCiId() != null || (ciId != null && (ciVo.getId().equals(ciId) || checkCiIdIsParent(ciId, ciVo))));
        return ciList;
    }

    private boolean checkCiIdIsParent(Long ciId, CiVo ciVo) {
        while (ciVo.getParentCi() != null) {
            if (ciVo.getParentCi().getId().equals(ciId)) {
                return true;
            }
            ciVo = ciVo.getParentCi();
        }
        return false;
    }


}
