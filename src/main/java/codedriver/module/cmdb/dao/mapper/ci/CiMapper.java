/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.ci;

import codedriver.framework.cmdb.dto.ci.CiTypeVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/*
 * @Description:
 * @Author: chenqiwei
 * @Date: 2021/3/20 3:04 下午
 * @Params: * @param null:
 * @Returns: * @return: null
 **/
public interface CiMapper {

    List<CiVo> getUpwardCiListByLR(@Param("lft") Integer lft, @Param("rht") Integer rht);

    List<CiVo> getDownwardCiListByLR(@Param("lft") Integer lft, @Param("rht") Integer rht);


    List<CiVo> getAllCi();

    List<CiVo> getCiByToCiId(Long ciId);

    List<CiVo> getCiByIdList(@Param("ciIdList") List<Long> ciIds);

    int checkCiNameIsExists(CiVo ciVo);

    int checkCiLabelIsExists(CiVo ciVo);

    List<CiTypeVo> searchCiTypeCi(CiVo ciVo);

    CiVo getCiById(Long ciId);

    int updateCi(CiVo ciVo);

    int insertCi(CiVo ciVo);

    int deleteCiById(Long ciId);
}
