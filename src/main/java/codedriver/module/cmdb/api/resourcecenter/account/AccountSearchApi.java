/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.account;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.AccountTagVo;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.cmdb.enums.CmdbFromType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.tagent.enums.TagentFromType;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
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
        List<AccountVo> returnAccountVoList = null;
        AccountVo paramAccountVo = JSON.toJavaObject(paramObj, AccountVo.class);
        int accountCount = resourceAccountMapper.searchAccountCount(paramAccountVo);
        if (accountCount > 0) {
            paramAccountVo.setRowNum(accountCount);
            returnAccountVoList = resourceAccountMapper.searchAccount(paramAccountVo);
            List<Long> accountIdList = returnAccountVoList.stream().map(AccountVo::getId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(accountIdList)) {

                //查询账号关联的标签
                List<AccountTagVo> accountTagVoList = resourceAccountMapper.getAccountTagListByAccountIdList(accountIdList);
                Map<Long, List<TagVo>> AccountTagVoMap = new HashMap<>();
                if (CollectionUtils.isNotEmpty(accountTagVoList)) {
                    Set<Long> tagIdSet = accountTagVoList.stream().map(AccountTagVo::getTagId).collect(Collectors.toSet());
                    List<TagVo> tagList = resourceTagMapper.getTagListByIdList(new ArrayList<>(tagIdSet));
                    Map<Long, TagVo> tagMap = tagList.stream().collect(Collectors.toMap(TagVo::getId, e -> e));
                    for (AccountTagVo accountTagVo : accountTagVoList) {
                        AccountTagVoMap.computeIfAbsent(accountTagVo.getAccountId(), k -> new ArrayList<>()).add(tagMap.get(accountTagVo.getTagId()));
                    }
                }
                //查询账号依赖的资产
                Map<Object, Integer> resourceReferredCountMap = DependencyManager.getBatchDependencyCount(CmdbFromType.RESOURCE_ACCOUNT, accountIdList);
                //查询账号依赖的tagent
                Map<Object, Integer> tagentReferredCountMap = DependencyManager.getBatchDependencyCount(TagentFromType.TAGENT_ACCOUNT, accountIdList);

                for (AccountVo accountVo : returnAccountVoList) {
                    Long returnAccountId = accountVo.getId();
                    //补充账号关联的标签
                    List<TagVo> tagVoList = AccountTagVoMap.get(returnAccountId);
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
        if (returnAccountVoList == null) {
            returnAccountVoList = new ArrayList<>();
        }
        return TableResultUtil.getResult(returnAccountVoList, paramAccountVo);
    }
}
