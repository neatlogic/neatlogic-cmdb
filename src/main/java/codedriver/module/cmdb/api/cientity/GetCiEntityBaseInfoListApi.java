/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional
public class GetCiEntityBaseInfoListApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityMapper ciEntityMapper;


    @Override
    public String getToken() {
        return "/cmdb/cientity/baseinfo/list";
    }

    @Override
    public String getName() {
        return "获取多个配置项基础信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "idList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "配置项id列表")})
    @Output({@Param(name = "Return", explode = CiEntityVo[].class)})
    @Description(desc = "根据id列表获取多个配置项基础信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray ids = jsonObj.getJSONArray("idList");
        List<Long> idList = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            idList.add(ids.getLong(i));
        }
        if (CollectionUtils.isNotEmpty(idList)) {
            return ciEntityMapper.getCiEntityBaseInfoByIdList(idList);
        }
        return null;
    }

}
