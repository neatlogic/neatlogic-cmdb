/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.ci;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.service.ci.CiService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional // 需要启用事务，以便查询权限时激活一级缓存
public class GetImportFieldListApi extends PrivateApiComponentBase {

    @Autowired
    private CiService ciService;

    @Autowired
    private CiMapper ciMapper;


    @Override
    public String getToken() {
        return "/cmdb/ci/import/listfield";
    }

    @Override
    public String getName() {
        return "获取可导入的属性和关系列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "模型id")})
    @Output({@Param(explode = CiVo.class)})
    @Description(desc = "获取可导入的属性和关系列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("id");
        CiVo ciVo = ciService.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }
        if (CollectionUtils.isNotEmpty(ciVo.getAttrList())) {
            ciVo.getAttrList().removeIf(attrVo -> !attrVo.getCanImport());
        }
        //唯一表达式的属性也需要设为必填
        if (CollectionUtils.isNotEmpty(ciVo.getUniqueAttrIdList())) {
            for (Long attrId : ciVo.getUniqueAttrIdList()) {
                ciVo.getAttrById(attrId).setIsRequired(1);
            }
        }
        return ciVo;
    }
}
