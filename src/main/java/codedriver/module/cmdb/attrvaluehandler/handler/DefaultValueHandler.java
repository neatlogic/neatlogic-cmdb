/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.attrvaluehandler.handler;

import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


@Service
public class DefaultValueHandler implements IAttrValueHandler {

    @Resource
    private CiEntityService ciEntityService;

    @Override
    public String getType() {
        return "default";
    }

    @Override
    public String getActualValue(JSONObject config, String value) {
        return value;
    }
}
