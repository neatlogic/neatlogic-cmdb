/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.customview;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.customview.CustomViewMapper;
import codedriver.module.cmdb.service.customview.CustomViewService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveCiCustomViewApi extends PrivateApiComponentBase {

    @Resource
    private CustomViewMapper customViewMapper;
    @Autowired
    private CustomViewService customViewService;


    @Override
    public String getToken() {
        return "/cmdb/ci/customview/save";
    }

    @Override
    public String getName() {
        return "保存配置项自定义视图";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "customViewId", type = ApiParamType.LONG, isRequired = true, desc = "视图id"),
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "direction", type = ApiParamType.ENUM, member = RelDirectionType.class, isRequired = true, desc = "方向"),
            @Param(name = "pathHash", type = ApiParamType.STRING, isRequired = true, desc = "关系路径唯一标识")
    })
    @Description(desc = "保存配置项自定义视图接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {

        return null;
    }

}
