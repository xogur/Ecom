package com.ecommerce.project.service;

import com.ecommerce.project.model.Member;
import com.ecommerce.project.repositories.MemberRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRedisRepository memberRedisRepository;

    @Transactional
    public Member addMember(Member member) {

        Member save = memberRedisRepository.save(member);

        Optional<Member> result = memberRedisRepository.findById(save.getId());

        if(result.isPresent()) {
            return result.get();
        } else {
            throw new RuntimeException("Member no Data");
        }

    }

    public Member findById(String memberId) {

        Optional<Member> result = memberRedisRepository.findById(memberId);
        if(result.isPresent()) {
            return result.get();
        } else {
            throw new RuntimeException("Member no Data");
        }
    }
}
