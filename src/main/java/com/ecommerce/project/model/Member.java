package com.ecommerce.project.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "member",timeToLive = 60) // options: timeToLive = ""
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Member {

    @Id
    private String id;

    private String name;
    private int age;
}
