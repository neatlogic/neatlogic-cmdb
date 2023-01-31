/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.ciview;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.enums.ShowType;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
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
        List<ValueTextVo> returnList = new ArrayList<>();
        for (ShowType showType : ShowType.values()) {
            ValueTextVo valueTextVo = new ValueTextVo();
            valueTextVo.setValue(showType.getValue());
            valueTextVo.setText(showType.getText());
            returnList.add(valueTextVo);
        }
        return returnList;
    }
}
