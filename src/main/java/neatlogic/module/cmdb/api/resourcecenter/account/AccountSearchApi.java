/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.api.resourcecenter.account;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountTagVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.cmdb.enums.CmdbFromType;
import neatlogic.framework.cmdb.enums.resourcecenter.AccountType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.tagent.enums.TagentFromType;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AccountSearchApi extends PrivateApiComponentBase {

    @Resource
    private ResourceTagMapper resourceTagMapper;

    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Override
    public String getToken() {
        return "resourcecenter/account/search";
    }

    @Override
    public String getName() {
        return "查询资源中心账号";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "protocolIdList", type = ApiParamType.JSONARRAY, desc = "协议id列表"),
            @Param(name = "protocolList", type = ApiParamType.JSONARRAY, desc = "协议名称列表"),
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "关键词"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的账号ID列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", explode = AccountVo[].class, desc = "账号列表"),
            @Param(explode = BasePageVo.class),
    })
    @Description(desc = "查询资源中心账号")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<AccountVo> returnAccountVoList = new ArrayList<>();
        AccountVo paramAccountVo = paramObj.toJavaObject(AccountVo.class);
        JSONArray defaultValue = paramAccountVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            returnAccountVoList = resourceAccountMapper.getAccountListByIdList(idList);
        } else {
            paramAccountVo.setType(AccountType.PUBLIC.getValue());
            int accountCount = resourceAccountMapper.searchAccountCount(paramAccountVo);
            if (accountCount > 0) {
                paramAccountVo.setRowNum(accountCount);
                returnAccountVoList = resourceAccountMapper.searchAccount(paramAccountVo);
                List<Long> accountIdList = returnAccountVoList.stream().map(AccountVo::getId).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(accountIdList)) {

                    //查询账号关联的标签
                    List<AccountTagVo> accountTagVoList = resourceAccountMapper.getAccountTagListByAccountIdList(accountIdList);
                    Map<Long, List<TagVo>> accountTagVoMap = new HashMap<>();
                    if (CollectionUtils.isNotEmpty(accountTagVoList)) {
                        Set<Long> tagIdSet = accountTagVoList.stream().map(AccountTagVo::getTagId).collect(Collectors.toSet());
                        List<TagVo> tagList = resourceTagMapper.getTagListByIdList(new ArrayList<>(tagIdSet));
                        Map<Long, TagVo> tagMap = tagList.stream().collect(Collectors.toMap(TagVo::getId, e -> e));
                        for (AccountTagVo accountTagVo : accountTagVoList) {
                            accountTagVoMap.computeIfAbsent(accountTagVo.getAccountId(), k -> new ArrayList<>()).add(tagMap.get(accountTagVo.getTagId()));
                        }
                    }
                    //查询账号依赖的资产
                    Map<Object, Integer> resourceReferredCountMap = DependencyManager.getBatchDependencyCount(CmdbFromType.RESOURCE_ACCOUNT, accountIdList);
                    //查询账号依赖的tagent
                    Map<Object, Integer> tagentReferredCountMap = DependencyManager.getBatchDependencyCount(TagentFromType.TAGENT_ACCOUNT, accountIdList);

                    for (AccountVo accountVo : returnAccountVoList) {
                        Long returnAccountId = accountVo.getId();
                        //补充账号关联的标签
                        List<TagVo> tagVoList = accountTagVoMap.get(returnAccountId);
                        if (CollectionUtils.isNotEmpty(tagVoList)) {
                            accountVo.setTagList(tagVoList);
                        }

                        //补充账号依赖的资产个数
                        if (resourceReferredCountMap.containsKey(returnAccountId)) {
                            accountVo.setResourceReferredCount(resourceReferredCountMap.get(returnAccountId));
                        }

                        //补充账号依赖的tagent个数
                        if (tagentReferredCountMap.containsKey(returnAccountId)) {
                            accountVo.setTagentReferredCount((tagentReferredCountMap.get(returnAccountId)));
                        }
                    }
                }
            }
        }
        return TableResultUtil.getResult(returnAccountVoList, paramAccountVo);
    }
}
