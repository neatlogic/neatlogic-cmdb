/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.globalsearch;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.fulltextindex.dto.globalsearch.DocumentTypeVo;
import codedriver.framework.fulltextindex.dto.globalsearch.DocumentVo;
import codedriver.framework.globalsearch.core.GlobalSearchManager;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.fulltextindex.enums.CmdbFullTextIndexType;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAllCiEntityApi extends PrivateApiComponentBase {


    @Override
    public String getToken() {
        return "/cmdb/globalsearch";
    }

    @Override
    public String getName() {
        return "配置项全局搜索";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, isRequired = true, desc = "关键字"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "页码，默认：1")})
    @Description(desc = "配置项全局搜索接口")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        DocumentVo documentVo = JSONObject.toJavaObject(jsonObj, DocumentVo.class);
        documentVo.setType(CmdbFullTextIndexType.CIENTITY.getType());
        List<DocumentTypeVo> documentTypeList = GlobalSearchManager.searchDocument(documentVo);
        JSONObject returnObj = new JSONObject();
        returnObj.put("documentTypeList", documentTypeList);
        returnObj.put("wordList", documentVo.getWordList());
        return returnObj;
    }

}
