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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.fulltextindex.dto.globalsearch.DocumentTypeVo;
import neatlogic.framework.fulltextindex.dto.globalsearch.DocumentVo;
import neatlogic.framework.globalsearch.core.GlobalSearchManager;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.fulltextindex.enums.CmdbFullTextIndexType;
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
    @Description(desc = "配置项全局搜索")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        DocumentVo documentVo = JSON.toJavaObject(jsonObj, DocumentVo.class);
        documentVo.setType(CmdbFullTextIndexType.CIENTITY.getType());
        List<DocumentTypeVo> documentTypeList = GlobalSearchManager.searchDocument(documentVo);
        JSONObject returnObj = new JSONObject();
        returnObj.put("documentTypeList", documentTypeList);
        returnObj.put("wordList", documentVo.getWordList());
        return returnObj;
    }

}
