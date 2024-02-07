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

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.framework.cmdb.enums.CiAuthType;
import neatlogic.framework.cmdb.enums.ShowType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.enums.TransactionStatus;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.transaction.TransactionService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeleteCiEntityApi extends PrivateApiComponentBase {

    @Resource
    private TransactionService transactionService;

    @Resource
    private CiViewMapper ciViewMapper;

    @Override
    public String getToken() {
        return "/cmdb/deletecientity/search";
    }

    @Override
    public String getName() {
        return "nmcac.searchdeletecientityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "common.keyword"),
            @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "nmcac.searchcientityapi.input.param.desc.needaction"),
            @Param(name = "needCheck", type = ApiParamType.BOOLEAN, desc = "nmcac.searchcientityapi.input.param.desc.needcheck")})
    @Output({@Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = CiEntityVo[].class),
            @Param(name = "theadList", type = ApiParamType.JSONARRAY, desc = "nmcac.searchattrtargetcientityapi.output.param.desc")})
    @Description(desc = "nmcac.searchdeletecientityapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        TransactionVo pTransactionVo = JSONObject.toJavaObject(jsonObj, TransactionVo.class);
        pTransactionVo.setAction(TransactionActionType.DELETE.getValue());
        pTransactionVo.setStatus(TransactionStatus.COMMITED.getValue());
        boolean needAction = jsonObj.getBooleanValue("needAction");
        boolean needCheck = jsonObj.getBooleanValue("needCheck");
        List<TransactionVo> transactionList = transactionService.searchTransaction(pTransactionVo);

        CiViewVo ciViewVo = new CiViewVo();
        ciViewVo.setCiId(pTransactionVo.getCiId());
        ciViewVo.addShowType(ShowType.LIST.getValue());
        ciViewVo.addShowType(ShowType.ALL.getValue());
        List<CiViewVo> ciViewList = RelUtil.ClearCiViewRepeatRel(ciViewMapper.getCiViewByCiId(ciViewVo));
        JSONArray theadList = new JSONArray();
        if (needCheck) {
            // 增加复选列
            theadList.add(new JSONObject() {
                {
                    this.put("key", "selection");
                }
            });
        }
        theadList.add(JSONObject.parse("{key:\"transactionId\",title:\"事务id\"}"));
        theadList.add(JSONObject.parse("{key:\"deleteTime\",title:\"删除时间\"}"));
        theadList.add(JSONObject.parse("{key:\"description\",title:\"备注\", width:300}"));
        if (CollectionUtils.isNotEmpty(ciViewList)) {
            for (CiViewVo ciview : ciViewList) {
                JSONObject headObj = new JSONObject();
                headObj.put("title", StringUtils.isNotBlank(ciview.getAlias()) ? ciview.getAlias() : ciview.getItemLabel());
                switch (ciview.getType()) {
                    case "attr":
                        headObj.put("key", "attr_" + ciview.getItemId());
                        break;
                    case "relfrom":
                        headObj.put("key", "relfrom_" + ciview.getItemId());
                        break;
                    case "relto":
                        headObj.put("key", "relto_" + ciview.getItemId());
                        break;
                    case "global":
                        headObj.put("key", "global_" + ciview.getItemId());
                        break;
                    case "const":
                        //固化属性需要特殊处理
                        headObj.put("key", "const_" + ciview.getItemName().replace("_", ""));
                        break;
                }
                theadList.add(headObj);
            }
            // if (needAction) {
            // 增加操作列，无需判断needAction，因为有“查看详情”操作
            theadList.add(new JSONObject() {
                {
                    this.put("key", "action");
                }
            });
        }
        JSONArray tbodyList = new JSONArray();

        if (CollectionUtils.isNotEmpty(transactionList)) {
            boolean canRecover = CiAuthChecker.chain().checkCiEntityRecoverPrivilege(pTransactionVo.getCiId()).check();
            List<Long> hasMaintainCiEntityIdList = new ArrayList<>();
            if (!canRecover) {
                hasMaintainCiEntityIdList = CiAuthChecker.isCiEntityInGroup(transactionList.stream().map(t -> t.getCiEntityTransactionVo().getCiEntityId()).collect(Collectors.toList()), GroupType.MAINTAIN);
            }
            for (TransactionVo transactionVo : transactionList) {
                CiEntityTransactionVo ciEntityTransactionVo = transactionVo.getCiEntityTransactionVo();
                if (StringUtils.isNotBlank(ciEntityTransactionVo.getSnapshot())) {
                    JSONObject entityObj = JSONObject.parseObject(ciEntityTransactionVo.getSnapshot());
                    entityObj.put("transactionId", transactionVo.getId());
                    entityObj.put("transactionGroupId", transactionVo.getTransactionGroupId());
                    entityObj.put("deleteTime", transactionVo.getCommitTime());
                    entityObj.put("description", transactionVo.getDescription());
                    if (needAction) {
                        JSONObject actionData = new JSONObject();
                        actionData.put(CiAuthType.CIENTITYRECOVER.getValue(), canRecover || hasMaintainCiEntityIdList.contains(entityObj.getLong("id")));
                        entityObj.put("authData", actionData);
                    }
                    tbodyList.add(entityObj);
                }
            }
        }
        JSONObject returnObj = new JSONObject();
        returnObj.put("pageSize", pTransactionVo.getPageSize());
        returnObj.put("pageCount", pTransactionVo.getPageCount());
        returnObj.put("rowNum", pTransactionVo.getRowNum());
        returnObj.put("currentPage", pTransactionVo.getCurrentPage());
        returnObj.put("tbodyList", tbodyList);
        returnObj.put("theadList", theadList);
        return returnObj;
    }

}
