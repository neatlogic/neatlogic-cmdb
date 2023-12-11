package neatlogic.module.cmdb.api.globalattr;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrItemVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetGlobalAttrItemListApi extends PrivateApiComponentBase {

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Override
    public String getName() {
        return "获取全局属性选项列表";
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, desc = "common.name", isRequired = true)
    })
    @Output({
            @Param(explode = GlobalAttrItemVo[].class)
    })
    @Description(desc = "获取全局属性选项列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String name = paramObj.getString("name");
        GlobalAttrVo globalAttrVo = globalAttrMapper.getGlobalAttrByName(name);
        if (globalAttrVo == null) {
//            throw new GlobalAttrNotFoundException(name);
        }
        return TableResultUtil.getResult(globalAttrVo.getItemList());
    }

    @Override
    public String getToken() {
        return "/cmdb/globalattr/itemlist/get";
    }
}
