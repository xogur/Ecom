package com.ecommerce.project;

import com.ecommerce.project.model.QUser;
import com.ecommerce.project.model.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class SbEcomApplicationTests {

	@Autowired
	EntityManager em;

	@Test
	void contextLoads() {

		User user = new User();
		user.setUserName ("snsk");
		user.setEmail("snsk656@naver.com");
		user.setPassword("4444");
		em.persist(user);

		// 영속성 컨텍스트를 DB와 동기화하고 1차 캐시 비우기(테스트 신뢰성 ↑)
		em.flush();
		em.clear();

		// when
		JPAQueryFactory query = new JPAQueryFactory(em);
		QUser qUser = QUser.user; // 또는: new QUser("u");

		User result = query
				.selectFrom(qUser)
				.where(qUser.userName.eq("snsk"))
				.fetchOne();

		// then
		assertThat(result).isNotNull();
		assertThat(result.getUserName()).isEqualTo("snsk"); // 엔티티 게터명 확인
		assertThat(result.getEmail()).isEqualTo("snsk656@naver.com");

	}

}
