package codedriver.module.cmdb.api.ci;

import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/6/23 10:38 上午
 */
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListNotAbstractCiApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getName() {
        return "查询非抽象模型列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/cmdb/not/abstract/ci/list";
    }

    @Input({
            @Param(name = "ciName", type = ApiParamType.STRING, desc = "模型名称")
    })
    @Output({
            @Param(explode = CiVo[].class, desc = "非抽象模型列表")
    })
    @Description(desc = "查询非抽象模型列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        CiVo ciVo = ciMapper.getCiByName(paramObj.getString("ciName"));
        if (ciVo == null) {
            throw new CiNotFoundException(paramObj.getString("ciName"));
        }
        return ciMapper.getDownwardNotAbstractCiListByByLR(ciVo.getLft(),ciVo.getRht());
    }
}
