/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.relativerel;

import codedriver.framework.cmdb.dto.cientity.RelEntityVo;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RelativeRelManager {
    private static RelEntityMapper relEntityMapper;

    @Autowired
    public RelativeRelManager(RelEntityMapper _relEntityMapper) {
        relEntityMapper = _relEntityMapper;
    }

    public static void delete(List<RelEntityVo> relEntityList) {

    }

    public static void insert(List<RelEntityVo> relEntityList) {

    }
}
