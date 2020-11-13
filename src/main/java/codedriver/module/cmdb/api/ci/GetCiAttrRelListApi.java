package codedriver.module.cmdb.api.ci;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.dto.ci.CiVo;
import codedriver.module.cmdb.dto.ci.RelVo;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional // 需要启用事务，以便查询权限时激活一级缓存
public class GetCiAttrRelListApi extends PrivateApiComponentBase {

    @Autowired
    private CiMapper ciMapper;

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/attrrellist";
    }

    @Override
    public String getName() {
        return "获取模型信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "模型id列表")})
    @Output({@Param(explode = CiVo.class)})
    @Description(desc = "获取模型信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray ciIds = jsonObj.getJSONArray("ciIdList");
        List<Long> ciIdList = new ArrayList<>();
        for (int i = 0; i < ciIds.size(); i++) {
            ciIdList.add(ciIds.getLong(i));
        }
        List<CiVo> ciList = ciMapper.getCiByIdList(ciIdList);
        if (CollectionUtils.isNotEmpty(ciList)) {
            List<AttrVo> attrList =
                attrMapper.getAttrByCiIdList(ciList.stream().map(ci -> ci.getId()).collect(Collectors.toList()));
            List<RelVo> relList =
                relMapper.getRelByCiIdList(ciList.stream().map(ci -> ci.getId()).collect(Collectors.toList()));
        }
        return null;
    }
}
