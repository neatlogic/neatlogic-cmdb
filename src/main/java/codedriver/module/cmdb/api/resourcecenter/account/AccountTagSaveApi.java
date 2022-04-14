package codedriver.module.cmdb.api.resourcecenter.account;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountTagVo;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterAccountNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.RESOURCECENTER_ACCOUNT_MODIFY;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = RESOURCECENTER_ACCOUNT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AccountTagSaveApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Override
    public String getToken() {
        return "resourcecenter/account/tag/save";
    }

    @Override
    public String getName() {
        return "保存账户标签";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "accountId", type = ApiParamType.LONG, isRequired = true, desc = "账户id"),
            @Param(name = "tagList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "标签列表")
    })
    @Description(desc = "保存账户标签")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray tagArray = paramObj.getJSONArray("tagList");
        if (CollectionUtils.isEmpty(tagArray)) {
            throw new ParamNotExistsException("tagList");
        }
        Long accountId = paramObj.getLong("accountId");
        if (resourceCenterMapper.checkAccountIsExists(accountId) == 0) {
            throw new ResourceCenterAccountNotFoundException(accountId);
        }
        List<String> tagList = tagArray.toJavaList(String.class);
        List<TagVo> existTagList = resourceCenterMapper.getTagListByTagNameList(tagList);
        List<Long> tagIdList = existTagList.stream().map(TagVo::getId).collect(Collectors.toList());
        if (tagList.size() > existTagList.size()) {
            List<String> existTagNameList = existTagList.stream().map(TagVo::getName).collect(Collectors.toList());
            tagList.removeAll(existTagNameList);
            for (String tagName : tagList) {
                TagVo tagVo = new TagVo(tagName);
                resourceCenterMapper.insertTag(tagVo);
                tagIdList.add(tagVo.getId());
            }
        }
        resourceCenterMapper.deleteAccountTagByAccountId(accountId);
        List<AccountTagVo> accountTagVoList = new ArrayList<>();
        for (Long tagId : tagIdList) {
            accountTagVoList.add(new AccountTagVo(accountId, tagId));
            if (accountTagVoList.size() > 100) {
                resourceCenterMapper.insertIgnoreAccountTag(accountTagVoList);
                accountTagVoList.clear();
            }
        }
        if (CollectionUtils.isNotEmpty(accountTagVoList)) {
            resourceCenterMapper.insertIgnoreAccountTag(accountTagVoList);
        }
        return null;
    }


}
