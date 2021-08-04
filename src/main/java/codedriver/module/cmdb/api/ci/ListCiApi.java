/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCiApi extends PrivateApiComponentBase {

    @Autowired
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/list";
    }

    @Override
    public String getName() {
        return "返回模型列表信息（下拉框用）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "模型id列表")})
    @Output({@Param(explode = ValueTextVo[].class)})
    @Description(desc = "返回模型列表信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray idList = jsonObj.getJSONArray("idList");
        List<Long> ciIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(idList)) {
            for (int i = 0; i < idList.size(); i++) {
                try {
                    ciIdList.add(idList.getLong(i));
                } catch (Exception ignored) {

                }
            }
        }
        List<CiVo> ciList = ciMapper.getAllCi(ciIdList);
        JSONArray jsonList = new JSONArray();
        for (CiVo ciVo : ciList) {
            JSONObject valueObj = new JSONObject();
            valueObj.put("value", ciVo.getId());
            valueObj.put("text", ciVo.getLabel());
            jsonList.add(valueObj);
        }
        return jsonList;
    }
}
