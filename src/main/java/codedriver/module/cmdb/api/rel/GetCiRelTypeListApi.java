package codedriver.module.cmdb.api.rel;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.RelTypeMapper;
import codedriver.module.cmdb.dto.ci.RelTypeVo;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiRelTypeListApi extends PrivateApiComponentBase {

    @Autowired
    private RelTypeMapper relTypeMapper;

    @Override
    public String getToken() {
        return "/cmdb/reltype/list";
    }

    @Override
    public String getName() {
        return "获取模型关系类型列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({@Param(explode = ValueTextVo[].class)})
    @Description(desc = "获取模型关系类型列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<RelTypeVo> relTypeList = relTypeMapper.getAllRelType();
        List<ValueTextVo> returnList = new ArrayList<>();
        for (RelTypeVo relTypeVo : relTypeList) {
            returnList.add(new ValueTextVo(relTypeVo.getId().toString(), relTypeVo.getName()));
        }
        return returnList;
    }
}
