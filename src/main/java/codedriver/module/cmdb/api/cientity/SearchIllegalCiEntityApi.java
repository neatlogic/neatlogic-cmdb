/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.legalvalid.IllegalCiEntityVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.legalvalid.IllegalCiEntityMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchIllegalCiEntityApi extends PrivateApiComponentBase {

    @Resource
    private IllegalCiEntityMapper illegalCiEntityMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/illegalcientity/search";
    }

    @Override
    public String getName() {
        return "查询不合规配置项";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "关键字")})
    @Output({@Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = CiEntityVo[].class),
            @Param(name = "theadList", type = ApiParamType.JSONARRAY, desc = "表头信息")})
    @Description(desc = "查询不合规配置项接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        IllegalCiEntityVo illegalCiEntityVo = JSONObject.toJavaObject(jsonObj, IllegalCiEntityVo.class);
        CiVo ciVo = ciMapper.getCiById(illegalCiEntityVo.getCiId());
        if (ciVo == null) {
            throw new CiNotFoundException(illegalCiEntityVo.getCiId());
        }
        List<CiVo> downwardCiList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        illegalCiEntityVo.setCiIdList(downwardCiList.stream().map(CiVo::getId).collect(Collectors.toList()));
        List<IllegalCiEntityVo> illegalCiEntityList = illegalCiEntityMapper.searchIllegalCiEntity(illegalCiEntityVo);
        if (CollectionUtils.isNotEmpty(illegalCiEntityList)) {
            illegalCiEntityVo.setRowNum(illegalCiEntityMapper.searchIllegalCiEntityCount(illegalCiEntityVo));
        }
        return TableResultUtil.getResult(illegalCiEntityList, illegalCiEntityVo);
    }

}
