package com.jincou.cache.dto;

import lombok.Data;

import java.io.Serializable;


/**
 *  测试实体
 *
 * @author xub
 * @date 2022/3/16 下午3:15
 */
@Data
public class UserDTO implements Serializable {

    public UserDTO() {

    }

    public UserDTO(String name, String addr) {
        this.name = name;
        this.addr = addr;
    }

    private String name;
    private String addr;
    private long currTime = System.currentTimeMillis();
}
