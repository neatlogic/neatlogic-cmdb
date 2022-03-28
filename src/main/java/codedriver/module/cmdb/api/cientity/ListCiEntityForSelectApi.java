/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linbq
 * @since 2021/6/18 14:50
 **/
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCiEntityForSelectApi extends PrivateApiComponentBase {
    @Resource
    private CiMapper ciMapper;
    @Resource
    private CiEntityMapper ciEntityMapper;

    @Override
    public String getToken() {
        return "cmdb/cientity/list/forselect";
    }

    @Override
    public String getName() {
        return "查询模型数据列表（下拉框）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciName", type = ApiParamType.STRING, isRequired = true, desc = "模型名称"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "默认值列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", explode = CiEntityVo[].class, desc = "模型数据列表")
    })
    @Description(desc = "查询资源中心状态列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        CiEntityVo ciEntityVo = paramObj.toJavaObject(CiEntityVo.class);
        String ciName = ciEntityVo.getCiName();
        CiVo ciVo = ciMapper.getCiByName(ciName);
        if (ciVo == null) {
            throw new CiNotFoundException(ciName);
        }
        JSONArray defaultValue = ciEntityVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            List<CiEntityVo> ciEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(idList);
            ciEntityVo.setPageSize(ciEntityList.size());
            ciEntityVo.setRowNum(ciEntityList.size());
            return TableResultUtil.getResult(ciEntityList, ciEntityVo);
        }
        int rowNum = ciEntityMapper.getCiEntityIdCountByCiIdAndKeyword(ciVo.getId(), ciEntityVo.getKeyword());
        if (rowNum > 0) {
            ciEntityVo.setRowNum(rowNum);
            ciEntityVo.setCiId(ciVo.getId());
            List<Long> idList = ciEntityMapper.getCiEntityIdByCiId(ciEntityVo);
            List<CiEntityVo> ciEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(idList);
            return TableResultUtil.getResult(ciEntityList, ciEntityVo);
        }
        return TableResultUtil.getResult(new ArrayList<>(), ciEntityVo);
    }
}
