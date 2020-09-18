package codedriver.module.cmdb.api.cientity;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.cientity.AttrEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import codedriver.module.cmdb.dto.cientity.AttrEntityVo;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.dto.cientity.RelEntityVo;

@Service
public class GetCiEntityApi extends PrivateApiComponentBase {


    @Autowired
    private CiEntityMapper ciEntityMapper;

    @Autowired
    private AttrEntityMapper attrEntityMapper;

    @Autowired
    private RelEntityMapper relEntityMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/get";
    }

    @Override
    public String getName() {
        return "获取配置项详细信息";
    }

    @Override
    public String getConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "配置项id")})
    @Output({@Param(explode = CiEntityVo.class)})
    @Description(desc = "获取配置项详细信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        // FIXME 补充权限校验
        Long id = jsonObj.getLong("id");
        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityById(id);
        if (ciEntityVo != null) {
            List<AttrEntityVo> attrEntityList = attrEntityMapper.getAttrEntityByCiEntityId(id);
            ciEntityVo.setAttrEntityList(attrEntityList);
            List<RelEntityVo> relEntityList = relEntityMapper.getRelEntityByCiEntityId(id);
            ciEntityVo.setRelEntityList(relEntityList);
        }
        return ciEntityVo;
    }

}
