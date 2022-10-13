/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.group;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.group.GroupVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.CardResultUtil;
import codedriver.framework.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.dao.mapper.group.GroupMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchGroupApi extends PrivateApiComponentBase {

    @Resource
    private GroupMapper groupMapper;


    @Override
    public String getToken() {
        return "/cmdb/group/search";
    }

    @Override
    public String getName() {
        return "搜索团体信息";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "是否激活，1：激活，0：禁用"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页大小"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "id列表，用于精确查找")
    })
    @Output({@Param(explode = GroupVo[].class)})
    @Description(desc = "搜索团体信息接口，如果提供了idList参数，将会直接返回团体列表，没有tbodyList包裹")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        GroupVo groupVo = JSONObject.toJavaObject(jsonObj, GroupVo.class);
        List<GroupVo> groupList = groupMapper.searchGroup(groupVo);
        if (CollectionUtils.isEmpty(groupVo.getIdList())) {
            if (CollectionUtils.isNotEmpty(groupList)) {
                int rowNum = groupMapper.searchGroupCount(groupVo);
                groupVo.setRowNum(rowNum);
            }
            return CardResultUtil.getResult(groupList, groupVo);
        } else {
            return groupList;
        }
    }
}
