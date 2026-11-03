/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.api.globalsearch;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.globalsearch.GlobalSearchMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCiEntityCountApi extends PrivateApiComponentBase {

    @Resource
    private GlobalSearchMapper globalSearchMapper;

    @Override
    public String getToken() {
        return "/cmdb/globalsearch/cientity/count";
    }

    @Override
    public String getName() {
        return "根据关键字搜索配置项数量";
    }

    @Override
    public String getConfig() {
        return null;
    }

    /** 复用分页条件的关键字解析，同时统计文本及完整配置项 ID 的命中结果。 */
    @Input({@Param(name = "keyword", type = ApiParamType.STRING, isRequired = true, desc = "关键字")})
    @Output({@Param(name = "ciList", type = ApiParamType.JSONARRAY, desc = "模型列表"), @Param(name = "wordList", type = ApiParamType.JSONARRAY, desc = "分词结果")})
    @Description(desc = "根据关键字搜索配置项数量")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        BasePageVo condition = new BasePageVo();
        condition.setKeyword(jsonObj.getString("keyword"));
        List<String> wordList = condition.getKeywordList();
        JSONObject returnObj = new JSONObject();
        returnObj.put("ciList", globalSearchMapper.searchCiEntityCountByKeyword(wordList, condition.getKeywordId()));
        returnObj.put("wordList", wordList);
        return returnObj;
    }

}
