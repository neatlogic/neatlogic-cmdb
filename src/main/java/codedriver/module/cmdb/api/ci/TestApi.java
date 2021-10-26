/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.tagent.dto.TagentVo;
import codedriver.framework.tagent.register.core.AfterRegisterJobManager;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import com.alibaba.fastjson.JSONObject;

import javax.annotation.Resource;

//@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class TestApi extends PrivateApiComponentBase {
    @Resource
    private CiMapper ciMapper;

    @Override
    public String getName() {
        return "测试Tagent注册";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Description(desc = "测试Tagent注册")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        TagentVo tagentVo = new TagentVo();
        tagentVo.setIp("99.99.99.99");
        tagentVo.setOsType("linux");
        tagentVo.setOsVersion("13.x");
        tagentVo.setOsbit("64");
        tagentVo.setName("tagent222");
        tagentVo.setAccountId(10L);
        AfterRegisterJobManager.executeAll(tagentVo);
        return null;
    }

    @Override
    public String getToken() {
        return "/tagenttest";
    }
}
