/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.rel;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiRelVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAllCiRelApi extends PrivateApiComponentBase {

    @Autowired
    private RelMapper relMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/rel/list";
    }

    @Override
    public String getName() {
        return "获取模型所有关系列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "direction", type = ApiParamType.ENUM, isRequired = true, desc = "方向", member = RelDirectionType.class)})
    @Output({@Param(explode = RelVo[].class)})
    @Description(desc = "获取模型所有关系列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        List<CiRelVo> path = new ArrayList<>();
        List<List<CiRelVo>> pathList = new ArrayList<>();
        RelDirectionType directionType = RelDirectionType.get(jsonObj.getString("direction"));
        if (directionType != null) {
            getRelPathByCiId(directionType, ciId, path, pathList);
            for (List<CiRelVo> p : pathList) {
                StringBuilder ps = new StringBuilder();
                for (CiRelVo ciRelVo : p) {
                    ps.append(ciRelVo.getCiLabel()).append("->");
                }
                System.out.println(ps);
            }
        }
        return pathList;
    }


    private void getRelPathByCiId(RelDirectionType direction, Long ciId, List<CiRelVo> path, List<List<CiRelVo>> pathList) {
        if (!path.contains(new CiRelVo(ciId))) {// 出现回环，中断递归
            List<RelVo> relList = relMapper.getRelByCiId(ciId);
            List<RelVo> finalRelList = null;
            if (direction == RelDirectionType.FROM) {
                finalRelList = relList.stream().filter(d -> d.getDirection().equals(RelDirectionType.FROM.getValue())).collect(Collectors.toList());
            } else if (direction == RelDirectionType.TO) {
                finalRelList = relList.stream().filter(d -> d.getDirection().equals(RelDirectionType.TO.getValue())).collect(Collectors.toList());
            }
            if (CollectionUtils.isNotEmpty(finalRelList)) {
                for (RelVo relVo : finalRelList) {
                    CiRelVo ciRelVo = new CiRelVo();
                    ciRelVo.setCiId(relVo.getFromCiId());
                    ciRelVo.setCiName(relVo.getFromCiName());
                    ciRelVo.setCiLabel(relVo.getFromCiLabel());
                    ciRelVo.setRelId(relVo.getId());
                    ciRelVo.setRelName(relVo.getToName());
                    ciRelVo.setRelLabel(relVo.getToLabel());
                    List<CiRelVo> newpath = new ArrayList<>(path);
                    newpath.add(ciRelVo);
                    getRelPathByCiId(direction, relVo.getToCiId(), newpath, pathList);
                }
            } else {
                CiVo ciVo = ciMapper.getCiById(ciId);
                CiRelVo ciRelVo = new CiRelVo();
                ciRelVo.setCiId(ciId);
                ciRelVo.setCiName(ciVo.getName());
                ciRelVo.setCiLabel(ciVo.getLabel());
                path.add(ciRelVo);
                pathList.add(path);
            }
        } else {
            //如果path中已经存在了ciId，代表当前关系链已经构建完成
            pathList.add(path);
        }
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

}
