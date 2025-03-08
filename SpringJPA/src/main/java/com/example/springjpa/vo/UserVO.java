package com.example.springjpa.vo;
import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String name;
    private String email;
    private int age;
}