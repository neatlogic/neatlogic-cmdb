package codedriver.module.cmdb.dao.mapper.ci;

import codedriver.module.cmdb.dto.ci.CiTypeVo;
import codedriver.module.cmdb.dto.ci.CiVo;
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
