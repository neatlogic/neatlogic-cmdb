/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.account;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountTagVo;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.dto.UserVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AccountSearchApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

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
            @Param(name = "protocolList", type = ApiParamType.JSONARRAY, desc = "协议"),
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
        JSONObject resultObj = new JSONObject();
        List<AccountVo> accountVoList = null;
        List<Long> idList = new ArrayList<>();
        AccountVo searchVo = JSON.toJavaObject(paramObj, AccountVo.class);
        accountVoList = resourceCenterMapper.searchAccount(searchVo);
        if (CollectionUtils.isNotEmpty(accountVoList)) {
            for (int i = 0; i < accountVoList.size(); i++) {
                idList.add(accountVoList.get(i).getId());
            }
        }
        if (CollectionUtils.isNotEmpty(idList)) {
            List<AccountTagVo> accountTagVoList = resourceCenterMapper.getAccountTagListByAccountIdList(idList);
            Map<Long, List<TagVo>> AccountTagVoMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(accountTagVoList)) {
                Set<Long> tagIdSet = accountTagVoList.stream().map(AccountTagVo::getTagId).collect(Collectors.toSet());
                List<TagVo> tagList = resourceCenterMapper.getTagListByIdList(new ArrayList<>(tagIdSet));
                Map<Long, TagVo> tagMap = tagList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
                for (AccountTagVo accountTagVo : accountTagVoList) {
                    AccountTagVoMap.computeIfAbsent(accountTagVo.getAccountId(), k -> new ArrayList<>()).add(tagMap.get(accountTagVo.getTagId()));
                }
            }
            for (AccountVo accountVo : accountVoList) {
                List<TagVo> tagVoList = AccountTagVoMap.get(accountVo.getId());
                if (CollectionUtils.isNotEmpty(tagVoList)) {
                    accountVo.setTagList(tagVoList);
                }
//                if (StringUtils.isNotBlank(accountVo.getFcu())) {
//                    accountVo.setFcuVo(new UserVo(accountVo.getFcu()));
//                }
//                if (StringUtils.isNotBlank(accountVo.getLcu())) {
//                    accountVo.setLcuVo(new UserVo(accountVo.getLcu()));
//                }

            }
        }
        if (accountVoList == null) {
            accountVoList = new ArrayList<>();
        }
        resultObj.put("tbodyList", accountVoList);
//        if (CollectionUtils.isNotEmpty(accountVoList)) {
//            Boolean hasAuth = AuthActionChecker.check(RESOURCECENTER_ACCOUNT_MODIFY.class.getSimpleName());
//            accountVoList.stream().forEach(o -> {
//                OperateVo delete = new OperateVo("delete", "删除");
//                if (hasAuth) {
//                    if (o.getAssetsCount() > 0) {
//                        delete.setDisabled(1);
//                        delete.setDisabledReason("当前账号已被引用，不可删除");
//                    }
//                    o.getOperateList().add(delete);
//                } else {
//                    delete.setDisabled(1);
//                    delete.setDisabledReason("无权限，请联系管理员");
//                }
//            });
//        }
        int rowNum = resourceCenterMapper.searchAccountCount(searchVo);
        searchVo.setRowNum(rowNum);
        resultObj.put("rowNum", rowNum);
        resultObj.put("pageCount", PageUtil.getPageCount(rowNum, searchVo.getPageSize()));
        resultObj.put("currentPage", searchVo.getCurrentPage());
        resultObj.put("pageSize", searchVo.getPageSize());
        return resultObj;
    }

}
