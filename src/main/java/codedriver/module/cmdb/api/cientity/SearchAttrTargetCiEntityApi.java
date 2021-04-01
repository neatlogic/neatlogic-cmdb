/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;
import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.dto.ci.CiVo;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.exception.attr.AttrNotFoundException;
import codedriver.module.cmdb.exception.attr.AttrTargetCiIdNotFoundException;
import codedriver.module.cmdb.exception.ci.CiNotFoundException;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAttrTargetCiEntityApi extends PrivateApiComponentBase {

    @Autowired
    private CiEntityService ciEntityService;

    @Autowired
    private CiViewMapper ciViewMapper;

    @Autowired
    private TeamMapper teamMapper;

    @Resource
    private CiMapper ciMapper;

    @Resource
    private AttrMapper attrMapper;

    @Override
    public String getToken() {
        return "/cmdb/attr/targetci/search";
    }

    @Override
    public String getName() {
        return "查询属性目标配置项";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "attrId", type = ApiParamType.LONG, isRequired = true, desc = "属性id"),
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "关键字")})
    @Output({@Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = CiEntityVo[].class),
            @Param(name = "theadList", type = ApiParamType.JSONARRAY, desc = "表头信息")})
    @Description(desc = "查询配置项接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long attrId = jsonObj.getLong("attrId");
        AttrVo attrVo = attrMapper.getAttrById(attrId);
        if (attrVo == null) {
            throw new AttrNotFoundException(attrId);
        }

        if (attrVo.getTargetCiId() == null) {
            throw new AttrTargetCiIdNotFoundException(attrVo.getName());
        }

        CiVo ciVo = ciMapper.getCiById(attrVo.getTargetCiId());
        if (ciVo == null) {
            throw new CiNotFoundException(attrVo.getTargetCiId());
        }
        List<CiVo> ciList = ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht());


        return null;
    }

}
