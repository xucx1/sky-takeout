package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品和对应的口味数据
     * Service注解：将业务逻辑类注册为 Spring 容器中的 Bean，使其能够被 @Autowired 等方式注入并参与 Spring 的统一管理。
     * Transactional注解：用于为方法或类开启事务边界，从而保证数据的一致性、原子性，要么全成功，要么全失败。（双表操作）
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {

        // DTO对象中还包含了菜品口味的属性，因此先创建一个dish实体类，将实体类传入dishMapper的insert方法
        Dish dish = new Dish();
        // 属性拷贝：前提是属性的命名要保持一致才可以拷贝
        BeanUtils.copyProperties(dishDTO, dish);
        // 向菜品表插入1条数据
        dishMapper.insert(dish);

        // 获取dishId，用于后续插入口味表时提供dishId属性值
        /*
            直接get，无法获得id值，需要在dishMapper的insert方法中将产生的主键值返回
            见DishMapper.xml文件的insert方法：
            useGeneratedKeys="true" 表示需要自动生成的主键值
            keyProperty="id" 表示将主键值赋给传入对象（即dish）的“id”属性
         */
        Long dishId = dish.getId();

        // 将DTO数据中的口味表集合取出
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            // 遍历flavors表，为每一个flavor赋上dishId值
            flavors.forEach(flavor -> {
                flavor.setDishId(dishId);
            });
            // 向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }

    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        // 调用mapper进行分页查询，注意泛型不是Dish，为了适应接口（有categoryName的属性），使用VO类型
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        // 把page对象加工处理成PageResult对象
        long total = page.getTotal();
        List<DishVO> records = page.getResult();

        return new PageResult(total, records);
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {
        // 判断当前菜品是否能够删除：1.是否存在起售中的菜品；2.是否有菜品被套餐关联
        for (Long id : ids) {
            // 根据id查询菜品，查询该菜品的状态是否为起售
            Dish dish = dishMapper.getById(id);
            if (Objects.equals(dish.getStatus(), StatusConstant.ENABLE)) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        List<Long> setmealIds = setmealDishMapper.getSetmealDishIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0) {
            // 当前菜品被套餐关联了，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        // 1.删除菜品表中的菜品数据 2.删除菜品关联的口味数据
//        for (Long id : ids) {
//            dishMapper.deleteById(id);
//            dishFlavorMapper.deleteByDishId(id);
//        }

        // 优化：直接根据id集合批量删除，避免发送过多的sql语句而引发性能问题
        dishMapper.deleteBatchByIds(ids);   // sql: delete from dish where id in (?,?,?)
        dishFlavorMapper.deleteBatchByDishIds(ids); // sql: delete from dish_flavor where dish_id in (?,?,?)
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        // 根据id查询菜品数据
        Dish dish = dishMapper.getById(id);
        // 根据菜品id查询对应的口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        // 将查询到的数据封装到DishVO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 根据id来修改菜品的基本信息和口味信息
     * @param dishDTO
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 修改菜品表的基本信息
        dishMapper.update(dish);
        // 删除原有的口味数据，再插入新的口味数据
        dishFlavorMapper.deleteByDishId(dish.getId());// DTO id?
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            // 遍历flavors表，为每一个flavor赋上dishId值
            flavors.forEach(flavor -> {
                flavor.setDishId(dish.getId());// DTO id?
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> list(Long categoryId) {
        // 为什么要传入状态变量status？？
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }
}
