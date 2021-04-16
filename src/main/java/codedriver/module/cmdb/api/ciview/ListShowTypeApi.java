/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.ciview;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.cmdb.enums.ShowType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;

@Deprecated
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListShowTypeApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "/cmdb/ciview/showtype/list";
    }

    @Override
    public String getName() {
        return "获取显示类型列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({@Param(explode = ValueTextVo[].class)})
    @Description(desc = "获取显示类型列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<ValueTextVo> dataList = new ArrayList<>();
        for (ShowType st : ShowType.values()) {
            ValueTextVo valueTextVo = new ValueTextVo();
            valueTextVo.setText(st.getText());
            valueTextVo.setValue(st.getValue());
            dataList.add(valueTextVo);
        }
        return dataList;
    }

}
