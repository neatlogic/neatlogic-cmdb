/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.globalsearch;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.fulltextindex.utils.FullTextIndexUtil;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.globalsearch.GlobalSearchMapper;
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
