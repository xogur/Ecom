package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface MemberRedisRepository extends CrudRepository<Member, String> {

}
