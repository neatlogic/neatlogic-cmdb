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
import neatlogic.framework.restful.annotation.*;
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

    @Override
    public JSONObject example() {
        String json = "{\"keyword\":\"mysql 192.168.0.22\",\"currentPage\":1,\"pageSize\":20}";
        return JSON.parseObject(json);
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, isRequired = true, desc = "搜索关键字，支持名称、IP、编号等配置项相关文本"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页码，默认值是1"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条数，默认值沿用系统分页配置")})
    @Output({@Param(name = "documentTypeList", explode = DocumentTypeVo[].class, desc = "按配置项类型分组的搜索结果，每组内包含分页信息和结果列表"),
            @Param(name = "wordList", type = ApiParamType.JSONARRAY, desc = "关键字分词结果，可用于理解实际检索词")})
    @Description(desc = "根据关键字搜索配置项，并按配置项类型分组返回结果")
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
