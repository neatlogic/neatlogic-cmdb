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

package neatlogic.module.cmdb.api.globalsearch;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.fulltextindex.utils.FullTextIndexUtil;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.globalsearch.GlobalSearchMapper;
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
    @Description(desc = "根据关键字搜索配置项数量")
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
