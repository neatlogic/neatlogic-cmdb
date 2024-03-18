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
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_ACCOUNT_MODIFY;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountTagVo;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
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
    private ResourceAccountMapper resourceAccountMapper;
    @Resource
    private ResourceTagMapper resourceTagMapper;

    @Override
    public String getToken() {
        return "resourcecenter/account/tag/save";
    }

    @Override
    public String getName() {
        return "保存账号标签";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "accountId", type = ApiParamType.LONG, isRequired = true, desc = "账号id"),
            @Param(name = "tagList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "标签列表")
    })
    @Description(desc = "保存账号标签")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray tagArray = paramObj.getJSONArray("tagList");
        if (CollectionUtils.isEmpty(tagArray)) {
            throw new ParamNotExistsException("tagList");
        }
        Long accountId = paramObj.getLong("accountId");
        if (resourceAccountMapper.checkAccountIsExists(accountId) == 0) {
            throw new ResourceCenterAccountNotFoundException(accountId);
        }
        List<String> tagList = tagArray.toJavaList(String.class);
        List<TagVo> existTagList = resourceTagMapper.getTagListByTagNameList(tagList);
        List<Long> tagIdList = existTagList.stream().map(TagVo::getId).collect(Collectors.toList());
        if (tagList.size() > existTagList.size()) {
            List<String> existTagNameList = existTagList.stream().map(TagVo::getName).collect(Collectors.toList());
            tagList.removeAll(existTagNameList);
            for (String tagName : tagList) {
                TagVo tagVo = new TagVo(tagName);
                resourceTagMapper.insertTag(tagVo);
                tagIdList.add(tagVo.getId());
            }
        }
        resourceAccountMapper.deleteAccountTagByAccountId(accountId);
        List<AccountTagVo> accountTagVoList = new ArrayList<>();
        for (Long tagId : tagIdList) {
            accountTagVoList.add(new AccountTagVo(accountId, tagId));
            if (accountTagVoList.size() > 100) {
                resourceAccountMapper.insertIgnoreAccountTag(accountTagVoList);
                accountTagVoList.clear();
            }
        }
        if (CollectionUtils.isNotEmpty(accountTagVoList)) {
            resourceAccountMapper.insertIgnoreAccountTag(accountTagVoList);
        }
        return null;
    }


}
