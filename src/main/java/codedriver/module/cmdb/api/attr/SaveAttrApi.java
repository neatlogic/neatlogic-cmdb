/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.attr;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.prop.PropMapper;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.exception.attr.AttrNameRepeatException;
import codedriver.module.cmdb.service.attr.AttrService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveAttrApi extends PrivateApiComponentBase {

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private PropMapper propMapper;

    @Resource
    private AttrService attrService;

    @Override
    public String getToken() {
        return "/cmdb/attr/save";
    }

    @Override
    public String getName() {
        return "保存模型属性";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id，不提供代表添加"),
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "type", type = ApiParamType.STRING, isRequired = true, desc = "属性类型"),
            @Param(name = "targetCiId", type = ApiParamType.LONG, desc = "目标模型id"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, desc = "属性配置"),
            //name不能大于25个字符，因为mysql表名最长64字符，需要给模型名留下位置
            @Param(name = "name", type = ApiParamType.STRING, xss = true, isRequired = true, maxLength = 25, desc = "英文名称"),
            @Param(name = "label", type = ApiParamType.STRING, desc = "中文名称", xss = true, maxLength = 100,
                    isRequired = true),
            @Param(name = "description", type = ApiParamType.STRING, desc = "备注", maxLength = 500, xss = true),
            @Param(name = "validator", type = ApiParamType.STRING, desc = "校验组件"),
            @Param(name = "validConfig", type = ApiParamType.JSONOBJECT, desc = "校验设置"),
            @Param(name = "isRequired", type = ApiParamType.INTEGER, desc = "是否必填"),
            @Param(name = "isUnique", type = ApiParamType.INTEGER, desc = "是否唯一"),
            @Param(name = "inputType", type = ApiParamType.ENUM, rule = "at,mt", desc = "输入类型，人工录入|自动发现"),
            @Param(name = "groupName", type = ApiParamType.STRING, desc = "分组")})
    @Output({@Param(name = "id", type = ApiParamType.LONG, desc = "属性id"),})
    @Description(desc = "保存模型属性接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AttrVo attrVo = JSONObject.toJavaObject(jsonObj, AttrVo.class);
        Long attrId = jsonObj.getLong("id");
        //校验name是否重复
        if (attrMapper.checkAttrNameIsRepeat(attrVo) > 0) {
            throw new AttrNameRepeatException(attrVo.getName());
        }

        if (attrId == null) {
            attrService.insertAttr(attrVo);
        } else {
            attrService.updateAttr(attrVo);
        }
        return attrVo.getId();
    }

}
