/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional
public class ListTopoRelApi extends PrivateApiComponentBase {


    @Resource
    private RelMapper relMapper;


    @Override
    public String getToken() {
        return "/cmdb/cientity/listtoporel";
    }

    @Override
    public String getName() {
        return "获取拓扑图所有关系类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "配置项id"),
            @Param(name = "level", type = ApiParamType.INTEGER, desc = "自动展开关系层数，默认是1")})
    @Output({@Param(name = "Return", explode = RelVo[].class)})
    @Description(desc = "获取拓扑图所有关系类型接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Integer level = jsonObj.getInteger("level");
        Long ciId = jsonObj.getLong("ciId");
        if (level == null) {
            level = 1;
        }
        Set<RelVo> finalRelList = new HashSet<>();
        Set<Long> ciIdList = new HashSet<>();
        ciIdList.add(ciId);
        for (int l = 0; l < level; l++) {
            Set<RelVo> levelRelList = new HashSet<>();
            for (Long cId : ciIdList) {
                List<RelVo> relList = relMapper.getRelByCiId(cId);
                levelRelList.addAll(relList);
                finalRelList.addAll(relList);
            }
            if (CollectionUtils.isNotEmpty(levelRelList)) {
                ciIdList = new HashSet<>();
                for (RelVo relVo : levelRelList) {
                    if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                        ciIdList.add(relVo.getToCiId());
                    } else if (relVo.getDirection().equals(RelDirectionType.TO.getValue())) {
                        ciIdList.add(relVo.getFromCiId());
                    }
                }
            } else {
                break;
            }
        }

        return finalRelList;
    }

}
