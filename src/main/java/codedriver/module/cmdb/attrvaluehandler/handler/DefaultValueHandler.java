/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.attrvaluehandler.handler;

import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import com.alibaba.fastjson.JSONArray;
import org.springframework.stereotype.Service;


@Service
public class DefaultValueHandler implements IAttrValueHandler {


    @Override
    public String getType() {
        return "default";
    }

    @Override
    public JSONArray getActualValueList(AttrVo attrVo, JSONArray valueList) {
        JSONArray returnValueList = new JSONArray();
        returnValueList.addAll(valueList);
        return returnValueList;
    }

}
