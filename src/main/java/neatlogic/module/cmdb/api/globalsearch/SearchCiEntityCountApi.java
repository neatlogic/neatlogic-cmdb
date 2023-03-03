/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.globalsearch;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.fulltextindex.utils.FullTextIndexUtil;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.globalsearch.GlobalSearchMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, isRequired = true, desc = "关键字")})
    @Output({@Param(name = "ciList", type = ApiParamType.JSONARRAY, desc = "模型列表"), @Param(name = "wordList", type = ApiParamType.JSONARRAY, desc = "分词结果")})
    @Description(desc = "根据关键字搜索配置项数量接口")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        Set<String> wordSet = FullTextIndexUtil.sliceKeyword(jsonObj.getString("keyword"));
        List<String> wordList = new ArrayList<>(wordSet);
        JSONObject returnObj = new JSONObject();
        returnObj.put("ciList", globalSearchMapper.searchCiEntityCountByWord(wordList));
        returnObj.put("wordList", wordList);
        return returnObj;
    }

}
